package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.WaitlistPositionDto;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WaitlistController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("WaitlistController")
class WaitlistControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean WaitingListService waitingListService;
    @MockitoBean com.beezhub.student_accommodation.security.user.AppUserDetailsService appUserDetailsService;
    @MockitoBean com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean RateLimitingFilter rateLimitingFilter;
    @MockitoBean com.beezhub.student_accommodation.security.MustChangePasswordFilter mustChangePasswordFilter;

    /**
     * The servlet filters are @MockitoBean'd so the security chain can be built
     * without their real dependencies. A bare mock swallows the request (it
     * never calls chain.doFilter), bypassing authorization entirely. Stub both
     * as pass-through so Spring Security's URL rules and @PreAuthorize checks
     * actually run — see TenantAdminControllerTest for the same pattern.
     */
    @BeforeEach
    void passThroughMockedFilters() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
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
    @DisplayName("GET /api/waitlist/position — 200 for student on the waiting list")
    void getPosition_studentOnWaitlist_returns200() throws Exception {
        WaitlistPositionDto dto = WaitlistPositionDto.builder()
                .position(3)
                .totalInQueue(12)
                .estimatedWaitDays(6)
                .build();
        when(waitingListService.getMyWaitlistPosition()).thenReturn(dto);

        mockMvc.perform(get("/api/waitlist/position"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(3))
                .andExpect(jsonPath("$.totalInQueue").value(12))
                .andExpect(jsonPath("$.estimatedWaitDays").value(6));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/waitlist/position — 404 when student is not on the waiting list")
    void getPosition_studentNotOnWaitlist_returns404() throws Exception {
        when(waitingListService.getMyWaitlistPosition())
                .thenThrow(new ResourceNotFoundException("Student is not currently on the waiting list"));

        mockMvc.perform(get("/api/waitlist/position"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("GET /api/waitlist/position — 403 for non-student role")
    void getPosition_nonStudentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/waitlist/position"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/waitlist/position — 403 for unauthenticated (no 401 entry point configured)")
    void getPosition_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/waitlist/position"))
                .andExpect(status().isForbidden());
    }
}
