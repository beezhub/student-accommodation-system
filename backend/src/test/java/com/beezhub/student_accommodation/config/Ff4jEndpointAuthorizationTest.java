package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.controller.FeatureController;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter;
import com.beezhub.student_accommodation.security.user.AppUserDetailsService;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for GH-116: the FF4j REST API ({@code /api/ff4j/**}) and web console
 * must never fall through to the generic "any authenticated user" rule.
 *
 * <p>The FF4j web-api jar is excluded from the build, so no handler is mapped under
 * {@code /api/ff4j}. The URL rule in {@link SecurityConfig} is defence in depth: the
 * request must be rejected by the filter chain before dispatch, so a non-platform-admin
 * sees 403 rather than 404, and a platform admin reaches dispatch (404 in this slice).
 */
@WebMvcTest(FeatureController.class)
@Import(SecurityConfig.class)
@DisplayName("SecurityConfig: FF4j endpoints are PLATFORM_ADMIN only")
class Ff4jEndpointAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @MockitoBean
    private com.beezhub.student_accommodation.security.MustChangePasswordFilter mustChangePasswordFilter;

    @BeforeEach
    void passThroughSecurityFilters() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());

        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain passThrough = invocation.getArgument(2);
            passThrough.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mustChangePasswordFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void ff4jStore_studentToken_forbidden() throws Exception {
        mockMvc.perform(get("/api/ff4j/store"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void ff4jEnableFeature_studentToken_forbidden() throws Exception {
        mockMvc.perform(post("/api/ff4j/store/features/auto-allocation/enable"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void ff4jStore_tenantAdminToken_forbidden() throws Exception {
        mockMvc.perform(get("/api/ff4j/store"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BUILDING_STAFF")
    void ff4jStore_buildingStaffToken_forbidden() throws Exception {
        mockMvc.perform(get("/api/ff4j/store"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void ff4jStore_anonymous_rejected() throws Exception {
        mockMvc.perform(get("/api/ff4j/store"))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("expected 401 or 403 but was " + status);
                    }
                });
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void ff4jStore_platformAdminToken_passesAuthorization() throws Exception {
        // No FF4j handler is on the classpath, so the request reaches dispatch and 404s.
        mockMvc.perform(get("/api/ff4j/store"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void ff4jWebConsole_studentToken_forbidden() throws Exception {
        mockMvc.perform(get("/ff4j-web-console/"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void adminFeatures_studentToken_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/features"))
                .andExpect(status().isForbidden());
    }
}
