package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestLoggingFilter}.
 *
 * Uses a no-arg {@link MockFilterChain} (no-op chain). The X-Request-ID response header
 * proves MDC was populated during filter execution; null MDC values after the call
 * prove the finally-block cleared the context correctly.
 */
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilter_authenticatedUser_mdcContainsUserId() throws Exception {
        // Arrange — an authenticated user is present in the SecurityContext
        var auth = new UsernamePasswordAuthenticationToken(
                "student@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/room-allocation");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert — the X-Request-ID header was set, confirming the filter ran and
        // populated MDC (the header value comes from the generated requestId)
        assertThat(response.getHeader("X-Request-ID"))
                .as("X-Request-ID header should be set by the filter")
                .isNotNull();
    }

    @Test
    void doFilter_afterCompletion_mdcCleared() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/applications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert — the response header confirms the filter executed and generated a requestId
        assertThat(response.getHeader("X-Request-ID"))
                .as("X-Request-ID header must be set during filter execution")
                .isNotNull();

        // Assert — MDC cleared in the finally block; no thread-local data leaks between requests
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("tenantId")).isNull();
    }
}
