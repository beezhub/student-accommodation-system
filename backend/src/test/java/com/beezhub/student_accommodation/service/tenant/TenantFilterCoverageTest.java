package com.beezhub.student_accommodation.service.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractJpaTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.SystemSetting;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.SystemSettingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * GH-160: {@code TenantFilterIntegrationTest} covered four entities — AcademicYear, Building,
 * IntakePeriod and RequiredDocumentType — and left the ones that carry the
 * money and the people uncovered.
 *
 * <p>This covers Room, Application, RoomAllocation and SystemSetting, and pins down the one thing
 * about the filter that is easiest to get wrong: it does not apply to {@code findById}.
 */
class TenantFilterCoverageTest extends AbstractJpaTest {

    private static final Long TENANT_A = 101L;
    private static final Long TENANT_B = 202L;

    @PersistenceContext private EntityManager entityManager;

    @Autowired private BuildingRepository buildingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private RoomAllocationRepository roomAllocationRepository;
    @Autowired private SystemSettingRepository systemSettingRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;

    private int seq;

    @BeforeEach
    void seedTenants() {
        entityManager
                .createNativeQuery(
                        "INSERT INTO tenant (id, name, config, is_active, created_at, updated_at) VALUES "
                                + "(0,   'Platform', '{}', true, current_timestamp, current_timestamp), "
                                + "(101, 'Tenant A', '{}', true, current_timestamp, current_timestamp), "
                                + "(202, 'Tenant B', '{}', true, current_timestamp, current_timestamp)")
                .executeUpdate();
        entityManager.flush();
    }

    @AfterEach
    void clearFilter() {
        entityManager.unwrap(Session.class).disableFilter("tenantFilter");
        TenantContext.clear();
    }

    // ── Query isolation for the entities that were uncovered ────────────────────────────────

    @Test
    void rooms_areScopedToTheTenantInContext() {
        room(TENANT_A, "A-1");
        room(TENANT_B, "B-1");

        enableFilterFor(TENANT_A);

        assertThat(roomRepository.findAll()).extracting(Room::getRoomNumber).containsExactly("A-1");
    }

    @Test
    void applications_areScopedToTheTenantInContext() {
        application(TENANT_A, student());
        application(TENANT_B, student());

        enableFilterFor(TENANT_A);

        assertThat(applicationRepository.findAll())
                .extracting(Application::getTenantId)
                .containsExactly(TENANT_A);
    }

    @Test
    void roomAllocations_areScopedToTheTenantInContext() {
        allocation(TENANT_A);
        allocation(TENANT_B);

        enableFilterFor(TENANT_B);

        assertThat(roomAllocationRepository.findAll())
                .extracting(RoomAllocation::getTenantId)
                .containsExactly(TENANT_B);
    }

    @Test
    void systemSettings_areScopedToTheTenantInContext() {
        setting(TENANT_A, "offer_expiry_days", "7");
        setting(TENANT_B, "offer_expiry_days", "14");

        enableFilterFor(TENANT_A);

        assertThat(systemSettingRepository.findAll())
                .extracting(SystemSetting::getSettingValue)
                .containsExactly("7");
    }

    /**
     * Platform rows are not exempt from the filter. That is why the settings cascade is a lookup
     * per tenant then a fallback to tenant 0, rather than one unfiltered query.
     */
    @Test
    void aPlatformSettingIsNotVisibleToATenantScopedQuery() {
        setting(0L, "offer_expiry_days", "7");

        enableFilterFor(TENANT_A);

        assertThat(systemSettingRepository.findAll()).isEmpty();
    }

    // ── The findById trap ───────────────────────────────────────────────────────────────────

    /**
     * The filter does <em>not</em> apply to {@code findById}. Hibernate's
     * {@code @Filter} covers queries, not load-by-key, and {@code applyToLoadByKey} is
     * deliberately off — platform rows in tenant-aware tables have to stay readable.
     *
     * <p>So every admin path that loads a tenant-owned row by id needs
     * {@code TenantOwnershipGuard.assertOwned} or {@code assertReadable}. These assert the gap
     * rather than a fix, because the gap is the design: a test that assumed findById was filtered
     * would be asserting a protection that does not exist.
     */
    @Test
    void findById_returnsAnotherTenantsRoom_whichIsWhyTheOwnershipGuardExists() {
        Room otherTenantsRoom = room(TENANT_B, "B-1");
        entityManager.flush();
        entityManager.clear();

        enableFilterFor(TENANT_A);

        assertThat(roomRepository.findById(otherTenantsRoom.getId())).isPresent();
    }

    @Test
    void findById_returnsAnotherTenantsApplication_whichIsWhyTheOwnershipGuardExists() {
        Application otherTenants = application(TENANT_B, student());
        entityManager.flush();
        entityManager.clear();

        enableFilterFor(TENANT_A);

        assertThat(applicationRepository.findById(otherTenants.getId())).isPresent();
    }

    /** The same query, expressed as a query rather than a load-by-key, is filtered. */
    @Test
    void theSameRowIsHiddenWhenItIsFetchedByAQueryRatherThanById() {
        Room otherTenantsRoom = room(TENANT_B, "B-1");
        entityManager.flush();
        entityManager.clear();

        enableFilterFor(TENANT_A);

        assertThat(roomRepository.findById(otherTenantsRoom.getId())).isPresent();
        assertThat(roomRepository.findAll()).isEmpty();
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────────────────

    private void enableFilterFor(Long tenantId) {
        entityManager.flush();
        entityManager.clear();
        entityManager
                .unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
    }

    private Building building(Long tenantId) {
        Building building = new Building();
        building.setName("Building " + (++seq));
        building.setAddress("1 Test Street");
        building.setTenantId(tenantId);
        return buildingRepository.saveAndFlush(building);
    }

    private Room room(Long tenantId, String number) {
        Room room = new Room();
        room.setBuilding(building(tenantId));
        room.setRoomNumber(number);
        room.setCapacity(2);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        room.setTenantId(tenantId);
        return roomRepository.saveAndFlush(room);
    }

    private Student student() {
        AppUser user = new AppUser();
        user.setEmail("tenant-filter-" + (++seq) + "@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Filter");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user = appUserRepository.saveAndFlush(user);

        Student student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        return studentRepository.saveAndFlush(student);
    }

    private Application application(Long tenantId, Student student) {
        Application application = Application.builder()
                .applicationCode("APP-" + (++seq))
                .applicationDate(LocalDate.now())
                .status(ApplicationStatus.PENDING)
                .student(student)
                .tenantId(tenantId)
                .build();
        return applicationRepository.saveAndFlush(application);
    }

    private RoomAllocation allocation(Long tenantId) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setRoom(room(tenantId, "R" + (++seq)));
        allocation.setStudent(student());
        allocation.setTenantId(tenantId);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setAllocationDate(LocalDateTime.now());
        return roomAllocationRepository.saveAndFlush(allocation);
    }

    private void setting(Long tenantId, String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setTenantId(tenantId);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        systemSettingRepository.saveAndFlush(setting);
    }
}
