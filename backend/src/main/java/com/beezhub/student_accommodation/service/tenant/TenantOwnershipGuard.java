package com.beezhub.student_accommodation.service.tenant;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.entity.TenantAware;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Confirms that an entity loaded by primary key belongs to the caller's tenant.
 *
 * <p>Hibernate's {@code @Filter} narrows queries but is not applied to load-by-key, so
 * {@code findById} happily returns another tenant's row. Identifiers are sequential, so a tenant
 * admin could reach records simply by changing a number in the URL. Enabling
 * {@code applyToLoadByKey} on the filter definition would close that globally, but several
 * tenant-aware tables legitimately hold platform-tenant rows — student {@code app_user} records,
 * platform system settings — that admins must still be able to read, so the check is explicit here.
 *
 * <p>A refusal is reported as "not found" rather than "forbidden", so probing ids cannot be used to
 * discover which records exist in other tenants.
 *
 * <p>Note what the platform-tenant short-circuit means for {@code PLATFORM_STAFF}: staff accounts
 * carry {@code tenant_id = 0}, so {@link #assertOwned} waves them through on every company's rows,
 * exactly as it does a platform admin. That is deliberate — GH-238 gives staff the whole shared
 * admin surface, writes included, and the governance endpoints they must not reach are held back
 * by {@code @PreAuthorize("hasRole('PLATFORM_ADMIN')")} rather than by tenancy. This guard is
 * about which company owns a row, never about what a role may do to it.
 */
@Service
@Slf4j
public class TenantOwnershipGuard {

    /**
     * Requires the entity to belong to the caller's tenant exactly. Use on admin write paths:
     * update, delete, status change, and anything else that mutates a tenant-owned record.
     */
    public <T extends TenantAware> T assertOwned(T entity, String entityLabel, Object id) {
        return check(entity, entityLabel, id, false);
    }

    /**
     * Like {@link #assertOwned} but also allows shared platform-tenant rows, which every tenant is
     * meant to see (seeded defaults, student-owned records). Use on read paths only.
     */
    public <T extends TenantAware> T assertReadable(T entity, String entityLabel, Object id) {
        return check(entity, entityLabel, id, true);
    }

    private <T extends TenantAware> T check(T entity, String entityLabel, Object id, boolean allowPlatformRows) {
        if (entity == null) {
            return null;
        }
        Long callerTenantId = TenantContext.getTenantId();
        // No tenant in context means a platform admin, a scheduled job or an async listener, none of
        // which are tenant-scoped.
        if (callerTenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(callerTenantId)) {
            return entity;
        }
        Long ownerTenantId = entity.getTenantId();
        if (callerTenantId.equals(ownerTenantId)) {
            return entity;
        }
        if (allowPlatformRows && TenantContext.PLATFORM_TENANT_ID.equals(ownerTenantId)) {
            return entity;
        }
        log.warn("action=crossTenantAccessDenied entity={} id={} ownerTenantId={} callerTenantId={}",
                entityLabel, id, ownerTenantId, callerTenantId);
        throw new ResourceNotFoundException(entityLabel + " not found with id: " + id);
    }
}
