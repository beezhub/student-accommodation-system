package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.exceptions.RestResponseExceptionHandler;
import com.beezhub.student_accommodation.exceptions.UnassignedAdminUnlockException;
import com.beezhub.student_accommodation.security.MustChangePasswordFilter;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter;
import com.beezhub.student_accommodation.security.user.AppUserDetailsService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.UserManagementService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The unlock endpoint as a platform admin calls it: method security, the service, the handler. */
@WebMvcTest(UserManagementController.class)
@Import({ SecurityConfig.class, RestResponseExceptionHandler.class })
@AutoConfigureMockMvc
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @MockitoBean
    private MustChangePasswordFilter mustChangePasswordFilter;

    /** Pass-through filters so the URL rules and @PreAuthorize run; see GeocodingControllerTest. */
    @BeforeEach
    void passThroughMockedFilters() throws Exception {
        for (Filter filter : List.of(jwtAuthenticationFilter, rateLimitingFilter, mustChangePasswordFilter)) {
            doAnswer(invocation -> {
                ServletRequest request = invocation.getArgument(0);
                ServletResponse response = invocation.getArgument(1);
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(request, response);
                return null;
            }).when(filter).doFilter(any(), any(), any());
        }
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void unlockAccount_lockedOutUser_ok() throws Exception {
        mockMvc.perform(put("/api/admin/users/5/unlock").with(csrf()))
                .andExpect(status().isOk());

        verify(authService).unlockAccount(5L);
    }

    /** #412: the refusal reaches the admin as a 409 carrying the service's message, not a 500. */
    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void unlockAccount_adminWithNoCompany_conflictWithTheWayForward() throws Exception {
        doThrow(new UnassignedAdminUnlockException("Thandi Mokoena is not linked to a company yet."))
                .when(authService).unlockAccount(7L);

        mockMvc.perform(put("/api/admin/users/7/unlock").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errors[0]").value("Thandi Mokoena is not linked to a company yet."));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void unlockAccount_tenantAdmin_forbidden() throws Exception {
        mockMvc.perform(put("/api/admin/users/5/unlock").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
