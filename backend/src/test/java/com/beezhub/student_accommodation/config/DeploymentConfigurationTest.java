package com.beezhub.student_accommodation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

        for (String service : List.of("db", "minio", "minio-volume-permissions", "prometheus",
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
}
