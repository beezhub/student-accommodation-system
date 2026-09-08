package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.util.SecureTokenHasher;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that sets up the Mapped Diagnostic Context (MDC) for logging.
 * Adds requestId, userId, and tenantId to all log entries for the current
 * request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String MDC_KEY_REQUEST_ID = "requestId";
    /**
     * Named userRef, not userId. It used to be keyed "userId" while holding the authenticated
     * email address, so every log line of every request carried PII, against the house rule
     * (GH-154). It now holds a stable one-way pseudonym, which keeps log lines groupable by user
     * without the logs holding an email.
     */
    private static final String MDC_KEY_USER_REF = "userRef";
    private static final String MDC_KEY_TENANT_ID = "tenantId";
    private static final String HEADER_REQUEST_ID = "X-Request-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Request ID
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_KEY_REQUEST_ID, requestId);

            // Add to response header for client correlation
            response.setHeader(HEADER_REQUEST_ID, requestId);

            // 2. Tenant ID (if available from TenantContext - usually set by
            // JwtAuthenticationFilter)
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                MDC.put(MDC_KEY_TENANT_ID, String.valueOf(tenantId));
            }

            // 3. User ID (if available from SecurityContext)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                MDC.put(MDC_KEY_USER_REF, SecureTokenHasher.pseudonym(authentication.getName()));
            }

            filterChain.doFilter(request, response);
        } finally {
            // Ensure MDC is cleared to prevent data leaking to other requests on the same
            // thread
            MDC.clear();
        }
    }
}
