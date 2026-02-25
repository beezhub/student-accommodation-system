package com.beezhub.student_accommodation.service.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context for storing the current tenant ID.
 * This is populated by the JwtAuthenticationFilter for each request.
 *
 * <p>The {@link #PLATFORM_TENANT_ID} sentinel value ({@code 0L}) identifies student
 * accounts.  Students are platform-level entities — they must be able to interact with
 * buildings owned by different tenants and therefore must NOT be scoped by the Hibernate
 * tenant filter.  {@link com.beezhub.student_accommodation.config.TenantFilterConfig}
 * skips enabling the filter whenever the stored tenant ID equals this sentinel.
 */
@Slf4j
public class TenantContext {

    /**
     * Sentinel tenant ID for student (platform-level) users.
     * When this value is present in the context the Hibernate {@code tenantFilter}
     * must NOT be enabled — student-owned data is scoped by {@code student_id} /
     * {@code user_id} ownership instead.
     */
    public static final Long PLATFORM_TENANT_ID = 0L;

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
