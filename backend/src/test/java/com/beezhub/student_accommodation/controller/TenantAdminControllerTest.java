package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.model.dto.TenantAdminRequestDto;
import com.beezhub.student_accommodation.model.dto.TenantAdminResponseDto;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.service.TenantAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantAdminController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("TenantAdminController")
class TenantAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean TenantAdminService tenantAdminService;
    @MockitoBean com.beezhub.student_accommodation.security.user.AppUserDetailsService appUserDetailsService;
    @MockitoBean com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean RateLimitingFilter rateLimitingFilter;

    private static final Long TENANT_ID = 1L;

    private TenantAdminResponseDto sampleResponse() {
        return new TenantAdminResponseDto(
                10L, "admin@tenant.com", "Jane", "Smith",
                LocalDateTime.now(), true, null);
    }

    // ── List admins ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("GET /api/admin/tenants/{id}/admins — 200 for PLATFORM_ADMIN")
    void listAdmins_platformAdmin_returns200() throws Exception {
        when(tenantAdminService.listTenantAdmins(TENANT_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/admin/tenants/{id}/admins", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@tenant.com"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    @DisplayName("GET /api/admin/tenants/{id}/admins — 403 for TENANT_ADMIN")
    void listAdmins_tenantAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/tenants/{id}/admins", TENANT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/tenants/{id}/admins — 401 for unauthenticated")
    void listAdmins_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/tenants/{id}/admins", TENANT_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── Create admin ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("POST /api/admin/tenants/{id}/admins — 201 for PLATFORM_ADMIN")
    void createAdmin_platformAdmin_returns201() throws Exception {
        TenantAdminRequestDto request = new TenantAdminRequestDto(
                "Jane", "Smith", "admin@tenant.com", "Password1!");
        when(tenantAdminService.createTenantAdmin(eq(TENANT_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/admin/tenants/{id}/admins", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@tenant.com"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    @DisplayName("POST /api/admin/tenants/{id}/admins — 403 for TENANT_ADMIN")
    void createAdmin_tenantAdmin_returns403() throws Exception {
        TenantAdminRequestDto request = new TenantAdminRequestDto(
                "Jane", "Smith", "admin@tenant.com", "Password1!");

        mockMvc.perform(post("/api/admin/tenants/{id}/admins", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("POST /api/admin/tenants/{id}/admins — 400 for invalid request body")
    void createAdmin_invalidBody_returns400() throws Exception {
        // Missing required fields
        String badBody = "{}";
        mockMvc.perform(post("/api/admin/tenants/{id}/admins", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badBody))
                .andExpect(status().isBadRequest());
    }

    // ── Deactivate admin ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    @DisplayName("DELETE /api/admin/tenants/{id}/admins/{userId} — 200 for PLATFORM_ADMIN")
    void deactivateAdmin_platformAdmin_returns200() throws Exception {
        doNothing().when(tenantAdminService).deactivateTenantAdmin(TENANT_ID, 10L);

        mockMvc.perform(delete("/api/admin/tenants/{id}/admins/{userId}", TENANT_ID, 10L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    @DisplayName("DELETE /api/admin/tenants/{id}/admins/{userId} — 403 for TENANT_ADMIN")
    void deactivateAdmin_tenantAdmin_returns403() throws Exception {
        mockMvc.perform(delete("/api/admin/tenants/{id}/admins/{userId}", TENANT_ID, 10L))
                .andExpect(status().isForbidden());
    }
}
