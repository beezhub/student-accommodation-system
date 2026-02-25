package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.service.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the tenant filter interceptor that enables the Hibernate {@code tenantFilter}
 * for every incoming HTTP request when a tenant ID is present in the {@link TenantContext}.
 *
 * <p>Works in conjunction with {@code @Filter} / {@code @FilterDef} annotations on
 * tenant-scoped entities and requires Spring Boot's Open Session in View (OSIV) — enabled
 * by default — so that the same Hibernate Session is shared across the request.
 *
 * <p>Scheduled jobs and {@code @Async} tasks intentionally run <em>without</em> the filter
 * because their thread-local {@link TenantContext} is empty; this means system-level jobs
 * can process data for all tenants.
 *
 * @see TenantContext
 */
@Slf4j
@Configuration
@ConditionalOnBean(EntityManagerFactory.class)
public class TenantFilterConfig implements WebMvcConfigurer, HandlerInterceptor {

    private static final String TENANT_FILTER_NAME = "tenantFilter";
    private static final String TENANT_ID_PARAM = "tenantId";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    /**
     * Enables the Hibernate {@code tenantFilter} before the request handler is invoked.
     *
     * <p>The filter is intentionally <em>skipped</em> in two cases:
     * <ol>
     *   <li>{@link TenantContext#getTenantId()} returns {@code null} — public endpoints and
     *       scheduled jobs that need a system-level (all-tenant) view.</li>
     *   <li>The tenant ID equals {@link TenantContext#PLATFORM_TENANT_ID} ({@code 0}) —
     *       student accounts.  Students are platform-level entities; their data access is
     *       scoped by {@code student_id} / {@code user_id} ownership rather than by the
     *       Hibernate tenant filter.</li>
     * </ol>
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter(TENANT_FILTER_NAME)
                    .setParameter(TENANT_ID_PARAM, tenantId);
            log.debug("Tenant filter enabled: tenantId={}, path={}", tenantId, request.getRequestURI());
        } else if (TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            log.debug("Tenant filter skipped for PLATFORM sentinel: path={}", request.getRequestURI());
        }
        return true;
    }
}
