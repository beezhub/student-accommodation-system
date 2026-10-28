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

    @Test
    void newSettingsAreDocumented() throws IOException {
        String env = read(".env.example");

        assertThat(env).contains("APP_CONTACT_PHONE");
        assertThat(env).contains("REDIS_TIMEOUT_MS");
    }
}
