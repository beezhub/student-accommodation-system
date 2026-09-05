package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-134: everything under {@code /actuator/**} except health required {@code PLATFORM_ADMIN} while
 * the Prometheus job carried no credentials, so every scrape was rejected and the Grafana
 * dashboards and alerts stayed empty.
 *
 * <p>The wiring spans the backend, the scrape config and compose, and the three have to agree. A
 * running scrape can only be proved with the container stack, so these checks pin the agreement.
 */
class MetricsScrapeSecurityTest {

    private static final Path REPO_ROOT = Path.of("..");

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void theScrapeChainIsRegisteredOnlyWhenAPasswordIsConfigured() throws NoSuchMethodException {
        // Otherwise an unconfigured deployment would gain an account with a blank password.
        Method chain = SecurityConfig.class.getMethod("metricsScrapeFilterChain",
                org.springframework.security.config.annotation.web.builders.HttpSecurity.class,
                String.class, String.class);

        var condition = chain.getAnnotation(
                org.springframework.boot.autoconfigure.condition.ConditionalOnExpression.class);
        assertThat(condition).isNotNull();
        assertThat(condition.value()).contains("management.metrics.scrape.password");
    }

    @Test
    void theScrapeChainIsMatchedBeforeTheMainChain() throws NoSuchMethodException {
        int scrapeOrder = SecurityConfig.class.getMethod("metricsScrapeFilterChain",
                        org.springframework.security.config.annotation.web.builders.HttpSecurity.class,
                        String.class, String.class)
                .getAnnotation(org.springframework.core.annotation.Order.class).value();
        int mainOrder = SecurityConfig.class.getMethod("securityFilterChain",
                        org.springframework.security.config.annotation.web.builders.HttpSecurity.class)
                .getAnnotation(org.springframework.core.annotation.Order.class).value();

        assertThat(scrapeOrder).isLessThan(mainOrder);
    }

    @Test
    void theBackendDefaultLeavesTheEndpointAdminOnly() throws IOException {
        String applicationYml = read(new ClassPathResource("application.yml").getFile().toPath());

        assertThat(applicationYml).contains("PROMETHEUS_SCRAPE_PASSWORD:}");
    }

    @Test
    void prometheusSendsCredentialsFromAMountedFile() throws IOException {
        String scrapeConfig = read(REPO_ROOT.resolve("tools/prometheus/prometheus.yml"));

        assertThat(scrapeConfig).contains("basic_auth");
        assertThat(scrapeConfig)
                .as("the password must come from a file so it is never committed")
                .contains("password_file")
                .doesNotContain("password:");
    }

    @Test
    void composeMountsThePasswordFileAndPassesTheCredentials() throws IOException {
        String compose = read(REPO_ROOT.resolve("docker-compose.yml"));

        assertThat(compose).contains("scrape-password:/etc/prometheus/scrape-password");
        assertThat(compose).contains("PROMETHEUS_SCRAPE_PASSWORD");
    }

    @Test
    void theCommittedPasswordFileHoldsNoWorkingCredential() throws IOException {
        // It exists so Prometheus starts locally; it grants nothing because the backend default is
        // empty, which means no scrape account is registered at all.
        String committed = read(REPO_ROOT.resolve("tools/prometheus/scrape-password")).trim();

        assertThat(committed).contains("change-me");
    }
}
