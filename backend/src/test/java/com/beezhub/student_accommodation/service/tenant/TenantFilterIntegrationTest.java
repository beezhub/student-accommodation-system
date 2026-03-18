package com.beezhub.student_accommodation.service.tenant;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.CustomApplicationField;
import com.beezhub.student_accommodation.model.entity.IntakePeriod;
import com.beezhub.student_accommodation.model.entity.RequiredDocumentType;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.CustomApplicationFieldRepository;
import com.beezhub.student_accommodation.repository.IntakePeriodRepository;
import com.beezhub.student_accommodation.repository.RequiredDocumentTypeRepository;
import com.beezhub.student_accommodation.AbstractJpaTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
 * <p>Extends {@link AbstractJpaTest} which starts a real PostgreSQL Testcontainer — resolving
 * the previous H2 incompatibility with {@code jsonb} columns. {@link AcademicYear} remains
 * the primary test entity because it implements {@link TenantAware} and carries the
 * {@code @Filter} annotation without complex FK dependencies.
 *
 * <p>Because the {@code tenant_id} column on tenant-scoped entities is mapped as a plain
 * {@code @Column} (no JPA {@code @ManyToOne}), raw {@code Long} values are used as tenant IDs.
 *
 * <p>Each test runs in a transaction that is rolled back after the method returns
 * (inherited from {@code @DataJpaTest} via {@link AbstractJpaTest}).
 */
class TenantFilterIntegrationTest extends AbstractJpaTest {

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

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private IntakePeriodRepository intakePeriodRepository;

    @Autowired
    private CustomApplicationFieldRepository customApplicationFieldRepository;

    @Autowired
    private RequiredDocumentTypeRepository requiredDocumentTypeRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUpTenants() {
        entityManager.createNativeQuery(
                "INSERT INTO tenant (id, name, config, is_active, created_at, updated_at) VALUES " +
                "(0,   'Platform', '{}', true, current_timestamp, current_timestamp), " +
                "(101, 'Tenant A', '{}', true, current_timestamp, current_timestamp), " +
                "(202, 'Tenant B', '{}', true, current_timestamp, current_timestamp)"
        ).executeUpdate();
        entityManager.flush();
    }

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
    // PLATFORM sentinel compliance
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("student_canSeeBuildings_fromTwoDifferentTenants")
    void student_canSeeBuildings_fromTwoDifferentTenants() {
        // Arrange: one academic year per tenant
        saveYearForTenant("2025 Owner-A", TENANT_1_ID);
        saveYearForTenant("2025 Owner-B", TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: student request — filter is NOT enabled (PLATFORM sentinel bypass).
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: student sees records from both tenants
        assertThat(results).hasSize(2);
        assertThat(results).extracting(AcademicYear::getTenantId)
                .containsExactlyInAnyOrder(TENANT_1_ID, TENANT_2_ID);
    }

    @Test
    @DisplayName("student_cannotSee_anotherStudentsDocuments")
    void student_cannotSee_anotherStudentsDocuments() {
        // Arrange: one record for a tenant, one for PLATFORM
        saveYearForTenant("2025 Owner-A Record", TENANT_1_ID);
        saveYearForTenant("2025 Student Record", TenantContext.PLATFORM_TENANT_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: tenant filter active — scoped to TENANT_1 only
        enableTenantFilter(TENANT_1_ID);
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: the PLATFORM-sentinel record (tenantId=0) is NOT visible inside
        // a tenant-scoped view; only the tenant's own record appears.
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_1_ID);
    }

    @Test
    @DisplayName("tenant_cannotSee_anotherTenantsBuildings")
    void tenant_cannotSee_anotherTenantsBuildings() {
        // Arrange: two records for tenant 1, two records for tenant 2
        saveYearForTenant("2025 OA Bldg 1", TENANT_1_ID);
        saveYearForTenant("2025 OA Bldg 2", TENANT_1_ID);
        saveYearForTenant("2025 OB Bldg 1", TENANT_2_ID);
        saveYearForTenant("2025 OB Bldg 2", TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: filter scoped to tenant 1
        enableTenantFilter(TENANT_1_ID);
        List<AcademicYear> results = academicYearRepository.findAll();

        // Assert: tenant 1 sees only their own records
        assertThat(results).hasSize(2);
        assertThat(results).extracting(AcademicYear::getTenantId)
                .containsOnly(TENANT_1_ID);
    }

    // ────────────────────────────────────────────────────────────
    // IntakePeriod — tenant isolation
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("intakePeriodRecord_notVisibleToCrossTenantQuery")
    void intakePeriodRecord_notVisibleToCrossTenantQuery() {
        // Arrange: two buildings (one per tenant) and one intake period each
        Building building1 = saveBuilding("Building A", TENANT_1_ID);
        Building building2 = saveBuilding("Building B", TENANT_2_ID);
        saveIntakePeriod("Spring 2025", building1, TENANT_1_ID);
        saveIntakePeriod("Autumn 2025", building2, TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: filter scoped to tenant 1
        enableTenantFilter(TENANT_1_ID);
        List<IntakePeriod> results = intakePeriodRepository.findAll();

        // Assert: only tenant 1's intake period is visible
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Spring 2025");
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_1_ID);
    }

    // ────────────────────────────────────────────────────────────
    // CustomApplicationField — tenant isolation
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("customApplicationFieldRecord_notVisibleToCrossTenantQuery")
    void customApplicationFieldRecord_notVisibleToCrossTenantQuery() {
        // Arrange: one custom field per tenant's building
        Building building1 = saveBuilding("Building A Fields", TENANT_1_ID);
        Building building2 = saveBuilding("Building B Fields", TENANT_2_ID);
        saveCustomApplicationField("ID Number", building1, TENANT_1_ID);
        saveCustomApplicationField("Proof of Income", building2, TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: filter scoped to tenant 2
        enableTenantFilter(TENANT_2_ID);
        List<CustomApplicationField> results = customApplicationFieldRepository.findAll();

        // Assert: only tenant 2's custom field is visible
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFieldLabel()).isEqualTo("Proof of Income");
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_2_ID);
    }

    // ────────────────────────────────────────────────────────────
    // RequiredDocumentType — tenant isolation
    // ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("requiredDocumentTypeRecord_notVisibleToCrossTenantQuery")
    void requiredDocumentTypeRecord_notVisibleToCrossTenantQuery() {
        // Arrange: one required document type per tenant's building
        Building building1 = saveBuilding("Building A Docs", TENANT_1_ID);
        Building building2 = saveBuilding("Building B Docs", TENANT_2_ID);
        saveRequiredDocumentType("Student ID", building1, TENANT_1_ID);
        saveRequiredDocumentType("Lease Agreement", building2, TENANT_2_ID);
        entityManager.flush();
        entityManager.clear();

        // Act: filter scoped to tenant 1
        enableTenantFilter(TENANT_1_ID);
        List<RequiredDocumentType> results = requiredDocumentTypeRepository.findAll();

        // Assert: only tenant 1's document type is visible
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocumentName()).isEqualTo("Student ID");
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_1_ID);
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

    /**
     * Persists a minimal {@link Building} row.
     */
    private Building saveBuilding(String name, Long tenantId) {
        Building building = new Building();
        building.setName(name);
        building.setTenantId(tenantId);
        return buildingRepository.save(building);
    }

    /**
     * Persists an {@link IntakePeriod} for the given building and tenant.
     */
    private IntakePeriod saveIntakePeriod(String name, Building building, Long tenantId) {
        IntakePeriod period = IntakePeriod.builder()
                .name(name)
                .building(building)
                .mode("ROLLING")
                .tenantId(tenantId)
                .build();
        return intakePeriodRepository.save(period);
    }

    /**
     * Persists a {@link CustomApplicationField} for the given building and tenant.
     */
    private CustomApplicationField saveCustomApplicationField(String label, Building building, Long tenantId) {
        CustomApplicationField field = CustomApplicationField.builder()
                .fieldLabel(label)
                .fieldType("TEXT")
                .building(building)
                .tenantId(tenantId)
                .build();
        return customApplicationFieldRepository.save(field);
    }

    /**
     * Persists a {@link RequiredDocumentType} for the given building and tenant.
     */
    private RequiredDocumentType saveRequiredDocumentType(String documentName, Building building, Long tenantId) {
        RequiredDocumentType docType = RequiredDocumentType.builder()
                .documentName(documentName)
                .building(building)
                .tenantId(tenantId)
                .build();
        return requiredDocumentTypeRepository.save(docType);
    }
}
