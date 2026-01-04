package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.config.JwtConfig;
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

    @BeforeEach
    void setUp() {
        var cfg = new JwtConfig(
                "01234567890123456789012345678901234567890123456789012345678901234",
                "3600000",
                "14400000"

        );
        jwtUtil = new JwtUtil(cfg);
        filter = new JwtAuthenticationFilter(jwtUtil);
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

        verify(chain).doFilter(req, res);
    }
}
