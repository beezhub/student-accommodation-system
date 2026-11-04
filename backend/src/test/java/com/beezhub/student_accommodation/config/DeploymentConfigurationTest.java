package com.beezhub.student_accommodation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * GH-162: the compose file publishes every backing service on the host, ships MailHog as the mail
 * server, and runs Redis with no password. That is right for a laptop and wrong for anything
 * reachable, so the hardening lives in an overlay rather than in the base file.
 *
 * <p>These pin agreement between files. They do not test behaviour: whether the overlay actually
 * brings a stack up needs a real deployment.
 */
class DeploymentConfigurationTest {

    private static final Path REPO_ROOT = Path.of("..");

    private static String read(String relativePath) throws IOException {
        return Files.readString(REPO_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String applicationYml() throws IOException {
        return Files.readString(new ClassPathResource("application.yml").getFile().toPath(),
                StandardCharsets.UTF_8);
    }

    /**
     * Redis is on the request path for token-blacklist checks and auth rate limiting, and both fail
     * open, so a stalled server must be given up on rather than holding a request thread. The
     * previous timeout was sixty seconds.
     */
    @Test
    void redisTimeouts_areShortEnoughToFailOpen() throws IOException {
        String yml = applicationYml();

        assertThat(yml).contains("timeout: ${REDIS_TIMEOUT_MS:2000}");
        assertThat(yml).contains("connect-timeout: ${REDIS_CONNECT_TIMEOUT_MS:1000}");
    }

    /**
     * An unlimited pool wait means a pool exhausted by a slow Redis blocks every later caller
     * indefinitely, which defeats the fail-open behaviour above.
     */
    @Test
    void redisPoolWait_isBounded() throws IOException {
        assertThat(applicationYml())
                .as("max-wait must not be -1ms")
                .doesNotContain("max-wait: -1ms");
    }

    /** It pointed at a database and credentials that compose never creates. */
    @Test
    void grafanaDatasource_namesTheDatabaseComposeCreates() throws IOException {
        String datasource = read("tools/grafana/provisioning/datasources/datasource.yml");

        assertThat(datasource).contains("studentaccommodationlocaldb");
        assertThat(datasource)
                .as("the datasource must not carry a hardcoded postgres/postgres fallback")
                .doesNotContain("GRAFANA_DB_PASSWORD:-postgres");
    }

    /**
     * A merge spliced two versions of three dashboards into one file each. Grafana skipped all three
     * and logged the parse error every ten seconds; nothing else reported it.
     */
    @Test
    void grafanaDashboards_areValidJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (Stream<Path> files = Files.list(REPO_ROOT.resolve("tools/grafana/dashboards"))) {
            List<Path> dashboards = files.filter(p -> p.toString().endsWith(".json")).toList();

            assertThat(dashboards).isNotEmpty();
            for (Path dashboard : dashboards) {
                assertThatCode(() -> mapper.readTree(dashboard.toFile()))
                        .as(dashboard.getFileName().toString())
                        .doesNotThrowAnyException();
            }
        }
    }

    /**
     * infra/terraform/prod-grafana/dashboards.tf copies these dashboards to Grafana Cloud (#340) by
     * rewriting each panel's and each query's data source one level deep. A row, a nested panel or a
     * panel without its own data source would break that at terraform plan, far from the change
     * that caused it, so the shape is pinned here.
     */
    @Test
    void grafanaDashboards_haveTheFlatShapeTheProdCopyRewrites() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (Stream<Path> files = Files.list(REPO_ROOT.resolve("tools/grafana/dashboards"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                String name = file.getFileName().toString();
                var dashboard = mapper.readTree(file.toFile());

                assertThat(dashboard.path("uid").asText()).as(name + " uid").isNotBlank();
                assertThat(dashboard.path("panels").isArray()).as(name + " panels").isTrue();
                for (var panel : dashboard.path("panels")) {
                    String where = name + " panel \"" + panel.path("title").asText() + "\"";
                    assertThat(panel.path("type").asText()).as(where).isNotEqualTo("row");
                    assertThat(panel.has("panels")).as(where + " nested panels").isFalse();
                    assertThat(panel.path("datasource").path("type").asText()).as(where + " datasource")
                            .isNotBlank();
                    assertThat(panel.path("targets").isArray()).as(where + " targets").isTrue();
                    for (var target : panel.path("targets")) {
                        assertThat(target.has("datasource")).as(where + " query datasource").isTrue();
                    }
                }
            }
        }
    }

    /**
     * Grafana builds its redirects from GRAFANA_ROOT_URL, which ends in /grafana. Without sub-path
     * serving the login page redirects to itself forever; without the Caddy route /grafana reaches
     * the frontend instead.
     */
    @Test
    void deployOverlay_servesGrafanaUnderTheCaddyRoute() throws IOException {
        assertThat(read("docker-compose.deploy.yml")).contains("GF_SERVER_SERVE_FROM_SUB_PATH=true");
        assertThat(read("tools/caddy/Caddyfile")).contains("@grafana path /grafana /grafana/*");
    }

    @Test
    void productionOverlay_exists() {
        assertThat(REPO_ROOT.resolve("docker-compose.prod.yml")).exists();
    }

    /**
     * MailHog accepts every message and delivers none. Left running in a real deployment, no
     * student ever receives an email and nothing reports an error.
     */
    @Test
    void productionOverlay_doesNotRunTheDevelopmentMailCatcher() throws IOException {
        String prod = read("docker-compose.prod.yml");

        assertThat(prod).contains("mailhog");
        assertThat(prod).contains("replicas: 0");
        assertThat(prod).contains("MAIL_HOST=${MAIL_HOST:?");
    }

    @Test
    void productionOverlay_requiresRedisAuthentication() throws IOException {
        assertThat(read("docker-compose.prod.yml")).contains("--requirepass");
    }

    /** The base file publishes every backing service; the overlay takes them all back. */
    @Test
    void productionOverlay_removesHostPortPublishing() throws IOException {
        String prod = read("docker-compose.prod.yml");

        assertThat(prod).contains("ports: !override []");
        assertThat(prod)
                .as("every service the base file publishes must be overridden")
                .containsSubsequence("db:", "student-accommodation-back-end:", "minio:", "redis:",
                        "prometheus:", "grafana:");
    }

    /** Without the prod profile Swagger stays on and the API docs stay open. */
    @Test
    void productionOverlay_selectsTheProductionProfile() throws IOException {
        assertThat(read("docker-compose.prod.yml")).contains("SPRING_PROFILES_ACTIVE=prod");
    }

    /** Checkouts on Windows carry CRLF; the multi-line assertions below are written with LF. */
    private static String readLf(String relativePath) throws IOException {
        return read(relativePath).replace("\r\n", "\n");
    }

    /**
     * PROD runs on a 2 GB server with RDS and S3 (docs/PROD_ENVIRONMENT_PLAN.md). Anything the
     * managed services replace, or that the server has no memory for, must not start there.
     */
    @Test
    void awsOverlay_switchesOffEveryServiceThatDoesNotRunInProd() throws IOException {
        String aws = readLf("docker-compose.aws.yml");

        for (String service : List.of("db", "db-tls-cert", "minio", "minio-volume-permissions", "prometheus",
                "grafana", "loki")) {
            assertThat(aws)
                    .as(service + " must be scaled to zero behind an unused profile")
                    .contains("  " + service + ":\n    deploy:\n      replicas: 0\n    profiles: [\"never\"]");
        }
    }

    /**
     * The base file makes the backend wait on db and minio being healthy, and a profile does not
     * stop Compose starting a dependency, so the switched-off services would come up anyway
     * (#246).
     */
    @Test
    void awsOverlay_backendDependsOnRedisOnly() throws IOException {
        String aws = readLf("docker-compose.aws.yml");
        String backend = aws.substring(aws.indexOf("  student-accommodation-back-end:"),
                aws.indexOf("  frontend:"));
        String dependsOn = backend.substring(backend.indexOf("depends_on: !override"),
                backend.indexOf("    environment:"));

        assertThat(dependsOn).contains("redis:");
        assertThat(dependsOn).doesNotContain("db:").doesNotContain("minio:");
    }

    /**
     * NONPROD pulls what images.yml built for arm64 in the same run, by digest, instead of building
     * on the host (#337). Local development keeps building from the base file.
     */
    @Test
    void deployOverlay_pullsImagesByDigestAndNeverBuilds() throws IOException {
        String deploy = read("docker-compose.deploy.yml");

        assertThat(deploy).contains("image: ${BACKEND_IMAGE:?");
        assertThat(deploy).contains("image: ${FRONTEND_IMAGE:?");
        assertThat(deploy).contains("build: !reset null");
        assertThat(deploy).doesNotContain("context: ./frontend");
        assertThat(read("docker-compose.yml")).contains("context: ./backend");
    }

    /** PROD runs the image NONPROD ran, pulled by digest, and never builds from the tree. */
    @Test
    void awsOverlay_pullsImagesByDigestAndNeverBuilds() throws IOException {
        String aws = read("docker-compose.aws.yml");

        assertThat(aws).contains("image: ${BACKEND_IMAGE:?");
        assertThat(aws).contains("image: ${FRONTEND_IMAGE:?");
        assertThat(aws.split("build: !reset null", -1)).hasSize(3);
    }

    /** PROD reaches S3 with the instance role and never creates the Terraform-owned bucket (#331). */
    @Test
    void awsOverlay_pinsTheS3StorageSettings() throws IOException {
        assertThat(read("docker-compose.aws.yml"))
                .contains("- S3_REGION=af-south-1")
                .contains("- S3_CREDENTIALS=aws-instance-role")
                .contains("- S3_CREATE_BUCKET=false");
        assertThat(read("docker-compose.aws.yml"))
                .as("the SES SMTP login is an IAM key ID, not an address (GH-356)")
                .contains("- MAIL_FROM=no-reply@beezhub.co.za");
        assertThat(read("docker-compose.aws.yml"))
                .as("SES has no SMTP endpoint in af-south-1 (GH-360)")
                .contains("- MAIL_TRANSPORT=ses-api");
        assertThat(read("docker-compose.yml")).contains("- MAIL_TRANSPORT=${MAIL_TRANSPORT:-smtp}");
        assertThat(read("docker-compose.yml")).contains("- MAIL_FROM=${MAIL_FROM:-}");
        assertThat(read("docker-compose.yml"))
                .as("the base file must forward the settings for the overlay to override")
                .contains("- S3_REGION=${S3_REGION:-}")
                .contains("- S3_CREDENTIALS=${S3_CREDENTIALS:-static}")
                .contains("- S3_CREATE_BUCKET=${S3_CREATE_BUCKET:-true}");
    }

    /**
     * The document previews show PDFs in an iframe from a blob: URL. frame-src 'none' blocked every
     * one of them while downloads worked (GH-362); other origins must stay refused.
     */
    @Test
    void frontendCsp_allowsBlobFramesForDocumentPreviewsOnly() throws IOException {
        String csp = read("frontend/nginx-security-headers.conf");

        assertThat(csp).contains("frame-src 'self' blob:;");
        assertThat(csp).contains("object-src 'self' blob:");
        assertThat(csp).doesNotContain("frame-src 'none'").doesNotContain("frame-src *")
                .doesNotContain("frame-src https:");
    }

    /** The base file's DB_URL names the db container, which does not exist in PROD. */
    @Test
    void awsOverlay_requiresAnExternalDatabaseUrl() throws IOException {
        assertThat(read("docker-compose.aws.yml")).contains("- DB_URL=${DB_URL:?");
    }

    /**
     * NONPROD and PROD share one log pipeline and differ only in where it writes. Alloy loads a
     * whole directory, and /etc/alloy itself holds the image's example config.alloy, which turns on
     * debug logging and exporters pointed at localhost.
     */
    @Test
    void alloy_sharesTheLogPipelineAndLoadsOnlyItsOwnDirectory() throws IOException {
        String deploy = readLf("docker-compose.deploy.yml");
        String aws = readLf("docker-compose.aws.yml");

        assertThat(deploy).contains("      - /etc/alloy/conf.d\n");
        assertThat(deploy).contains("tools/alloy/logs.alloy:/etc/alloy/conf.d/logs.alloy:ro");
        assertThat(deploy).contains("tools/alloy/loki-local.alloy:/etc/alloy/conf.d/loki-local.alloy:ro");
        assertThat(aws).contains("tools/alloy/logs.alloy:/etc/alloy/conf.d/logs.alloy:ro");
        assertThat(aws).contains("tools/alloy/grafana-cloud.alloy:/etc/alloy/conf.d/grafana-cloud.alloy:ro");
        assertThat(aws).doesNotContain("tools/alloy/loki-local.alloy:");

        assertThat(readLf("tools/alloy/logs.alloy"))
                .as("the destination belongs to the per-host file, not the shared pipeline")
                .doesNotContain("\nloki.write \"");
        assertThat(read("tools/alloy/loki-local.alloy")).contains("loki.write \"default\"");
        assertThat(read("tools/alloy/grafana-cloud.alloy")).contains("loki.write \"default\"");
    }

    /**
     * The notification stream takes the JWT as ?token=, so nginx and Caddy log a working credential.
     * Both log pipelines must mask it before it reaches Loki or Grafana Cloud, and every line must
     * say which environment it came from.
     */
    @Test
    void alloy_masksTokensAndLabelsTheEnvironment() throws IOException {
        String logs = readLf("tools/alloy/logs.alloy");

        assertThat(logs.split("expression = \"\\[\\?&\\]token=\\(\\[\\^&\\\\\\\\s\\\\\"\\]\\+\\)\"", -1))
                .as("one token mask in the container pipeline and one in the journal pipeline")
                .hasSize(3);
        assertThat(logs).contains("replace    = \"REDACTED\"");
        assertThat(read("tools/alloy/loki-local.alloy")).contains("external_labels = {env = \"nonprod\"}");
        assertThat(read("tools/alloy/grafana-cloud.alloy").split("external_labels = \\{env = \"prod\"\\}", -1))
                .as("logs and metrics both carry env=prod")
                .hasSize(3);
    }

    /** Alloy does Prometheus's job in PROD, with the same credentials the backend checks. */
    @Test
    void awsOverlay_alloyScrapesTheBackendWithTheSharedSecret() throws IOException {
        String aws = readLf("docker-compose.aws.yml");
        String alloy = aws.substring(aws.indexOf("  alloy:"));

        assertThat(alloy).contains("- prometheus_scrape_password");
        assertThat(alloy).contains("- app-network");
        assertThat(read("tools/alloy/grafana-cloud.alloy"))
                .contains("password_file = \"/run/secrets/prometheus_scrape_password\"")
                .contains("job_name        = \"student-accommodation-backend\"");
    }

    /**
     * The memory, swap and disk alerts read the host, not the Alloy container (#340). The exporter's
     * paths must be the ones compose mounts, read-only, and only the kept series reach Grafana
     * Cloud, whose free plan counts every series.
     */
    @Test
    void awsOverlay_alloyShipsOnlyTheHostSeriesTheAlertsRead() throws IOException {
        String aws = readLf("docker-compose.aws.yml");
        String alloy = aws.substring(aws.indexOf("  alloy:"));
        String cloud = readLf("tools/alloy/grafana-cloud.alloy");

        assertThat(alloy).contains("- /proc:/host/proc:ro\n", "- /sys:/host/sys:ro\n", "- /:/host/root:ro,rslave\n");
        assertThat(cloud).contains("procfs_path    = \"/host/proc\"", "sysfs_path     = \"/host/sys\"",
                "rootfs_path    = \"/host/root\"");
        assertThat(cloud).contains("forward_to      = [prometheus.relabel.host.receiver]");
        assertThat(cloud.substring(cloud.indexOf("prometheus.relabel \"host\"")))
                .contains("action        = \"keep\"")
                .contains("forward_to = [prometheus.remote_write.default.receiver]");
    }

    /**
     * The Grafana alerts keep their own state, so an AWS plan never needs the Grafana token, and
     * every query is pinned to env="prod" so a second environment on the stack (#381) is ignored.
     */
    @Test
    void prodGrafana_ownStateAndOnlyProdSeries() throws IOException {
        String versions = readLf("infra/terraform/prod-grafana/versions.tf");
        String alerts = readLf("infra/terraform/prod-grafana/alerts.tf");

        assertThat(versions).contains("key          = \"prod-grafana/terraform.tfstate\"");
        assertThat(versions).contains("version = \"4.50.0\"");
        assertThat(alerts).contains("backend = \"job=\\\"student-accommodation-backend\\\",env=\\\"prod\\\"\"");
        assertThat(alerts).contains("requests   = \"http_server_requests_seconds_count{${local.backend},");
        assertThat(alerts).contains("errors_5xx = \"http_server_requests_seconds_count{${local.backend},");
        assertThat(alerts.lines().filter(line -> line.trim().matches("expr\\s+= \".*")))
                .hasSize(5)
                .allSatisfy(line -> assertThat(line)
                        .containsAnyOf("env=\\\"prod\\\"", "${local.backend}", "${local.requests}"));
    }

    /**
     * A misspelt usage type group is not an error: the budget just measures 0 GB for ever. These
     * are the names Cost Explorer lists for this account (#340), and both paths out are counted:
     * pages from the server and documents straight from S3.
     */
    @Test
    void dataOutBudget_countsServerAndS3Downloads() throws IOException {
        String monitoring = readLf("infra/terraform/prod/monitoring.tf");
        String budget = monitoring.substring(monitoring.indexOf("resource \"aws_budgets_budget\" \"data_out\""));

        assertThat(budget).contains("budget_type  = \"USAGE\"", "limit_unit   = \"GB\"",
                "name   = \"UsageTypeGroup\"");
        assertThat(budget).contains(
                "values = [\"EC2: Data Transfer - Internet (Out)\", \"S3: Data Transfer - Internet (Out)\"]");
    }

    /**
     * The backend runs on the database password write-env.sh copied at the last deploy, so every
     * RDS rotation takes PROD down until a redeploy (#407). Yearly, not RDS's default weekly; and
     * rotate_immediately defaults to true, which would rotate, and cause that outage, on apply.
     */
    @Test
    void dbPasswordRotation_yearlyAndNotOnApply() throws IOException {
        String database = readLf("infra/terraform/prod/database.tf");
        String rotation = database.substring(
                database.indexOf("resource \"aws_secretsmanager_secret_rotation\" \"db_master\""));

        assertThat(rotation).contains("secret_id          = aws_db_instance.main.master_user_secret[0].secret_arn");
        assertThat(rotation).contains("rotate_immediately = false");
        assertThat(rotation).contains("automatically_after_days = 365");
    }

    @Test
    void awsOverlay_settingsAreDocumented() throws IOException {
        String env = read(".env.example");

        assertThat(env).contains("BACKEND_IMAGE=").contains("FRONTEND_IMAGE=");
        assertThat(env).contains("GRAFANA_CLOUD_LOGS_URL=").contains("GRAFANA_CLOUD_METRICS_URL=")
                .contains("GRAFANA_CLOUD_API_KEY=");
    }

    @Test
    void newSettingsAreDocumented() throws IOException {
        String env = read(".env.example");

        assertThat(env).contains("APP_CONTACT_PHONE");
        assertThat(env).contains("REDIS_TIMEOUT_MS");
    }

    /**
     * The example-data migration runs once (#365). Local and NONPROD keep the rows by default, and
     * PROD cannot start without saying what it wants: a ${VAR:?} key is what write-env.sh checks
     * before it changes anything on the server, so a missing value stops the deploy before Flyway.
     */
    @Test
    void exampleData_keptByDefaultAndRequiredOnProd() throws IOException {
        assertThat(applicationYml()).contains("exampleData: ${EXAMPLE_DATA:true}");
        assertThat(read("backend/src/main/resources/db/migration/V2026_11_04_1000__remove_example_data.sql"))
                .contains("IF lower('${exampleData}') = 'true' THEN");

        String aws = readLf("docker-compose.aws.yml");
        String backend = aws.substring(aws.indexOf("  student-accommodation-back-end:"), aws.indexOf("  alloy:"));
        assertThat(backend).contains("- EXAMPLE_DATA=${EXAMPLE_DATA:?");
        assertThat(read("tools/prod/write-env.sh")).contains("grep -hoE '\\$\\{[A-Z0-9_]+(\\}|:?\\?)'");
        assertThat(read(".env.example")).contains("EXAMPLE_DATA=true");
    }

    /**
     * NONPROD rehearses releases: it deploys main and records what passed its smoke check, which
     * is the only thing deploy-prod.yml will promote (#338).
     */
    @Test
    void nonprodDeploy_tracksMainAndRecordsTheRelease() throws IOException {
        String deploy = readLf(".github/workflows/deploy.yml");

        assertThat(deploy).contains("  push:\n    branches: [main]\n");
        assertThat(deploy).contains("name: nonprod-release");
        assertThat(deploy.indexOf("name: Record the release for PROD"))
                .as("the release is recorded only after the smoke check passes")
                .isGreaterThan(deploy.indexOf("name: Smoke check"));
    }

    /**
     * After a hotfix, a workflow opens the main to develop pull request, so the fix cannot be
     * stranded on main the way #288 was (#386). It asks whether merging main into develop would
     * change develop, not whether their files differ: develop is usually ahead of main, so the
     * files differ after every normal release too.
     */
    @Test
    void backMerge_opensAPullRequestOnlyWhenDevelopLacksAChangeFromMain() throws IOException {
        String workflow = readLf(".github/workflows/back-merge.yml");
        String script = read("tools/ci/back-merge.sh");

        assertThat(workflow).contains("on:\n  push:\n    branches: [main]\n");
        assertThat(workflow).contains("permissions:\n  contents: read\n  pull-requests: write\n");
        assertThat(workflow).contains("fetch-depth: 0").contains("run: bash tools/ci/back-merge.sh");
        assertThat(script).contains("git merge-tree --write-tree");
        assertThat(script).contains("gh pr list --base develop --head main --state open");
        assertThat(script).contains("gh pr create --base develop --head main");
    }

    /**
     * A hotfix is the one branch that starts from main, and a person decides on it (#386).
     */
    @Test
    void hotfixRule_isWrittenWhereContributorsAndScoutLook() throws IOException {
        assertThat(readLf("CONTRIBUTING.md")).contains("### Hotfixes")
                .contains("Branch from `main` and name it `hotfix/TASK-{id}-{slug}`.");
        assertThat(read(".claude/scout/playbook.md")).contains("Scout never opens one");
    }

    /**
     * The first major version of each action that runs on Node 24 or later (#387). Older ones ran
     * on Node 20 only because GitHub forced them onto Node 24, and stop working when it stops.
     * Raise a number here when a workflow moves to a newer major; never lower one.
     */
    private static final Map<String, Integer> MINIMUM_ACTION_MAJOR = Map.of(
            "actions/checkout", 7,
            "actions/setup-java", 6,
            "actions/setup-node", 7,
            "actions/upload-artifact", 7,
            "aws-actions/configure-aws-credentials", 6,
            "aws-actions/amazon-ecr-login", 2,
            "docker/build-push-action", 7,
            "docker/setup-buildx-action", 4);

    @Test
    void workflows_useNoActionOlderThanItsNode24Major() throws IOException {
        Pattern uses = Pattern.compile("uses: ([\\w.-]+/[\\w.-]+)@v(\\d+)");
        int checked = 0;

        try (Stream<Path> files = Files.list(REPO_ROOT.resolve(".github/workflows"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yml")).toList()) {
                Matcher m = uses.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    assertThat(MINIMUM_ACTION_MAJOR)
                            .as(m.group(1) + " in " + file.getFileName() + " needs a minimum here")
                            .containsKey(m.group(1));
                    assertThat(Integer.parseInt(m.group(2)))
                            .as(m.group(1) + " in " + file.getFileName())
                            .isGreaterThanOrEqualTo(MINIMUM_ACTION_MAJOR.get(m.group(1)));
                    checked++;
                }
            }
        }
        assertThat(checked).as("action references found").isGreaterThan(20);
    }

    /**
     * A release pull request builds no images: every change in it built on its own pull request,
     * and the push to main builds again for the deploy (#376). The deploy's call is not a pull
     * request, so the condition never skips it.
     */
    @Test
    void images_skipReleasePullRequestsOnly() throws IOException {
        String images = readLf(".github/workflows/images.yml");
        String skip = "    if: github.event_name != 'pull_request' || github.head_ref != 'develop'\n";

        assertThat(images).contains("    name: Backend image (arm64)\n" + skip);
        assertThat(images).contains("    name: Frontend image (arm64)\n" + skip);
    }

    /**
     * CI runs on pull requests and the develop push, not on main: a release merges files develop
     * already tested (#385). A hotfix pull request into main is still tested.
     */
    @Test
    void ci_runsOnDevelopPushesAndOnPullRequestsOnly() throws IOException {
        String ci = readLf(".github/workflows/ci.yml");

        assertThat(ci).contains("  push:\n    branches: [develop]\n");
        assertThat(ci).contains("  pull_request:\n    branches: [main, develop]\n");
    }

    /**
     * With no CI on main, the NONPROD deploy checks that a successful CI run tested these files
     * before it builds anything (#385), and PROD only promotes a green NONPROD run.
     */
    @Test
    void nonprodDeploy_buildsOnlyAfterCiPassedOnTheseFiles() throws IOException {
        String deploy = readLf(".github/workflows/deploy.yml");
        String tested = deploy.substring(deploy.indexOf("  tested:"), deploy.indexOf("  images:"));

        assertThat(tested).contains("run: bash tools/ci/ci-passed.sh \"$GITHUB_SHA\"");
        assertThat(tested).contains("fetch-depth: 0").contains("actions: read");
        assertThat(deploy).contains("  images:\n    name: Build images\n    needs: tested\n");
    }

    /**
     * ci-passed.sh reads the ignored paths from the push trigger in ci.yml, one quoted entry per
     * line. If that layout changes the script finds no ignored paths: safe, but every docs-only
     * release would then be refused.
     */
    @Test
    void ciPassedScript_readsTheIgnoredPathsFromCi() throws IOException {
        String ci = readLf(".github/workflows/ci.yml");
        String push = ci.substring(ci.indexOf("  push:"), ci.indexOf("  pull_request:"));

        assertThat(push).contains("    paths-ignore:\n      - '**/*.md'\n      - 'docs/**'\n");
        assertThat(read("tools/ci/ci-passed.sh")).contains("ci=.github/workflows/ci.yml");
    }

    /**
     * Two NONPROD deploys lost their ssh session mid-pull (#375). Every session keeps itself alive,
     * the pull is retried in a fresh session, and `up -d` runs in a later step, so a failed pull
     * never replaces a running container.
     */
    @Test
    void nonprodDeploy_retriesThePullAndStartsOnlyAfterIt() throws IOException {
        String deploy = readLf(".github/workflows/deploy.yml");
        String pull = deploy.substring(deploy.indexOf("      - name: Pull the images"),
                deploy.indexOf("      - name: Start"));

        assertThat(deploy.split("-o StrictHostKeyChecking=yes", -1).length)
                .as("every ssh call sends keep-alives")
                .isEqualTo(deploy.split("-o ServerAliveInterval=30 -o ServerAliveCountMax=6", -1).length);
        assertThat(pull).contains("for attempt in 1 2 3; do").contains("pull --quiet");
        assertThat(pull).doesNotContain("up -d");
        assertThat(deploy.indexOf("up -d --no-build")).isGreaterThan(deploy.indexOf("      - name: Start"));
    }

    /** PROD is started by a person, and only with a green NONPROD deploy of main (#338). */
    @Test
    void prodDeploy_isManualAndPromotesOnlyNonprodReleasesOfMain() throws IOException {
        String prod = readLf(".github/workflows/deploy-prod.yml");

        assertThat(prod).contains("on:\n  workflow_dispatch:");
        assertThat(prod).doesNotContain("  push:").doesNotContain("  schedule:");
        assertThat(prod).contains("environment: production");
        assertThat(prod).contains("[ \"$GITHUB_REF\" = refs/heads/main ]");
        assertThat(prod).contains("[ \"$branch\" = main ]").contains("[ \"$conclusion\" = success ]");
        assertThat(prod).contains("--name nonprod-release");
        assertThat(prod).contains("ref: ${{ steps.release.outputs.sha }}");
    }

    /**
     * Run workflow can start any branch's copy of deploy-prod.yml. The workflow's own branch check
     * can be edited away on that branch, so the deploy role itself only trusts runs from main.
     */
    @Test
    void prodDeployRole_trustsOnlyRunsFromMain() throws IOException {
        String iam = readLf("infra/terraform/prod/iam.tf");

        assertThat(iam).contains("for_each = each.key == \"prod_deploy\" ? [\"refs/heads/main\"] : []");
        assertThat(iam).contains("variable = \"${local.github_oidc_url}:ref\"");
    }

    /**
     * The run id is typed by a person. Written as ${{ inputs.x }} inside a run block it would be
     * pasted into the shell script before bash ever sees it, so it reaches the steps through env.
     */
    @Test
    void prodDeploy_neverInterpolatesTheInputIntoAScript() throws IOException {
        String prod = read(".github/workflows/deploy-prod.yml");

        assertThat(prod.split("\\$\\{\\{ inputs\\.", -1)).hasSize(2);
        assertThat(prod).contains("NONPROD_RUN: ${{ inputs.nonprod_run }}");
    }

    /**
     * write-env.sh checks the new .env against the release's compose files before it touches
     * /opt/sas (GH-274), and start.sh stops before `up` when the images cannot be pulled.
     */
    @Test
    void prodScripts_stopBeforeChangingTheServer() throws IOException {
        String writeEnv = read("tools/prod/write-env.sh");
        String start = read("tools/prod/start.sh");

        assertThat(writeEnv.indexOf("missing=$(comm -23"))
                .isPositive()
                .isLessThan(writeEnv.indexOf("find /opt/sas -mindepth 1"));
        assertThat(start.indexOf("PULL FAILED"))
                .isPositive()
                .isLessThan(start.indexOf("docker compose $F up -d"));
        assertThat(start).contains("echo \"schema_before=").contains("echo \"schema_after=");
    }

    /**
     * RDS rejects connections without TLS, so a driver, URL or certificate problem would otherwise
     * first appear on PROD (#380). NONPROD's container now asks for TLS and its backend uses it.
     */
    @Test
    void nonprodDatabase_requiresTls() throws IOException {
        assertThat(readLf("docker-compose.deploy.yml"))
                .contains("- ssl=on")
                .contains("- hba_file=/etc/postgresql/pg_hba.conf")
                .contains("- ./tools/postgres/pg_hba.conf:/etc/postgresql/pg_hba.conf:ro")
                .contains("- DB_URL=jdbc:postgresql://db:5432/studentaccommodationlocaldb?sslmode=require");
    }

    /**
     * The order matters: Postgres uses the first line that matches. The network may only use TLS,
     * and the local socket stays open for the healthcheck, the deploy's schema check and backups.
     */
    @Test
    void nonprodDatabase_acceptsOnlyTlsFromTheNetwork() throws IOException {
        List<String> rules = readLf("tools/postgres/pg_hba.conf").lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.trim().replaceAll("\\s+", " "))
                .toList();

        assertThat(rules).containsExactly(
                "local all all trust",
                "local replication all trust",
                "hostssl all all all scram-sha-256",
                "hostnossl all all all reject");
    }

    /**
     * Local development keeps plain connections: only the deploy overlay turns TLS on.
     */
    @Test
    void localDatabase_staysWithoutTls() throws IOException {
        assertThat(readLf("docker-compose.yml"))
                .contains("- DB_URL=jdbc:postgresql://db:5432/studentaccommodationlocaldb\n")
                .doesNotContain("ssl=on");
    }
}
