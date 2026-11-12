package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.model.dto.ContactInfoResponse;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.service.ContactInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactInfoController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ContactInfoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ContactInfoService contactInfoService;
    @MockitoBean com.beezhub.student_accommodation.security.user.AppUserDetailsService appUserDetailsService;
    @MockitoBean com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean RateLimitingFilter rateLimitingFilter;
    @MockitoBean com.beezhub.student_accommodation.security.MustChangePasswordFilter mustChangePasswordFilter;

    /** Pass-through filters so the URL rules run; see WaitlistControllerTest. */
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
    void getContactInfo_anonymous_returnsEmailAndPhone() throws Exception {
        when(contactInfoService.getContactInfo())
                .thenReturn(new ContactInfoResponse("help@example.co.za", "+27 11 555 0100"));

        mockMvc.perform(get("/api/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("help@example.co.za"))
                .andExpect(jsonPath("$.phone").value("+27 11 555 0100"));
    }

    @Test
    void getContactInfo_noPhoneConfigured_returnsNullPhone() throws Exception {
        when(contactInfoService.getContactInfo())
                .thenReturn(new ContactInfoResponse("help@example.co.za", null));

        mockMvc.perform(get("/api/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }
}
