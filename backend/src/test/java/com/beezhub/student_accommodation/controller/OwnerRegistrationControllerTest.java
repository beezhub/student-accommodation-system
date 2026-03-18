package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.InvalidTokenException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.TokenExpiredException;
import com.beezhub.student_accommodation.model.dto.OwnerRegistrationResponse;
import com.beezhub.student_accommodation.model.dto.SetOwnerPasswordRequest;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.service.OwnerRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OwnerRegistrationController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("OwnerRegistrationController tests")
class OwnerRegistrationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OwnerRegistrationService ownerRegistrationService;
    @MockitoBean private com.beezhub.student_accommodation.security.user.AppUserDetailsService appUserDetailsService;
    @MockitoBean private com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUpFilters() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    private Map<String, String> validRegisterBody() {
        return Map.of(
                "fullName", "Jane Smith",
                "email", "jane@acme.com",
                "organisationName", "Acme Properties"
        );
    }

    // ── POST /api/public/owner/register ──────────────────────────────────────

    @Test
    @DisplayName("POST /register returns 201 with registration response")
    void register_validRequest_returns201() throws Exception {
        OwnerRegistrationResponse response = OwnerRegistrationResponse.builder()
                .email("jane@acme.com")
                .build();
        when(ownerRegistrationService.submit(any())).thenReturn(response);

        mockMvc.perform(post("/api/public/owner/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@acme.com"));

        verify(ownerRegistrationService).submit(any());
    }

    @Test
    @DisplayName("POST /register returns 400 when required field missing")
    void register_missingField_returns400() throws Exception {
        mockMvc.perform(post("/api/public/owner/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jane@acme.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register returns 409 on duplicate registration")
    void register_duplicate_returns409() throws Exception {
        when(ownerRegistrationService.submit(any()))
                .thenThrow(new DuplicateResourceException("pending registration already exists"));

        mockMvc.perform(post("/api/public/owner/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterBody())))
                .andExpect(status().isConflict());
    }

    // ── GET /api/public/owner/verify-email ───────────────────────────────────

    @Test
    @DisplayName("GET /verify-email returns 200 for valid token")
    void verifyEmail_validToken_returns200() throws Exception {
        doNothing().when(ownerRegistrationService).verifyEmail("good-token");

        mockMvc.perform(get("/api/public/owner/verify-email")
                        .param("token", "good-token"))
                .andExpect(status().isOk());

        verify(ownerRegistrationService).verifyEmail("good-token");
    }

    @Test
    @DisplayName("GET /verify-email returns 400 for invalid token")
    void verifyEmail_invalidToken_returns400() throws Exception {
        doThrow(new InvalidTokenException("Invalid email verification token"))
                .when(ownerRegistrationService).verifyEmail("bad-token");

        mockMvc.perform(get("/api/public/owner/verify-email")
                        .param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /verify-email returns 400 for expired token")
    void verifyEmail_expiredToken_returns400() throws Exception {
        doThrow(new TokenExpiredException("This verification link has expired"))
                .when(ownerRegistrationService).verifyEmail("expired");

        mockMvc.perform(get("/api/public/owner/verify-email")
                        .param("token", "expired"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/public/owner/resend-verification ────────────────────────────

    @Test
    @DisplayName("POST /resend-verification returns 200 when email found")
    void resendVerification_emailFound_returns200() throws Exception {
        doNothing().when(ownerRegistrationService).resendVerification("jane@acme.com");

        mockMvc.perform(post("/api/public/owner/resend-verification")
                        .param("email", "jane@acme.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /resend-verification returns 404 when no pending registration exists")
    void resendVerification_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("No pending registration"))
                .when(ownerRegistrationService).resendVerification(anyString());

        mockMvc.perform(post("/api/public/owner/resend-verification")
                        .param("email", "nobody@acme.com"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/public/owner/set-password ──────────────────────────────────

    @Test
    @DisplayName("POST /set-password returns 200 for valid token and password")
    void setPassword_validRequest_returns200() throws Exception {
        doNothing().when(ownerRegistrationService).setPassword(any());

        Map<String, String> body = Map.of("token", "setup-token", "password", "newPass123");
        mockMvc.perform(post("/api/public/owner/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /set-password returns 400 for expired token")
    void setPassword_expiredToken_returns400() throws Exception {
        doThrow(new TokenExpiredException("Password setup link has expired"))
                .when(ownerRegistrationService).setPassword(any());

        Map<String, String> body = Map.of("token", "old-token", "password", "newPass123");
        mockMvc.perform(post("/api/public/owner/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
