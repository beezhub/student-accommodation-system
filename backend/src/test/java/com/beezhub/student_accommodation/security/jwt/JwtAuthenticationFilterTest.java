package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.config.JwtConfig;
import com.beezhub.student_accommodation.security.FilterErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;
    private com.beezhub.student_accommodation.service.TokenBlacklistService tokenBlacklistService;
    private FilterErrorResponseWriter errorResponseWriter;

    @BeforeEach
    void setUp() {
        var cfg = new JwtConfig(
                "01234567890123456789012345678901234567890123456789012345678901234",
                "3600000",
                "14400000"

        );
        jwtUtil = new JwtUtil(cfg);
        tokenBlacklistService = mock(com.beezhub.student_accommodation.service.TokenBlacklistService.class);
        errorResponseWriter = new FilterErrorResponseWriter(
                new ObjectMapper().registerModule(new JavaTimeModule()));
        filter = new JwtAuthenticationFilter(jwtUtil, tokenBlacklistService, errorResponseWriter);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_populatesAuthoritiesFromJwt() throws ServletException, IOException {
        String token = jwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                "bob@example.com", "pw", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("bob@example.com", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        // Verify credentials hold the token
        assertEquals(token, auth.getCredentials());

        verify(chain).doFilter(req, res);
    }

    @Test
    void doFilterInternal_blacklistedToken_returnsUnauthorizedWithAnErrorResponseBody()
            throws ServletException, IOException {
        String token = jwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                "bob@example.com", "pw", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        ));
        String jti = jwtUtil.extractJti(token);
        
        when(tokenBlacklistService.isBlacklisted(jti)).thenReturn(true);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(req, res, chain);

        // Delegating to the HandlerExceptionResolver with a null handler resolved nothing, because
        // RestResponseExceptionHandler carries basePackages, so the request fell through to the
        // container error page. The filter writes the body itself now (GH-204).
        assertEquals(401, res.getStatus());
        assertTrue(res.getContentAsString().contains("Authentication failed"));
        assertTrue(res.getContentType().startsWith("application/json"));
        // Never the reason: "Token is blacklisted" tells an unauthenticated caller which check ran.
        assertFalse(res.getContentAsString().contains("blacklisted"));
        verify(chain, never()).doFilter(req, res);
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    }
}
