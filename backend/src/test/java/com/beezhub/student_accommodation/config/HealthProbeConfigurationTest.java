package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-147: the only health surface was the aggregate {@code /actuator/health}, which invokes every
 * registered contributor: SMTP opens a socket, storage checks the bucket, Redis pings, and Boot adds
 * a full mail connection test. Any one DOWN made the aggregate 503, and the container HEALTHCHECK
 * pointed at it, so an SMTP outage restarted a perfectly healthy application.
 *
 * <p>These pin agreement between configuration files. They do not test behaviour: that liveness
 * stays up with SMTP down, that readiness fails only on database loss, and that a SIGTERM drains
 * inside the grace period all need a running container and are verifiable in CI only.
 */
class HealthProbeConfigurationTest {

    private static final Path REPO_ROOT = Path.of("..");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> applicationYml() throws IOException {
        try (InputStream in = new ClassPathResource("application.yml").getInputStream()) {
            return new Yaml().loadAs(in, Map.class);
        }
    }

    /** Walks a dotted path, tolerating the mixed flat and nested style application.yml uses. */
    @SuppressWarnings("unchecked")
    private static Object at(Map<String, Object> root, String dottedPath) {
        Object current = root;
        StringBuilder remaining = new StringBuilder(dottedPath);
        while (remaining.length() > 0) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            String key = null;
            for (String candidate : ((Map<String, Object>) map).keySet()) {
                if (remaining.toString().equals(candidate) || remaining.toString().startsWith(candidate + ".")) {
                    if (key == null || candidate.length() > key.length()) {
                        key = candidate;
                    }
                }
            }
            if (key == null) {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
            remaining.delete(0, Math.min(key.length() + 1, remaining.length()));
        }
        return current;
    }

    @Test
    void probes_areEnabled() throws IOException {
        // Boot only auto-enables these when it detects Kubernetes or Cloud Foundry.
        assertThat(at(applicationYml(), "management.endpoint.health.probes.enabled")).isEqualTo(true);
    }

    /**
     * Liveness answers one question: is this JVM broken beyond recovery. Anything external here
     * would have a restart used as the response to someone else's outage.
     */
    @Test
    void liveness_dependsOnNothingExternal() throws IOException {
        assertThat(at(applicationYml(), "management.endpoint.health.group.liveness.include"))
                .isEqualTo("livenessState");
    }

    /**
     * Readiness adds the one dependency the application genuinely cannot serve without. Redis is
     * excluded on purpose: blacklist checks fail open and the rate-limit filter swallows Redis
     * errors, so an outage keeps traffic flowing with revocation and auth rate limiting disabled.
     */
    @Test
    void readiness_coversTheDatabaseAndNothingOptional() throws IOException {
        String include = String.valueOf(at(applicationYml(), "management.endpoint.health.group.readiness.include"));

        assertThat(include).contains("readinessState").contains("db");
        assertThat(include).doesNotContain("smtp").doesNotContain("storage")
                .doesNotContain("redis").doesNotContain("mail");
    }

    @Test
    void healthDetails_areNeverShown() throws IOException {
        assertThat(at(applicationYml(), "management.endpoint.health.show-details")).isEqualTo("never");
    }

    @Test
    void shutdown_isGraceful() throws IOException {
        assertThat(at(applicationYml(), "server.shutdown")).isEqualTo("graceful");
    }

    /** The container check must not be the aggregate, which is the whole point of the change. */
    @Test
    void dockerHealthcheck_usesReadinessNotTheAggregate() throws IOException {
        String dockerfile = Files.readString(REPO_ROOT.resolve("backend/Dockerfile"), StandardCharsets.UTF_8);

        assertThat(dockerfile).contains("/actuator/health/readiness");
        assertThat(dockerfile)
                .as("the HEALTHCHECK line must not point at the aggregate")
                .doesNotContain("spider http://localhost:8080/actuator/health |");
    }

    /**
     * Without this the JVM takes its own default share of the container limit, which is 25% — about
     * 256MB under the 1G compose limit.
     */
    @Test
    void dockerfile_setsAHeapPercentageAndTimezone() throws IOException {
        String dockerfile = Files.readString(REPO_ROOT.resolve("backend/Dockerfile"), StandardCharsets.UTF_8);

        assertThat(dockerfile).contains("MaxRAMPercentage");
        assertThat(dockerfile).contains("ExitOnOutOfMemoryError");
        assertThat(dockerfile).contains("ENV TZ=");
        assertThat(dockerfile)
                .as("-Duser.timezone would beat the TZ env var and make APP_TIMEZONE inert")
                .doesNotContain("-Duser.timezone");
    }

    /** Docker must not SIGKILL the container before graceful shutdown has finished draining. */
    @Test
    void compose_allowsTimeForGracefulShutdown() throws IOException {
        String compose = Files.readString(REPO_ROOT.resolve("docker-compose.yml"), StandardCharsets.UTF_8);

        assertThat(compose).contains("stop_grace_period");
        assertThat(compose).contains("TZ=${APP_TIMEZONE:-UTC}");
    }

    @Test
    void timezone_isDocumented() throws IOException {
        assertThat(Files.readString(REPO_ROOT.resolve(".env.example"), StandardCharsets.UTF_8))
                .contains("APP_TIMEZONE");
    }
}
