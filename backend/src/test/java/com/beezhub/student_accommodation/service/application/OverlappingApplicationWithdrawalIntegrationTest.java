package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.dto.AcceptOfferRequest;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accepting a place withdraws the student's overlapping applications at every company, on
 * PostgreSQL (GH-497). This is the first write across companies, so it is also the isolation test:
 * another student's applications are never touched, and a rolled-back accept withdraws nothing.
 */
@DisplayName("Accepting an offer withdraws overlapping applications (integration)")
class OverlappingApplicationWithdrawalIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate ANNUAL_START = LocalDate.of(2091, 2, 1);
    private static final LocalDate ANNUAL_END = LocalDate.of(2091, 11, 30);

    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired TenantRepository tenantRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired RoomAllocationRepository roomAllocationRepository;
    @Autowired RoomAllocationService roomAllocationService;

    private final List<AppUser> users = new ArrayList<>();
    private final List<Tenant> tenants = new ArrayList<>();

    private AppUser studentUser;
    private Student student;
    private Student otherStudent;
    private AppUser companyBAdmin;

    private Tenant companyA;
    private Tenant companyB;
    private Tenant companyC;
    private Tenant companyD;

    private RoomAllocation offerAtA;
    private Application underReviewAtB;
    private Application approvedAtC;
    private RoomAllocation offerAtC;
    private Application nextYearAtD;
    private Application otherStudentAtB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        companyA = company("A");
        companyB = company("B");
        companyC = company("C");
        companyD = company("D");
        studentUser = saveUser(UserRole.STUDENT, TenantContext.PLATFORM_TENANT_ID);
        student = saveStudent(studentUser);
        otherStudent = saveStudent(saveUser(UserRole.STUDENT, TenantContext.PLATFORM_TENANT_ID));
        companyBAdmin = saveUser(UserRole.TENANT_ADMIN, companyB.getId());

        // A: the offer the student accepts, Annual 2091.
        Long[] annualA = calendar(companyA, "Annual", "ANNUAL", ANNUAL_START, ANNUAL_END);
        Application atA = application(companyA, student, annualA, ApplicationStatus.APPROVED);
        offerAtA = offer(companyA, atA, ANNUAL_START, ANNUAL_END);
        // B: Semester 1 2091 overlaps, under review. The other student applied there too.
        Long[] semesterB = calendar(companyB, "Semester 1", "SEMESTER", ANNUAL_START, LocalDate.of(2091, 6, 30));
        underReviewAtB = application(companyB, student, semesterB, ApplicationStatus.UNDER_REVIEW);
        otherStudentAtB = application(companyB, otherStudent, semesterB, ApplicationStatus.PENDING);
        // C: Annual 2091 overlaps, approved with an open offer.
        Long[] annualC = calendar(companyC, "Annual", "ANNUAL", ANNUAL_START, ANNUAL_END);
        approvedAtC = application(companyC, student, annualC, ApplicationStatus.APPROVED);
        offerAtC = offer(companyC, approvedAtC, ANNUAL_START, ANNUAL_END);
        // D: Annual 2092 does not overlap.
        Long[] annualD = calendar(companyD, "Annual", "ANNUAL", LocalDate.of(2092, 2, 1), LocalDate.of(2092, 11, 30));
        nextYearAtD = application(companyD, student, annualD, ApplicationStatus.PENDING);

        TenantContext.clear();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(studentUser.getEmail(), null, List.of()));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Emails and company notifications are written by async listeners after commit.
        Thread.sleep(1500);
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        for (Tenant tenant : tenants) {
            jdbc.update("DELETE FROM allocation_audit_log WHERE allocation_id IN "
                    + "(SELECT id FROM room_allocation WHERE tenant_id = ?)", tenant.getId());
            jdbc.update("DELETE FROM room_allocation WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM waiting_list_entry WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM application WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM room WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM building WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM intake WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM academic_year WHERE tenant_id = ?", tenant.getId());
        }
        for (AppUser user : users) {
            jdbc.update("DELETE FROM notification WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM user_notifications WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM student WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM app_user WHERE id = ?", user.getId());
        }
        for (Tenant tenant : tenants) {
            jdbc.update("DELETE FROM activity_logs WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM user_notifications WHERE tenant_id = ?", tenant.getId());
            jdbc.update("DELETE FROM tenant WHERE id = ?", tenant.getId());
        }
    }

    @Test
    @DisplayName("Overlapping applications at two other companies are withdrawn; the rest are kept")
    void acceptOffer_overlappingAtTwoCompanies_withdrawnAndOthersUntouched() throws InterruptedException {
        roomAllocationService.acceptOffer(offerAtA.getId(), new AcceptOfferRequest());

        assertThat(allocationStatus(offerAtA)).isEqualTo("ACCEPTED");
        assertThat(status(underReviewAtB)).isEqualTo("CANCELLED");
        assertThat(status(approvedAtC)).isEqualTo("CANCELLED");
        assertThat(allocationStatus(offerAtC)).isEqualTo("CANCELLED");
        assertThat(status(nextYearAtD)).isEqualTo("PENDING");
        assertThat(status(otherStudentAtB)).isEqualTo("PENDING");
        assertThat(roomAllocationRepository.countHoldingBeds(offerAtC.getRoom().getId(), ANNUAL_START, ANNUAL_END))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM allocation_audit_log WHERE allocation_id = ? "
                + "AND new_status = 'CANCELLED'", Integer.class, offerAtC.getId())).isPositive();

        String email = await("SELECT template_data::text FROM notification WHERE user_id = ? "
                + "AND template_data->>'templateName' = 'applications-withdrawn'", studentUser.getId());
        assertThat(email).contains(underReviewAtB.getApplicationCode()).contains(approvedAtC.getApplicationCode())
                .doesNotContain(nextYearAtD.getApplicationCode()).doesNotContain(otherStudentAtB.getApplicationCode());
        String companyNotice = await("SELECT message FROM user_notifications WHERE user_id = ?", companyBAdmin.getId());
        assertThat(companyNotice).contains(underReviewAtB.getApplicationCode())
                .doesNotContain(approvedAtC.getApplicationCode()).doesNotContain(companyA.getName());
    }

    @Test
    @DisplayName("A rolled-back accept withdraws nothing")
    void acceptOffer_rolledBack_cancelsNothing() {
        transactionTemplate.executeWithoutResult(tx -> {
            roomAllocationService.acceptOffer(offerAtA.getId(), new AcceptOfferRequest());
            tx.setRollbackOnly();
        });

        assertThat(allocationStatus(offerAtA)).isEqualTo("OFFERED");
        assertThat(status(underReviewAtB)).isEqualTo("UNDER_REVIEW");
        assertThat(status(approvedAtC)).isEqualTo("APPROVED");
        assertThat(allocationStatus(offerAtC)).isEqualTo("OFFERED");
        assertThat(status(nextYearAtD)).isEqualTo("PENDING");
    }

    private String await(String sql, Long id) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            List<String> rows = jdbc.queryForList(sql, String.class, id);
            if (!rows.isEmpty()) {
                return rows.get(0);
            }
            Thread.sleep(100);
        }
        throw new AssertionError("nothing written for " + id + " by: " + sql);
    }

    private String status(Application application) {
        return jdbc.queryForObject("SELECT status FROM application WHERE id = ?", String.class, application.getId());
    }

    private String allocationStatus(RoomAllocation allocation) {
        return jdbc.queryForObject("SELECT status FROM room_allocation WHERE id = ?", String.class, allocation.getId());
    }

    private Tenant company(String name) {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Company " + name + " " + System.nanoTime()).isActive(true).build());
        tenants.add(tenant);
        return tenant;
    }

    /** A year and one intake for the company: {yearId, intakeId}. */
    private Long[] calendar(Tenant company, String intakeName, String type, LocalDate start, LocalDate end) {
        Long yearId = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, tenant_id) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, "W" + System.nanoTime(), start, end, company.getId());
        Long intakeId = jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, company.getId(), yearId, type, intakeName, start, end,
                start.minusDays(120), start.minusDays(14));
        return new Long[] {yearId, intakeId};
    }

    private Application application(Tenant company, Student owner, Long[] calendar, ApplicationStatus status) {
        TenantContext.setTenantId(company.getId());
        try {
            return applicationRepository.save(Application.builder()
                    .academicYear(AcademicYear.builder().id(calendar[0]).build())
                    .intake(Intake.builder().id(calendar[1]).build())
                    .applicationCode("APP-" + System.nanoTime())
                    .applicationDate(LocalDate.now())
                    .status(status)
                    .student(owner)
                    .tenantId(company.getId())
                    .build());
        } finally {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        }
    }

    private RoomAllocation offer(Tenant company, Application application, LocalDate start, LocalDate end) {
        TenantContext.setTenantId(company.getId());
        try {
            Building building = new Building();
            building.setName("Hall " + System.nanoTime());
            building.setAddress("1 Test Street");
            building.setTenantId(company.getId());
            building = buildingRepository.save(building);
            Room room = new Room();
            room.setBuilding(building);
            room.setRoomNumber("R" + System.nanoTime() % 100000);
            room.setCapacity(1);
            room.setCurrentOccupancy(0);
            room.setStatus("AVAILABLE");
            room.setTenantId(company.getId());
            room = roomRepository.save(room);

            RoomAllocation allocation = new RoomAllocation();
            allocation.setStudent(application.getStudent());
            allocation.setRoom(room);
            allocation.setApplication(application);
            allocation.setTenantId(company.getId());
            allocation.setStatus(AllocationStatus.OFFERED);
            allocation.setAllocationDate(LocalDateTime.now());
            allocation.setOfferExpiryDate(LocalDateTime.now().plusDays(5));
            allocation.setStayStartDate(start);
            allocation.setStayEndDate(end);
            return roomAllocationRepository.save(allocation);
        } finally {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        }
    }

    private AppUser saveUser(UserRole role, Long tenantId) {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("withdraw." + System.nanoTime() + "@test.com")
                .firstName("Test")
                .lastName(role.name())
                .userPassword("$2a$10$hashed")
                .userRole(role)
                .tenantId(tenantId)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        users.add(user);
        return user;
    }

    private Student saveStudent(AppUser user) {
        Student saved = new Student();
        saved.setAppUser(user);
        saved.setStudentNumber("S-" + System.nanoTime());
        saved.setPhoneNumber("0123456789");
        saved.setDateOfBirth(LocalDate.of(2000, 5, 14));
        saved.setGender("Female");
        return studentRepository.save(saved);
    }
}
