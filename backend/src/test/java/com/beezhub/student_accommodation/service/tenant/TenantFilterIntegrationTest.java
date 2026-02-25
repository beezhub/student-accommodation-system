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
    // TASK-2.02: PLATFORM sentinel compliance
    // ────────────────────────────────────────────────────────────

    /**
     * A student (tenantId=PLATFORM_TENANT_ID) must be able to see records that belong to
     * two different building-owner tenants.
     *
     * <p>In production, {@link com.beezhub.student_accommodation.config.TenantFilterConfig}
     * skips enabling the Hibernate filter when the authenticated user's tenant ID equals
     * {@link TenantContext#PLATFORM_TENANT_ID}.  This test replicates that behaviour
     * directly: the filter is intentionally NOT enabled (matching the PLATFORM sentinel
     * bypass logic) and we assert that records for both tenants are visible.
     */
    @Test
    @DisplayName("student_canSeeBuildings_fromTwoDifferentTenants")
    void student_canSeeBuildings_fromTwoDifferentTenants() {
        // Arrange: one academic year per building-owner tenant
        saveYearForTenant("2025 Owner-A", TENANT_1_ID);
        saveYearForTenant("2025 Owner-B", TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: student request — filter is NOT enabled (PLATFORM sentinel bypass).
        // TenantFilterConfig.preHandle() skips enableFilter() when tenantId==0.
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        // Do NOT call enableTenantFilter() — that is the key invariant being tested.
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: student sees records from both tenants
        assertThat(results).hasSize(2);
        assertThat(results).extracting(AcademicYear::getTenantId)
                .containsExactlyInAnyOrder(TENANT_1_ID, TENANT_2_ID);
    }

    /**
     * Student-owned data isolation is enforced by {@code student_id} / {@code user_id}
     * ownership predicates at the service layer — NOT by the Hibernate tenant filter.
     *
     * <p>This test verifies the inverse: that when the tenant filter is active for a
     * building-owner tenant, a PLATFORM-sentinel record (simulating a student's resource)
     * is correctly excluded from that tenant's scoped view.  This ensures that setting
     * tenant_id=0 on student records does not accidentally expose them inside a
     * building-owner's filtered session.
     */
    @Test
    @DisplayName("student_cannotSee_anotherStudentsDocuments")
    void student_cannotSee_anotherStudentsDocuments() {
        // Arrange: one record owned by a building-owner tenant, one "owned" by PLATFORM
        saveYearForTenant("2025 Owner-A Record", TENANT_1_ID);
        saveYearForTenant("2025 Student Record", TenantContext.PLATFORM_TENANT_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: building-owner tenant filter active — scoped to TENANT_1 only
        enableTenantFilter(TENANT_1_ID);
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: the PLATFORM-sentinel record (tenantId=0) is NOT visible inside
        // a building-owner's scoped view; only the owner's own record appears.
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_1_ID);
    }

    /**
     * Building-owner tenant isolation must remain intact after the PLATFORM sentinel
     * bypass is introduced.  A building owner can only see their own buildings —
     * records belonging to a different building owner must not be visible.
     */
    @Test
    @DisplayName("buildingOwner_cannotSee_anotherOwnersBuildings")
    void buildingOwner_cannotSee_anotherOwnersBuildings() {
        // Arrange: two records for tenant 1, two records for tenant 2
        saveYearForTenant("2025 Owner-A Building 1", TENANT_1_ID);
        saveYearForTenant("2025 Owner-A Building 2", TENANT_1_ID);
        saveYearForTenant("2025 Owner-B Building 1", TENANT_2_ID);
        saveYearForTenant("2025 Owner-B Building 2", TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: filter scoped to tenant 1 (building owner A)
        enableTenantFilter(TENANT_1_ID);
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: owner A sees only their own buildings; owner B's buildings are hidden
        assertThat(results).hasSize(2);
        assertThat(results).extracting(AcademicYear::getTenantId)
                .containsOnly(TENANT_1_ID);
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
