package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GH-135: credentials and URLs carried development defaults, so a deployment that forgot to set
 * them started anyway with a known database account, known object-storage keys and localhost URLs.
 * Only {@code JWT_SECRET} failed fast. A stray {@code CorsFilter} also wrote
 * {@code Access-Control-Allow-Origin: *} on every response, which made Spring skip the allowlist.
 */
class ConfigurationDefaultsTest {

    private static final Path REPO_ROOT = Path.of("..");

    private static String applicationYml() throws IOException {
        return Files.readString(new ClassPathResource("application.yml").getFile().toPath(),
                StandardCharsets.UTF_8);
    }

    @Test
    void theWildcardCorsFilterIsGone() {
        // It ran at HIGHEST_PRECEDENCE and answered every OPTIONS with 200, so the allowlist in
        // SecurityConfig never applied.
        assertThatThrownBy(() -> Class.forName("com.beezhub.student_accommodation.config.CorsFilter"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "${USER_DB}", "${PASSWORD_DB}", "${MINIO_ACCESS_KEY}", "${MINIO_SECRET_KEY}",
            "${APP_BASE_URL}", "${ALLOWED_ORIGINS}"
    })
    void requiredSettingsHaveNoFallback(String placeholder) throws IOException {
        // A placeholder with no ":default" makes startup fail when the value is missing.
        assertThat(applicationYml())
                .as(placeholder + " must not carry a default")
                .contains(placeholder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"localadmin", "minioadmin"})
    void noDevelopmentCredentialRemainsAsADefault(String credential) throws IOException {
        assertThat(applicationYml()).doesNotContain(credential);
    }

    @Test
    void redisPasswordIsConfigurable() throws IOException {
        // There was no password property at all, so a Redis instance requiring auth was unreachable.
        assertThat(applicationYml()).contains("password: ${REDIS_PASSWORD:}");
    }

    @Test
    void theCspStorageOriginIsConfigurable() throws IOException {
        // It was hardcoded to http://localhost:9000, blocking presigned URLs everywhere else.
        assertThat(applicationYml()).contains("storage-public-origin: ${STORAGE_PUBLIC_ORIGIN:");

        String securityConfig = Files.readString(REPO_ROOT.resolve(
                "backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java"),
                StandardCharsets.UTF_8);
        assertThat(securityConfig).contains("storagePublicOrigin");
        assertThat(securityConfig)
                .as("the CSP must not pin a developer's storage port")
                .doesNotContain("http://localhost:9000");
    }

    @Test
    void corsOriginsComeFromTheSpringEnvironment() throws IOException {
        // System.getenv cannot be set by a profile file, a container secret or a test.
        String securityConfig = Files.readString(REPO_ROOT.resolve(
                "backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java"),
                StandardCharsets.UTF_8);

        assertThat(securityConfig).doesNotContain("System.getenv");
        assertThat(securityConfig).contains("app.security.allowed-origins");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALLOWED_ORIGINS", "STORAGE_PUBLIC_ORIGIN", "REDIS_PASSWORD"})
    void newSettingsAreDocumentedAndPassedThrough(String name) throws IOException {
        assertThat(Files.readString(REPO_ROOT.resolve(".env.example"), StandardCharsets.UTF_8))
                .contains(name);
        assertThat(Files.readString(REPO_ROOT.resolve("docker-compose.yml"), StandardCharsets.UTF_8))
                .contains(name);
    }
}
