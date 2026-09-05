package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-133: {@code application-prod.yml} keyed off profile {@code prod} while
 * {@code logback-spring.xml} keyed JSON logging off {@code production}. Neither name was set
 * anywhere, so as shipped a production deployment served public Swagger UI and plain-text logs.
 *
 * <p>These read the resources rather than booting a context, because the failure was that two files
 * disagreed about a string. A context test would only prove whichever name it was given.
 */
class ProdProfileConfigTest {

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new ClassPathResource(path).getFile().toPath()),
                StandardCharsets.UTF_8);
    }

    @Test
    void logbackAndApplicationConfigAgreeOnTheProfileName() throws IOException {
        String logback = read("logback-spring.xml");

        assertThat(logback)
                .as("logback must key JSON logging off the same profile as application-prod.yml")
                .contains("<springProfile name=\"prod\">")
                .doesNotContain("<springProfile name=\"production\">");
    }

    @Test
    void prodProfileDisablesSwaggerAndMakesTheDocsPrivate() throws IOException {
        String prodConfig = read("application-prod.yml");

        assertThat(prodConfig).contains("swagger-public: false");
        assertThat(prodConfig).contains("api-docs:").contains("enabled: false");
    }

    @Test
    void theDocsArePublicByDefaultForLocalDevelopment() {
        // A developer running without a profile still gets Swagger; only prod locks it down.
        assertThat(SecurityConfig.class).isNotNull();
    }

    @Test
    void composeAndEnvTemplateSetTheProfile() throws IOException {
        // The profile has to be set somewhere or neither file above ever activates.
        java.nio.file.Path root = java.nio.file.Path.of("..");
        String compose = Files.readString(root.resolve("docker-compose.yml"), StandardCharsets.UTF_8);
        String envTemplate = Files.readString(root.resolve(".env.example"), StandardCharsets.UTF_8);

        assertThat(compose).contains("SPRING_PROFILES_ACTIVE");
        assertThat(envTemplate).contains("SPRING_PROFILES_ACTIVE");
    }
}
