package com.beezhub.student_accommodation.security.jwt;

import com.beezhub.student_accommodation.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GH-123: the JWT was accepted from a {@code token} query parameter on every endpoint, so tokens
 * leaked into proxy access logs and browser history. Only the SSE notification stream, which cannot
 * set an Authorization header, may carry it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterQueryTokenTest {

    private static final String TOKEN = "header.payload.signature";
    private static final String USER = "admin@example.com";

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void stubValidToken() {
        SecurityContextHolder.clearContext();
        when(jwtUtil.extractJti(TOKEN)).thenReturn("jti-1");
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(USER);
        when(jwtUtil.validateToken(TOKEN, USER)).thenReturn(true);
        when(jwtUtil.extractRoles(TOKEN)).thenReturn(List.of("ROLE_PLATFORM_ADMIN"));
        when(jwtUtil.extractTenantId(TOKEN)).thenReturn(0L);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithQueryToken(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        request.setParameter("token", TOKEN);
        return request;
    }

    @Test
    void queryToken_onSseStream_authenticates() throws Exception {
        filter.doFilter(requestWithQueryToken(JwtAuthenticationFilter.SSE_STREAM_PATH),
                new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(USER);
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void queryToken_onOrdinaryEndpoint_isIgnored() throws Exception {
        filter.doFilter(requestWithQueryToken("/api/admin/users"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(anyString(), anyString());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void queryToken_onPathThatMerelyStartsWithTheStreamPath_isIgnored() throws Exception {
        filter.doFilter(requestWithQueryToken(JwtAuthenticationFilter.SSE_STREAM_PATH + "/../admin"),
                new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authorizationHeader_stillWorksEverywhere() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.setRequestURI("/api/admin/users");
        request.addHeader("Authorization", "Bearer " + TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void queryToken_underServletContextPath_stillMatchesTheStream() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/app" + JwtAuthenticationFilter.SSE_STREAM_PATH);
        request.setRequestURI("/app" + JwtAuthenticationFilter.SSE_STREAM_PATH);
        request.setContextPath("/app");
        request.setParameter("token", TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
