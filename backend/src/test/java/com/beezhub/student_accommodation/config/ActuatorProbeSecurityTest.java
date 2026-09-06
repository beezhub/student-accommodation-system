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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GH-147: the probe paths are new, and an orchestrator calls them without credentials. If the
 * filter chain rejected them the container would be restarted forever while the application was
 * perfectly healthy.
 *
 * <p>No actuator endpoint is mapped in a web slice, so an anonymous request that gets past the
 * chain reaches dispatch and returns 404. That is the assertion: 404 means the rule let it through,
 * 403 means it did not. The distinction is the whole point.
 */
@WebMvcTest(FeatureController.class)
@Import(SecurityConfig.class)
@DisplayName("SecurityConfig: health probes are anonymous, the rest of actuator is not")
class ActuatorProbeSecurityTest {

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

    /** A bare mock swallows the chain and every request returns 200 regardless of the rules. */
    @BeforeEach
    void passThroughSecurityFilters() throws Exception {
        for (var filter : new jakarta.servlet.Filter[] {
                jwtAuthenticationFilter, rateLimitingFilter, mustChangePasswordFilter }) {
            doAnswer(invocation -> {
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(filter).doFilter(any(), any(), any());
        }
    }

    @Test
    @WithAnonymousUser
    void liveness_isReachableWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void readiness_isReachableWithoutCredentials() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void aggregateHealth_staysReachable() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
    }

    /**
     * The probe paths are listed individually rather than matched with {@code /actuator/health/**},
     * so a component health path is not anonymous just because it sits under health.
     */
    @Test
    @WithAnonymousUser
    void componentHealthPaths_areNotAnonymous() throws Exception {
        mockMvc.perform(get("/actuator/health/db")).andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void metrics_remainRestricted() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isForbidden());
    }
}
