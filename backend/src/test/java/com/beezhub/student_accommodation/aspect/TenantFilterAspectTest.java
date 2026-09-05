package com.beezhub.student_accommodation.aspect;

import com.beezhub.student_accommodation.config.TenantFilterConfig;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * GH-124: the filter is only effective when enabled on the transactional session. Outside a
 * transaction the shared EntityManager proxy hands back a throwaway session, so the call silently
 * did nothing and the caller read every tenant's rows.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantFilterAspectTest {

    private static final long TENANT = 7L;

    @Mock
    private EntityManager entityManager;
    @Mock
    private Session session;
    @Mock
    private Filter filter;

    private final TenantFilterAspect aspect = new TenantFilterAspect();

    private void givenEntityManager() {
        ReflectionTestUtils.setField(aspect, "entityManager", entityManager);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);
        when(filter.setParameter("tenantId", TENANT)).thenReturn(filter);
    }

    @AfterEach
    void reset() {
        TenantContext.clear();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void insideATransaction_filterIsEnabledForTheTenant() {
        givenEntityManager();
        TenantContext.setTenantId(TENANT);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        aspect.enableTenantFilter();

        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("tenantId", TENANT);
    }

    @Test
    void withoutATransaction_theThrowawaySessionIsNeverTouched() {
        ReflectionTestUtils.setField(aspect, "entityManager", entityManager);
        TenantContext.setTenantId(TENANT);
        TransactionSynchronizationManager.setActualTransactionActive(false);

        aspect.enableTenantFilter();

        // Enabling the filter here would look like it worked while filtering nothing.
        verifyNoInteractions(entityManager);
    }

    @Test
    void alreadyEnabledFilter_isNotReapplied() {
        ReflectionTestUtils.setField(aspect, "entityManager", entityManager);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.getEnabledFilter("tenantFilter")).thenReturn(filter);
        TenantContext.setTenantId(TENANT);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        aspect.enableTenantFilter();

        verify(session, never()).enableFilter("tenantFilter");
    }

    @Test
    void platformTenantAndNoTenant_areNotFiltered() {
        ReflectionTestUtils.setField(aspect, "entityManager", entityManager);
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        aspect.enableTenantFilter();

        TenantContext.clear();
        aspect.enableTenantFilter();

        verifyNoInteractions(entityManager);
    }

    /**
     * The two advices previously both defaulted to LOWEST_PRECEDENCE, leaving their order
     * undefined. This pins the relationship that makes the aspect run inside the transaction.
     */
    @Test
    void transactionAdviceIsOutermostAndTheAspectRunsInsideIt() {
        EnableTransactionManagement tx =
                TenantFilterConfig.class.getAnnotation(EnableTransactionManagement.class);
        Order aspectOrder = TenantFilterAspect.class.getAnnotation(Order.class);

        assertThat(tx).as("transaction management order must be pinned").isNotNull();
        assertThat(tx.order()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(aspectOrder).isNotNull();
        assertThat(aspectOrder.value())
                .as("the aspect must be less precedent than the transaction advisor, so it runs inside it")
                .isGreaterThan(tx.order());
    }
}
