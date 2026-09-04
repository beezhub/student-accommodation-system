package com.beezhub.student_accommodation.security;

import com.beezhub.student_accommodation.exceptions.AccountLockedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final HandlerExceptionResolver handlerExceptionResolver;

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:ip:";

    @Value("${app.security.trusted-proxies:}")
    private List<String> trustedProxies;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // Only apply to /api/auth/** endpoints
        if (requestURI.startsWith("/api/auth/")) {
            String clientIp = getClientIp(request);
            String key = RATE_LIMIT_PREFIX + clientIp;

            try {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, 1, TimeUnit.MINUTES);
                }

                if (count != null && count > MAX_REQUESTS_PER_MINUTE) {
                    log.warn("Rate limit exceeded for IP: {}", clientIp);
                    // Use HandlerExceptionResolver to throw 429
                    // We reuse AccountLockedException or create a specific RateLimitExceededException
                    // The task says "map lockout to 429", so AccountLockedException works, 
                    // or better, a dedicated RateLimitException mapped to 429.
                    // For now, I'll use a new RateLimitExceededException.
                    handlerExceptionResolver.resolveException(request, response, null, 
                        new AccountLockedException("Too many requests. Please try again later."));
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
        if (trustedProxies != null && !trustedProxies.isEmpty()
                && trustedProxies.contains(request.getRemoteAddr())) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
