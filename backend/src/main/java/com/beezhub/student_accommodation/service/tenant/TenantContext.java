package com.beezhub.student_accommodation.service.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context for storing the current tenant ID.
 * This is populated by the JwtAuthenticationFilter for each request.
 */
@Slf4j
public class TenantContext {

    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        log.trace("Setting tenant ID to {}", tenantId);
        currentTenant.set(tenantId);
    }

    public static Long getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        log.trace("Clearing tenant ID");
        currentTenant.remove();
    }
}
