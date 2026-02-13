package com.beezhub.student_accommodation.aspect;

import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP Aspect that automatically logs activities for methods annotated with @LogActivity
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLogAspect {

    private final ActivityLogService activityLogService;
    private final AppUserRepository appUserRepository;

    /**
     * Intercepts methods annotated with @LogActivity and logs the activity
     */
    @AfterReturning(value = "@annotation(logActivity)", returning = "result")
    public void logActivity(JoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                log.debug("No authenticated user found, skipping activity log");
                return;
            }

            String userEmail = authentication.getName();

            // Get user ID
            Long userId = appUserRepository.findByEmail(userEmail)
                    .map(user -> user.getId())
                    .orElse(null);

            if (userId == null) {
                log.warn("User not found for email: {}, skipping activity log", userEmail);
                return;
            }

            // Extract entity ID from method parameters
            Long entityId = extractEntityId(joinPoint, logActivity.entityIdParamIndex());

            // Build description
            String description = buildDescription(logActivity.description(), joinPoint.getArgs());

            // Get IP address from request
            String ipAddress = getIpAddress();

            // Build metadata
            Map<String, Object> metadata = buildMetadata(joinPoint, result);

            // Log the activity
            activityLogService.logActivity(
                    userId,
                    logActivity.action(),
                    logActivity.entityType(),
                    entityId,
                    description,
                    ipAddress,
                    metadata
            );

            log.debug("Activity logged: {} - {} on {} {}", userEmail, logActivity.action(),
                    logActivity.entityType(), entityId);

        } catch (Exception e) {
            // Don't let logging errors break the application
            log.error("Failed to log activity", e);
        }
    }

    /**
     * Extract entity ID from method parameters
     */
    private Long extractEntityId(JoinPoint joinPoint, int paramIndex) {
        if (paramIndex < 0) {
            return null;
        }

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length <= paramIndex) {
            return null;
        }

        Object param = args[paramIndex];
        if (param instanceof Long) {
            return (Long) param;
        } else if (param instanceof Integer) {
            return ((Integer) param).longValue();
        } else if (param instanceof String) {
            try {
                return Long.parseLong((String) param);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Build description from template and method parameters
     */
    private String buildDescription(String template, Object[] args) {
        if (template == null || template.isEmpty()) {
            return "Action performed";
        }

        try {
            // Use MessageFormat to replace placeholders like {0}, {1}, etc.
            return MessageFormat.format(template, args);
        } catch (Exception e) {
            log.warn("Failed to format description template: {}", template, e);
            return template;
        }
    }

    /**
     * Get IP address from current HTTP request
     */
    private String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return extractIpFromRequest(request);
            }
        } catch (Exception e) {
            log.debug("Failed to get IP address from request", e);
        }
        return "unknown";
    }

    /**
     * Extract IP address from HTTP request headers
     */
    private String extractIpFromRequest(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs (take the first one)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Build metadata from method signature and result
     */
    private Map<String, Object> buildMetadata(JoinPoint joinPoint, Object result) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Add method name
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            metadata.put("method", signature.getMethod().getName());
            metadata.put("class", signature.getDeclaringType().getSimpleName());

            // Add parameter count (don't log actual values for security)
            metadata.put("paramCount", joinPoint.getArgs() != null ? joinPoint.getArgs().length : 0);

            // Add result type (don't log actual value for security)
            if (result != null) {
                metadata.put("resultType", result.getClass().getSimpleName());
            }

        } catch (Exception e) {
            log.debug("Failed to build metadata", e);
        }

        return metadata.isEmpty() ? null : metadata;
    }
}
