package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationSummaryDto;
import com.beezhub.student_accommodation.model.enums.OwnerRegistrationStatus;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.service.OwnerRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOwnerRegistrationController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("AdminOwnerRegistrationController tests")
class AdminOwnerRegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OwnerRegistrationService ownerRegistrationService;
    @MockitoBean private com.beezhub.student_accommodation.security.user.AppUserDetailsService appUserDetailsService;
    @MockitoBean private com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private RateLimitingFilter rateLimitingFilter;

    private OwnerRegistrationSummaryDto summaryDto;

    @BeforeEach
    void setUp() {
        summaryDto = OwnerRegistrationSummaryDto.builder()
                .id(1L)
                .fullName("Jane Smith")
                .email("jane@acme.com")
                .organisationName("Acme Properties")
                .status(OwnerRegistrationStatus.PENDING_REVIEW)
                .build();
    }

    // ── GET /api/admin/owner-registrations ──────────────────────────────────

    @Test
    @DisplayName("GET / returns 200 and list for PLATFORM_ADMIN")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void getPendingRegistrations_asAdmin_returns200() throws Exception {
        when(ownerRegistrationService.getPendingRegistrations()).thenReturn(List.of(summaryDto));

        mockMvc.perform(get("/api/admin/owner-registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("jane@acme.com"))
                .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"));

        verify(ownerRegistrationService).getPendingRegistrations();
    }

    @Test
    @DisplayName("GET / returns 403 for unauthenticated request")
    void getPendingRegistrations_unauthenticated_returns401or403() throws Exception {
        mockMvc.perform(get("/api/admin/owner-registrations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET / returns 403 for non-admin user")
    @WithMockUser(roles = "STUDENT")
    void getPendingRegistrations_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/owner-registrations"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/admin/owner-registrations/{id}/approve ────────────────────

    @Test
    @DisplayName("POST /{id}/approve returns 200 for PLATFORM_ADMIN")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void approve_asAdmin_returns200() throws Exception {
        doNothing().when(ownerRegistrationService).approve(1L);

        mockMvc.perform(post("/api/admin/owner-registrations/1/approve"))
                .andExpect(status().isOk());

        verify(ownerRegistrationService).approve(1L);
    }

    @Test
    @DisplayName("POST /{id}/approve returns 404 when registration not found")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void approve_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Owner registration not found"))
                .when(ownerRegistrationService).approve(99L);

        mockMvc.perform(post("/api/admin/owner-registrations/99/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /{id}/approve returns 400 when invalid status transition")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void approve_invalidStatus_returns400() throws Exception {
        doThrow(new InvalidStatusException("Registration must be in PENDING_REVIEW status"))
                .when(ownerRegistrationService).approve(1L);

        mockMvc.perform(post("/api/admin/owner-registrations/1/approve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /{id}/approve returns 403 for non-admin")
    @WithMockUser(roles = "BUILDING_OWNER")
    void approve_asOwner_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/owner-registrations/1/approve"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/admin/owner-registrations/{id}/reject ─────────────────────

    @Test
    @DisplayName("POST /{id}/reject returns 200 with reason")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void reject_withReason_returns200() throws Exception {
        doNothing().when(ownerRegistrationService).reject(eq(1L), eq("Insufficient docs"));

        Map<String, String> body = Map.of("reason", "Insufficient docs");
        mockMvc.perform(post("/api/admin/owner-registrations/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(ownerRegistrationService).reject(eq(1L), eq("Insufficient docs"));
    }

    @Test
    @DisplayName("POST /{id}/reject returns 200 with no body (null reason)")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void reject_noBody_returns200WithNullReason() throws Exception {
        doNothing().when(ownerRegistrationService).reject(eq(1L), isNull());

        mockMvc.perform(post("/api/admin/owner-registrations/1/reject"))
                .andExpect(status().isOk());

        verify(ownerRegistrationService).reject(eq(1L), isNull());
    }

    @Test
    @DisplayName("POST /{id}/reject returns 404 when registration not found")
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void reject_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Owner registration not found"))
                .when(ownerRegistrationService).reject(eq(99L), any());

        mockMvc.perform(post("/api/admin/owner-registrations/99/reject"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /{id}/reject returns 403 for non-admin")
    @WithMockUser(roles = "STUDENT")
    void reject_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/owner-registrations/1/reject"))
                .andExpect(status().isForbidden());
    }
}
