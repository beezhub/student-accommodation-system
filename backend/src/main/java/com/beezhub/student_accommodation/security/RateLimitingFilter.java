package com.beezhub.student_accommodation.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final FilterErrorResponseWriter errorResponseWriter;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:ip:";

    /**
     * Only the endpoints a caller can reach without a session are limited. The filter used to cover
     * every {@code /api/auth/**} path, so a signed-in user polling {@code /me} or refreshing a token
     * spent the same allowance as someone guessing passwords (GH-119).
     */
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-reset-token",
            "/api/auth/verify-email",
            "/api/auth/resend-verification");

    @Value("${app.security.trusted-proxies:}")
    private List<String> trustedProxies;

    @Value("${app.security.rate-limit.requests-per-minute:30}")
    private int maxRequestsPerMinute;

    /**
     * Built once from {@link #trustedProxies}. {@link IpAddressMatcher} accepts a plain address as
     * an exact match and a {@code /}-suffixed value as a CIDR range, so a single matcher type covers
     * both forms {@code .env.example} documents (GH-252).
     */
    private List<IpAddressMatcher> trustedProxyMatchers = List.of();

    @PostConstruct
    void warnIfProxiesAreNotConfigured() {
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            log.warn("action=rateLimitProxyUnconfigured app.security.trusted-proxies is empty. "
                    + "Behind a reverse proxy every client shares the proxy's address and therefore one "
                    + "rate-limit bucket. Set APP_SECURITY_TRUSTED_PROXIES to the proxy addresses.");
            return;
        }

        List<IpAddressMatcher> matchers = new ArrayList<>();
        for (String trustedProxy : trustedProxies) {
            try {
                matchers.add(new IpAddressMatcher(trustedProxy));
            } catch (IllegalArgumentException e) {
                log.warn("action=rateLimitProxyInvalid value={} is not a valid address or CIDR range "
                        + "and will never be trusted. Check APP_SECURITY_TRUSTED_PROXIES.", trustedProxy);
            }
        }
        trustedProxyMatchers = matchers;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // Only the unauthenticated auth endpoints; see RATE_LIMITED_PATHS.
        if (RATE_LIMITED_PATHS.contains(requestURI)) {
            String clientIp = getClientIp(request);
            String key = RATE_LIMIT_PREFIX + clientIp;

            try {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, 1, TimeUnit.MINUTES);
                }

                if (count != null && count > maxRequestsPerMinute) {
                    log.warn("Rate limit exceeded for IP: {}", clientIp);
                    // This handed an AccountLockedException to the shared HandlerExceptionResolver
                    // with a null handler. RestResponseExceptionHandler carries basePackages, so no
                    // advice was applicable, resolveException returned null and the filter returned
                    // having written nothing at all: a rate-limited caller got 200 with an empty
                    // body rather than a 429 (GH-204).
                    errorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                            "Too many requests. Please try again later.");
                    return;
                }
            } catch (Exception e) {
                log.warn("Redis unavailable, skipping rate limit check", e);
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        // Only trust X-Forwarded-For when the request comes from a known reverse proxy
        if (isTrustedProxy(request.getRemoteAddr())) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // Leftmost value only. That is client-controllable unless the trusted proxy
                // replaces the header rather than appending to it, which the code here cannot
                // enforce; it depends on the proxy being configured to overwrite X-Forwarded-For.
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isTrustedProxy(String remoteAddr) {
        for (IpAddressMatcher matcher : trustedProxyMatchers) {
            if (matcher.matches(remoteAddr)) {
                return true;
            }
        }
        return false;
    }
}
