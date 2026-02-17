package com.beezhub.student_accommodation.service.tenant;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that the Hibernate {@code tenantFilter} correctly isolates
 * data between tenants when explicitly enabled on the current Hibernate {@link Session}.
 *
 * <p>In production the filter is enabled by {@link com.beezhub.student_accommodation.config.TenantFilterConfig}
 * (a Spring MVC HandlerInterceptor). These tests activate the filter directly via the
 * {@link Session} API to replicate that behaviour without the full Spring MVC stack.
 *
 * <p>Uses {@link DataJpaTest} with an H2 in-memory database (Flyway disabled, schema from JPA
 * {@code create-drop}). {@link AcademicYear} is chosen as the test entity because it has no
 * {@code jsonb} columns — which H2 cannot reliably create — while still implementing
 * {@link TenantAware} and carrying the {@code @Filter} annotation.
 *
 * <p>Because the {@code tenant_id} column on tenant-scoped entities is mapped as a plain
 * {@code @Column} (no JPA {@code @ManyToOne}), there is no foreign-key constraint in the H2
 * schema and raw {@code Long} values are used as tenant IDs.
 *
 * <p>Each test runs in a transaction that is rolled back after the method returns.
 */
@DataJpaTest
@ActiveProfiles("test")
class TenantFilterIntegrationTest {

    private static final String TENANT_FILTER_NAME = "tenantFilter";
    private static final String TENANT_ID_PARAM = "tenantId";

    /** Arbitrary stable tenant identifiers — no rows in {@code tenant} table required. */
    private static final Long TENANT_1_ID = 101L;
    private static final Long TENANT_2_ID = 202L;

    /** Counter to generate unique AcademicYear names across tests within the same session. */
    private static final AtomicInteger NAME_SEQ = new AtomicInteger(0);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @AfterEach
    void tearDown() {
        disableTenantFilter();
        TenantContext.clear();
    }

    // ────────────────────────────────────────────────────────────
    // Filter — query isolation
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listBuildings_tenant1_onlyTenant1BuildingsReturned")
    void listBuildings_tenant1_onlyTenant1BuildingsReturned() {
        // Arrange: one academic year per tenant
        saveYearForTenant("2025 Alpha", TENANT_1_ID);
        saveYearForTenant("2025 Beta", TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: enable filter scoped to tenant 1
        enableTenantFilter(TENANT_1_ID);
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: only tenant 1's record is visible
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("2025 Alpha");
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_1_ID);
    }

    @Test
    @DisplayName("listBuildings_tenant2_onlyTenant2BuildingsReturned")
    void listBuildings_tenant2_onlyTenant2BuildingsReturned() {
        // Arrange: two records for tenant 1, one for tenant 2
        saveYearForTenant("2025 Alpha A", TENANT_1_ID);
        saveYearForTenant("2025 Alpha B", TENANT_1_ID);
        saveYearForTenant("2025 Beta", TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: enable filter scoped to tenant 2
        enableTenantFilter(TENANT_2_ID);
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: only tenant 2's record is visible
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("2025 Beta");
    }

    @Test
    @DisplayName("listBuildings_noFilterEnabled_allBuildingsReturned")
    void listBuildings_noFilterEnabled_allBuildingsReturned() {
        // Arrange: records for two tenants
        saveYearForTenant("2025 Alpha", TENANT_1_ID);
        saveYearForTenant("2025 Beta", TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: no filter enabled — system-level view sees everything
        List<AcademicYear> results = academicYearRepository.findAll();

        assertThat(results).hasSize(2);
    }

    // ────────────────────────────────────────────────────────────
    // TenantListener — automatic tenantId assignment on @PrePersist
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createBuilding_tenantIdAutoSet_byTenantListener")
    void createBuilding_tenantIdAutoSet_byTenantListener() {
        // Arrange: set TenantContext so TenantListener injects tenantId on @PrePersist
        TenantContext.setTenantId(TENANT_1_ID);

        AcademicYear year = buildYear("2026 Listener Test");
        // tenantId intentionally NOT set — TenantListener should populate it

        AcademicYear saved = academicYearRepository.save(year);
        entityManager.flush();
        entityManager.clear();

        // Assert: TenantListener copied the ThreadLocal value onto the entity
        AcademicYear reloaded = academicYearRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTenantId()).isEqualTo(TENANT_1_ID);
    }

    @Test
    @DisplayName("createBuilding_tenantIdAlreadySet_tenantListenerDoesNotOverwrite")
    void createBuilding_tenantIdAlreadySet_tenantListenerDoesNotOverwrite() {
        // Arrange: TenantContext has tenant 1, but entity is explicitly assigned to tenant 2
        TenantContext.setTenantId(TENANT_1_ID);

        AcademicYear year = buildYear("2026 Explicit Tenant");
        year.setTenantId(TENANT_2_ID); // explicit override takes precedence

        AcademicYear saved = academicYearRepository.save(year);
        entityManager.flush();
        entityManager.clear();

        // Assert: TenantListener did NOT overwrite the explicitly assigned tenant
        AcademicYear reloaded = academicYearRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTenantId()).isEqualTo(TENANT_2_ID);
    }

    // ────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────

    private AcademicYear saveYearForTenant(String name, Long tenantId) {
        AcademicYear year = buildYear(name);
        year.setTenantId(tenantId);
        return academicYearRepository.save(year);
    }

    /**
     * Builds a valid {@link AcademicYear} instance satisfying all {@code nullable = false} constraints.
     */
    private AcademicYear buildYear(String name) {
        LocalDate base = LocalDate.of(2025, 1, 1);
        return AcademicYear.builder()
                .name(name)
                .startDate(base)
                .endDate(base.plusMonths(11))
                .applicationOpenDate(base.minusMonths(3))
                .applicationCloseDate(base.minusMonths(1))
                .build();
    }

    private void enableTenantFilter(Long tenantId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter(TENANT_FILTER_NAME).setParameter(TENANT_ID_PARAM, tenantId);
    }

    private void disableTenantFilter() {
        try {
            Session session = entityManager.unwrap(Session.class);
            session.disableFilter(TENANT_FILTER_NAME);
        } catch (Exception ignored) {
            // Session may already be closed during teardown — safe to ignore
        }
    }
}
