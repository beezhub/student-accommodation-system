package com.beezhub.student_accommodation.config;

import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Guarantees the Hibernate {@code tenantFilter} is enabled inside the transaction, not beside it.
 *
 * <p>An earlier interceptor-based approach was removed because it enabled the filter in
 * {@code HandlerInterceptor.preHandle()}, and with {@code spring.jpa.open-in-view=false} the
 * {@code EntityManager} there is a throwaway instance created outside any transaction, so the
 * filter had no effect on the sessions opened later by repository calls. That left this class
 * empty, and {@link com.beezhub.student_accommodation.aspect.TenantFilterAspect} as the only
 * enabler.
 *
 * <p>The aspect has the same problem unless it runs <em>inside</em> the transaction. Both it and
 * Spring's transaction advisor default to {@code LOWEST_PRECEDENCE}, so their relative order was
 * undefined: whenever the aspect won, it unwrapped a session that was not the transactional one and
 * the filter was silently ineffective (GH-124). Pinning the transaction advisor to the highest
 * precedence makes it the outermost advice, so the aspect always runs with the transactional
 * session bound to the thread.
 *
 * @see TenantContext
 * @see com.beezhub.student_accommodation.aspect.TenantFilterAspect
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class TenantFilterConfig {
    // The annotation is the configuration; see the class javadoc for why the order matters.
}
