package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtConfig} fail-fast validation.
 *
 * <p>Verifies that the application refuses to start when {@code JWT_SECRET} is absent,
 * blank, or shorter than the minimum required length of 32 characters.
 *
 * <p>These tests exercise the {@code @PostConstruct} method directly (by constructing
 * the component and calling {@code validateSecurityConfiguration()} explicitly) so that
 * a full Spring application context is not required.
 */
class JwtConfigTest {

    private static final String VALID_SECRET =
            "jvc+QC/0myyJ9+Rzo+qYxfMmHDiQdlysvcT9XhUF7/6mvHD9evZBB/ngNAMwTD";
    private static final String EXPIRATION = "36000000";
    private static final String ADMIN_EXPIRATION = "14400000";

    // ─────────────────────────────────────────────────────────────────
    // TASK-1.04: Fail-fast startup checks
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("startup_withoutJwtSecret_throwsIllegalStateException")
    void startup_withoutJwtSecret_throwsIllegalStateException() {
        // @Value cannot inject null via constructor in tests, but an empty string is the closest
        // equivalent to a missing/unset environment variable placeholder
        JwtConfig config = new JwtConfig("", EXPIRATION, ADMIN_EXPIRATION);

        assertThatThrownBy(config::validateSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("startup_withBlankJwtSecret_throwsIllegalStateException")
    void startup_withBlankJwtSecret_throwsIllegalStateException() {
        JwtConfig config = new JwtConfig("   ", EXPIRATION, ADMIN_EXPIRATION);

        assertThatThrownBy(config::validateSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("startup_withShortJwtSecret_throwsIllegalStateException")
    void startup_withShortJwtSecret_throwsIllegalStateException() {
        // 31 characters — one short of the minimum 32
        JwtConfig config = new JwtConfig("this-is-only-31-chars-long-----", EXPIRATION, ADMIN_EXPIRATION);

        assertThatThrownBy(config::validateSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("32 characters");
    }

    @Test
    @DisplayName("startup_withValidJwtSecret_succeeds")
    void startup_withValidJwtSecret_succeeds() {
        JwtConfig config = new JwtConfig(VALID_SECRET, EXPIRATION, ADMIN_EXPIRATION);

        assertThatCode(config::validateSecurityConfiguration)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("startup_withExactly32CharSecret_succeeds")
    void startup_withExactly32CharSecret_succeeds() {
        String exactly32Chars = "12345678901234567890123456789012"; // exactly 32 chars
        JwtConfig config = new JwtConfig(exactly32Chars, EXPIRATION, ADMIN_EXPIRATION);

        assertThatCode(config::validateSecurityConfiguration)
                .doesNotThrowAnyException();
    }
}
