package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.service.geocoding.AddressSuggestion;
import com.beezhub.student_accommodation.service.geocoding.GeocodingService;
import com.beezhub.student_accommodation.service.geocoding.ResolvedPlace;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeocodingController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("GeocodingController")
class GeocodingControllerTest {

    private static final String SESSION = "3f2b7c1e-9a4d-4e8b-b6a1-0c5d2e7f8a9b";

    @Autowired MockMvc mockMvc;

    @MockitoBean GeocodingService geocodingService;
    @MockitoBean com.beezhub.student_accommodation.security.user.AppUserDetailsService appUserDetailsService;
    @MockitoBean com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean RateLimitingFilter rateLimitingFilter;
    @MockitoBean com.beezhub.student_accommodation.security.MustChangePasswordFilter mustChangePasswordFilter;

    /** Pass-through filters so the URL rules and @PreAuthorize run; see WaitlistControllerTest. */
    @BeforeEach
    void passThroughMockedFilters() throws Exception {
        for (jakarta.servlet.Filter filter : List.of(
                jwtAuthenticationFilter, rateLimitingFilter, mustChangePasswordFilter)) {
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
    @WithMockUser(roles = "TENANT_ADMIN")
    void search_admin_passesQueryAndSessionThrough() throws Exception {
        when(geocodingService.search("32 Tsutsube", SESSION))
                .thenReturn(List.of(new AddressSuggestion("EiQz", "32 Tsutsube Street, Soweto")));

        mockMvc.perform(get("/api/geocoding/search").param("q", "32 Tsutsube").param("session", SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value("EiQz"))
                .andExpect(jsonPath("$[0].displayName").value("32 Tsutsube Street, Soweto"));
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void resolve_admin_returnsCoordinates() throws Exception {
        when(geocodingService.resolve("EiQz", SESSION)).thenReturn(new ResolvedPlace("EiQz", -26.25, 27.85));

        mockMvc.perform(get("/api/geocoding/places/EiQz").param("session", SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(-26.25))
                .andExpect(jsonPath("$.longitude").value(27.85));
    }

    /** The id goes into a URL at the provider, so only the characters a place id uses get through. */
    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void resolve_placeIdWithForeignCharacters_isBadRequestAndNotForwarded() throws Exception {
        mockMvc.perform(get("/api/geocoding/places/abc.def"))
                .andExpect(status().isBadRequest());

        verify(geocodingService, never()).resolve(anyString(), any());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void search_malformedSession_isBadRequest() throws Exception {
        mockMvc.perform(get("/api/geocoding/search").param("q", "1 Main Road").param("session", "not a token"))
                .andExpect(status().isBadRequest());

        verify(geocodingService, never()).search(anyString(), any());
    }

    /** Every call is billed, so a student cannot spend the budget. */
    @Test
    @WithMockUser(roles = "STUDENT")
    void resolve_student_isForbidden() throws Exception {
        mockMvc.perform(get("/api/geocoding/places/EiQz"))
                .andExpect(status().isForbidden());

        verify(geocodingService, never()).resolve(anyString(), any());
    }
}
