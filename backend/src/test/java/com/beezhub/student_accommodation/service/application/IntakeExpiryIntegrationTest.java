package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The intake expiry job on PostgreSQL (GH-497): the queries by stay end date, the waiting-list
 * entries it closes, and the email after commit offering the next open intake at the company.
 */
@DisplayName("Applications expire with their intake (integration)")
class IntakeExpiryIntegrationTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired TenantRepository tenantRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired IntakeExpiryService intakeExpiryService;

    private final List<AppUser> users = new ArrayList<>();
    private final List<Long> yearIds = new ArrayList<>();
    private Tenant tenant;
    private LocalDate today;

    private Long endedYear;
    private Long currentYear;
    private Long semester1;
    private Long annual;
    private Long semester2;
    private Long nextIntake;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(clock);
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        tenant = tenantRepository.save(Tenant.builder().name("Expiry Co " + System.nanoTime()).isActive(true).build());

        // Last year ended yesterday: its Annual intake ran to the year end, Semester 1 ended earlier.
        endedYear = year("Ended " + System.nanoTime(), today.minusDays(300), today.minusDays(1));
        annual = intake(endedYear, "ANNUAL", "Annual", today.minusDays(300), today.minusDays(1));
        semester1 = intake(endedYear, "SEMESTER", "Semester 1", today.minusDays(300), today.minusDays(150));
        // This year is running; one of its intakes takes applications today.
        currentYear = year("Now " + System.nanoTime(), today.minusDays(60), today.plusDays(300));
        semester2 = intake(currentYear, "SEMESTER", "Semester 2", today.minusDays(60), today.plusDays(60));
        nextIntake = intake(currentYear, "TRIMESTER", "Trimester 3", today.plusDays(30), today.plusDays(200),
                today.minusDays(10), today.plusDays(10));
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // The expiry emails are written by an async listener after commit; wait for them so their
        // notification rows do not outlive the users they point at.
        for (int i = 0; i < 50 && !users.isEmpty() && notificationsFor(users.get(0)) == 0; i++) {
            Thread.sleep(100);
        }
        Thread.sleep(300);
        TenantContext.clear();
        jdbc.update("DELETE FROM waiting_list_entry WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM application WHERE tenant_id = ?", tenant.getId());
        jdbc.update("DELETE FROM intake WHERE tenant_id = ?", tenant.getId());
        yearIds.forEach(id -> jdbc.update("DELETE FROM academic_year WHERE id = ?", id));
        for (AppUser user : users) {
            jdbc.update("DELETE FROM notification WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM user_notifications WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM student WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM app_user WHERE id = ?", user.getId());
        }
        jdbc.update("DELETE FROM tenant WHERE id = ?", tenant.getId());
    }

    @Test
    @DisplayName("An ended Semester 1 and an Annual intake at its year end expire; a running intake is kept")
    void runDaily_endedAndRunningIntakes_onlyTheEndedOnesExpire() throws InterruptedException {
        Application endedPending = application(endedYear, semester1, ApplicationStatus.PENDING);
        Application endedWaiting = application(endedYear, semester1, ApplicationStatus.AWAITING_ALLOCATION);
        Application annualReview = application(endedYear, annual, ApplicationStatus.UNDER_REVIEW);
        Application running = application(currentYear, semester2, ApplicationStatus.PENDING);
        Application allocated = application(endedYear, semester1, ApplicationStatus.ALLOCATED);
        jdbc.update("INSERT INTO waiting_list_entry (student_id, application_id, status, tenant_id) VALUES (?, ?, 'WAITING', ?)",
                endedWaiting.getStudent().getId(), endedWaiting.getId(), tenant.getId());

        int expired = intakeExpiryService.runDaily();

        assertThat(expired).isGreaterThanOrEqualTo(3);
        assertThat(statusOf(endedPending)).isEqualTo("EXPIRED");
        assertThat(statusOf(endedWaiting)).isEqualTo("EXPIRED");
        assertThat(statusOf(annualReview)).isEqualTo("EXPIRED");
        assertThat(statusOf(running)).isEqualTo("PENDING");
        assertThat(statusOf(allocated)).isEqualTo("ALLOCATED");
        assertThat(jdbc.queryForObject("SELECT status FROM waiting_list_entry WHERE application_id = ?",
                String.class, endedWaiting.getId())).isEqualTo("CANCELLED");
        assertThat(TenantContext.getTenantId()).isEqualTo(TenantContext.PLATFORM_TENANT_ID);

        String stored = awaitEmail(users.get(0));
        assertThat(stored).contains("application-expired").contains("Trimester 3");
    }

    private String awaitEmail(AppUser user) throws InterruptedException {
        for (int i = 0; i < 100 && notificationsFor(user) == 0; i++) {
            Thread.sleep(100);
        }
        return jdbc.queryForObject("SELECT template_data::text FROM notification WHERE user_id = ? LIMIT 1",
                String.class, user.getId());
    }

    private int notificationsFor(AppUser user) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE user_id = ?",
                Integer.class, user.getId());
        return count == null ? 0 : count;
    }

    private String statusOf(Application application) {
        return jdbc.queryForObject("SELECT status FROM application WHERE id = ?", String.class, application.getId());
    }

    private Long year(String name, LocalDate start, LocalDate end) {
        Long id = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, tenant_id) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, name, start, end, tenant.getId());
        yearIds.add(id);
        return id;
    }

    private Long intake(Long yearId, String type, String name, LocalDate start, LocalDate end) {
        return intake(yearId, type, name, start, end, start.minusDays(120), start.minusDays(14));
    }

    private Long intake(Long yearId, String type, String name, LocalDate start, LocalDate end,
            LocalDate opens, LocalDate closes) {
        return jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, tenant.getId(), yearId, type, name, start, end, opens, closes);
    }

    private Application application(Long yearId, Long intakeId, ApplicationStatus status) {
        Student student = saveStudent();
        TenantContext.setTenantId(tenant.getId());
        try {
            return applicationRepository.save(Application.builder()
                    .academicYear(AcademicYear.builder().id(yearId).build())
                    .intake(Intake.builder().id(intakeId).build())
                    .applicationCode("APP-" + System.nanoTime())
                    .applicationDate(today.minusDays(200))
                    .status(status)
                    .student(student)
                    .tenantId(tenant.getId())
                    .build());
        } finally {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        }
    }

    private Student saveStudent() {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("expiry." + System.nanoTime() + "@test.com")
                .firstName("Test")
                .lastName("Student")
                .userPassword("$2a$10$hashed")
                .userRole(UserRole.STUDENT)
                .tenantId(TenantContext.PLATFORM_TENANT_ID)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        users.add(user);
        Student student = new Student();
        student.setAppUser(user);
        student.setStudentNumber("S-" + System.nanoTime());
        student.setPhoneNumber("0123456789");
        student.setDateOfBirth(LocalDate.of(2000, 5, 14));
        student.setGender("Female");
        return studentRepository.save(student);
    }
}
