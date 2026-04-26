package com.beezhub.student_accommodation.aspect;

import com.beezhub.student_accommodation.service.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enables the Hibernate {@code tenantFilter} at the start of every
 * {@code @Transactional} method execution.
 *
 * <p>This replaces the {@link com.beezhub.student_accommodation.config.TenantFilterConfig}
 * interceptor-based approach, which does not work when OSIV is disabled
 * ({@code spring.jpa.open-in-view=false}).  Without OSIV the interceptor's
 * {@code EntityManager} is a short-lived, throwaway instance — not the same session
 * used by Spring Data JPA repository calls inside service methods.
 *
 * <p>By running at {@link Ordered#LOWEST_PRECEDENCE} this advice executes <em>inside</em>
 * Spring's {@code @Transactional} proxy, where the thread-bound {@code EntityManager}
 * already points to the active transactional session.  Enabling the filter here therefore
 * applies it to every query executed within that transaction.
 *
 * <p>The filter is intentionally skipped when:
 * <ul>
 *   <li>{@link TenantContext#getTenantId()} is {@code null} — public / unauthenticated requests.</li>
 *   <li>The tenant ID equals {@link TenantContext#PLATFORM_TENANT_ID} ({@code 0}) — student
 *       (platform-level) users whose data access is scoped by ownership, not by tenant.</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantFilterAspect {

    private static final String TENANT_FILTER_NAME = "tenantFilter";
    private static final String TENANT_ID_PARAM    = "tenantId";

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional) || " +
            "@within(org.springframework.transaction.annotation.Transactional)  || " +
            "@annotation(jakarta.transaction.Transactional)                     || " +
            "@within(jakarta.transaction.Transactional)                         || " +
            "execution(* com.beezhub.student_accommodation.service..*(..))      || " +
            "execution(* com.beezhub.student_accommodation.controller..*(..))")
    public void enableTenantFilter() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            return;
        }

        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter(TENANT_FILTER_NAME) == null) {
                session.enableFilter(TENANT_FILTER_NAME).setParameter(TENANT_ID_PARAM, tenantId);
                log.debug("Tenant filter enabled via AOP: tenantId={}", tenantId);
            }
        } catch (Exception e) {
            log.warn("Could not enable tenant filter: {}", e.getMessage());
        }
    }
}
