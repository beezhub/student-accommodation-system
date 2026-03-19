package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.service.tenant.TenantContext;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

/**
 * Tenant filter configuration.
 *
 * <p><strong>Note:</strong> The previous interceptor-based approach of enabling the Hibernate
 * {@code tenantFilter} in {@code HandlerInterceptor.preHandle()} was broken when OSIV is
 * disabled ({@code spring.jpa.open-in-view=false}).  Without OSIV the interceptor's
 * {@code EntityManager} is a short-lived, throwaway instance created outside any transaction,
 * so enabling the filter there has no effect on the sessions opened later by Spring Data JPA
 * repository calls.
 *
 * <p>Filter enabling is now handled by
 * {@link com.beezhub.student_accommodation.aspect.TenantFilterAspect}, an AOP {@code @Before}
 * advice that runs <em>inside</em> every {@code @Transactional} method, where the
 * thread-bound {@code EntityManager} already points to the active transactional session.
 *
 * @see TenantContext
 * @see com.beezhub.student_accommodation.aspect.TenantFilterAspect
 */
@Configuration
@ConditionalOnBean(EntityManagerFactory.class)
public class TenantFilterConfig {
    // No interceptor registration needed — see TenantFilterAspect.
}
