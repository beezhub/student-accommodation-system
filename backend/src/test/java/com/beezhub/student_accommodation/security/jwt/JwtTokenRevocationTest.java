package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.config.JwtConfig;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.security.FilterErrorResponseWriter;
import com.beezhub.student_accommodation.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GH-122: deactivating an account left its already-issued token working until expiry, and only
 * PLATFORM_ADMIN received the shorter admin token lifetime.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtTokenRevocationTest {

    private static final String SECRET =
            "test-secret-for-unit-tests-only-must-be-at-least-64-chars-long-padding";
    private static final long STUDENT_MS = 36_000_000L; // 10h
    private static final long ADMIN_MS = 14_400_000L;   // 4h

    private final JwtUtil realJwtUtil = new JwtUtil(jwtConfig());

    private static JwtConfig jwtConfig() {
        return new JwtConfig(SECRET, String.valueOf(STUDENT_MS), String.valueOf(ADMIN_MS));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static AppUserDetails details(UserRole role) {
        return new AppUserDetails("user@example.com", "pw", role.name(), 1L);
    }

    private long lifetimeMsOf(String token, JwtUtil util) {
        Date expiry = util.extractExpiration(token);
        Instant issued = util.extractIssuedAt(token);
        return expiry.getTime() - issued.toEpochMilli();
    }

    // ── token lifetime by role ───────────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"PLATFORM_ADMIN", "TENANT_ADMIN", "ADMIN"})
    void adminRoles_getTheShorterAdminLifetime(UserRole role) {
        String token = realJwtUtil.generateToken(details(role));

        assertThat(lifetimeMsOf(token, realJwtUtil)).isEqualTo(ADMIN_MS);
    }

    @Test
    void student_keepsTheLongerLifetime() {
        String token = realJwtUtil.generateToken(details(UserRole.STUDENT));

        assertThat(lifetimeMsOf(token, realJwtUtil)).isEqualTo(STUDENT_MS);
    }

    @Test
    void buildingStaff_isNotAnAdminRole_soKeepsTheLongerLifetime() {
        // BUILDING_STAFF is outside UserRole.isAdmin(), so it keeps the standard lifetime.
        String token = realJwtUtil.generateToken(details(UserRole.BUILDING_STAFF));

        assertThat(lifetimeMsOf(token, realJwtUtil)).isEqualTo(STUDENT_MS);
    }

    @Test
    void issuedAt_isReadableFromTheToken() {
        Instant before = Instant.now().minusSeconds(1);

        Instant issuedAt = realJwtUtil.extractIssuedAt(realJwtUtil.generateToken(details(UserRole.STUDENT)));

        assertThat(issuedAt).isAfter(before);
    }

    // ── revocation enforced by the filter ────────────────────────────────────

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private FilterChain chain;

    @Spy
    private FilterErrorResponseWriter errorResponseWriter =
            new FilterErrorResponseWriter(new ObjectMapper().registerModule(new JavaTimeModule()));

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest bearerRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.setRequestURI("/api/admin/users");
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    @Test
    void revokedToken_isRejectedAndNeverAuthenticates() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(600);
        when(jwtUtil.extractJti("token")).thenReturn("jti");
        when(tokenBlacklistService.isBlacklisted("jti")).thenReturn(false);
        when(jwtUtil.extractUsername("token")).thenReturn("user@example.com");
        when(jwtUtil.extractIssuedAt("token")).thenReturn(issuedAt);
        when(tokenBlacklistService.isRevoked("user@example.com", issuedAt)).thenReturn(true);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(bearerRequest(), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Asserting the response, not the delegation: delegating to the resolver was exactly what
        // produced no response at all (GH-204).
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication failed");
    }

    @Test
    void tokenIssuedAfterTheCutoff_stillAuthenticates() throws Exception {
        Instant issuedAt = Instant.now();
        when(jwtUtil.extractJti("token")).thenReturn("jti");
        when(tokenBlacklistService.isBlacklisted("jti")).thenReturn(false);
        when(jwtUtil.extractUsername("token")).thenReturn("user@example.com");
        when(jwtUtil.extractIssuedAt("token")).thenReturn(issuedAt);
        when(tokenBlacklistService.isRevoked("user@example.com", issuedAt)).thenReturn(false);
        when(jwtUtil.validateToken("token", "user@example.com")).thenReturn(true);
        when(jwtUtil.extractRoles("token")).thenReturn(java.util.List.of("ROLE_TENANT_ADMIN"));
        when(jwtUtil.extractTenantId("token")).thenReturn(1L);

        filter.doFilter(bearerRequest(), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(any(), any());
    }
}
