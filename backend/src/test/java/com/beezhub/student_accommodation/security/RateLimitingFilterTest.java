package com.beezhub.student_accommodation.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GH-119: the limiter keyed on the proxy's address when trusted proxies were unset, so one bucket
 * throttled the whole deployment, and it covered authenticated paths, so a signed-in user spent the
 * same allowance as someone guessing passwords.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitingFilterTest {

    private static final String PROXY_IP = "10.0.0.1";
    private static final String CLIENT_IP = "203.0.113.7";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        ReflectionTestUtils.setField(filter, "maxRequestsPerMinute", 30);
        ReflectionTestUtils.setField(filter, "trustedProxies", List.of());
    }

    private MockHttpServletRequest request(String path, String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/login", "/api/auth/signup", "/api/auth/forgot-password",
            "/api/auth/reset-password", "/api/auth/verify-email"})
    void unauthenticatedAuthPaths_areLimited(String path) throws Exception {
        filter.doFilter(request(path, CLIENT_IP, null), new MockHttpServletResponse(), chain);

        verify(valueOperations).increment("rate_limit:ip:" + CLIENT_IP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", "/api/auth/refresh-token", "/api/auth/logout",
            "/api/auth/change-password"})
    void authenticatedAuthPaths_areNotLimited(String path) throws Exception {
        // A signed-in user polling /me should not spend the sign-in allowance.
        filter.doFilter(request(path, CLIENT_IP, null), new MockHttpServletResponse(), chain);

        verify(valueOperations, never()).increment(anyString());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void trustedProxy_keysOnTheForwardedClientAddress() throws Exception {
        ReflectionTestUtils.setField(filter, "trustedProxies", List.of(PROXY_IP));

        filter.doFilter(request("/api/auth/login", PROXY_IP, CLIENT_IP + ", 10.0.0.5"),
                new MockHttpServletResponse(), chain);

        verify(valueOperations).increment("rate_limit:ip:" + CLIENT_IP);
    }

    @Test
    void untrustedSource_forwardedHeaderIsIgnored() throws Exception {
        // Otherwise anyone could set the header and get a fresh bucket per request.
        filter.doFilter(request("/api/auth/login", "198.51.100.9", "1.2.3.4"),
                new MockHttpServletResponse(), chain);

        verify(valueOperations).increment("rate_limit:ip:198.51.100.9");
    }

    @Test
    void overTheCeiling_theRequestIsRejected() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(31L);

        filter.doFilter(request("/api/auth/login", CLIENT_IP, null), new MockHttpServletResponse(), chain);

        verify(handlerExceptionResolver).resolveException(any(), any(), any(), any());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void atTheCeiling_theRequestStillPasses() throws Exception {
        when(valueOperations.increment(anyString())).thenReturn(30L);

        filter.doFilter(request("/api/auth/login", CLIENT_IP, null), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void redisUnavailable_requestIsAllowedThrough() throws Exception {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        filter.doFilter(request("/api/auth/login", CLIENT_IP, null), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }
}
