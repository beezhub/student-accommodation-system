package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who is a returning resident, on PostgreSQL (GH-499): checked in at the same company in an earlier
 * year or living there now, a terminated lease included.
 */
@DisplayName("Returning residents (integration)")
class ReturningResidentIntegrationTest extends AbstractIntegrationTest {

    @Autowired ReturningResidentPolicy returningResidentPolicy;
    @Autowired JdbcTemplate jdbc;
    @Autowired TenantRepository tenantRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired StudentRepository studentRepository;

    private final List<AppUser> users = new ArrayList<>();
    private Long companyA;
    private Long companyB;
    private Long roomA;
    private Long roomB;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        companyA = tenantRepository.save(Tenant.builder().name("Return A " + System.nanoTime()).isActive(true).build()).getId();
        companyB = tenantRepository.save(Tenant.builder().name("Return B " + System.nanoTime()).isActive(true).build()).getId();
        TenantContext.clear();
        roomA = room(companyA);
        roomB = room(companyB);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        for (Long company : List.of(companyA, companyB)) {
            jdbc.update("DELETE FROM room_allocation WHERE tenant_id = ?", company);
            jdbc.update("DELETE FROM application WHERE tenant_id = ?", company);
            jdbc.update("DELETE FROM room WHERE tenant_id = ?", company);
            jdbc.update("DELETE FROM building WHERE tenant_id = ?", company);
        }
        for (AppUser user : users) {
            jdbc.update("DELETE FROM student WHERE user_id = ?", user.getId());
            jdbc.update("DELETE FROM app_user WHERE id = ?", user.getId());
        }
        jdbc.update("DELETE FROM tenant WHERE id IN (?, ?)", companyA, companyB);
    }

    @Test
    @DisplayName("Set for a current resident, a past resident and a terminated lease")
    void isReturning_livedThere_isTrue() {
        Student current = student();
        Student past = student();
        Student terminated = student();
        Student checkedInBefore = student();
        allocation(current, companyA, roomA, "ACTIVE");
        allocation(past, companyA, roomA, "COMPLETED");
        allocation(terminated, companyA, roomA, "TERMINATED");
        checkedInApplication(checkedInBefore, companyA);

        assertThat(returningResidentPolicy.isReturning(current.getId(), companyA)).isTrue();
        assertThat(returningResidentPolicy.isReturning(past.getId(), companyA)).isTrue();
        assertThat(returningResidentPolicy.isReturning(terminated.getId(), companyA)).isTrue();
        assertThat(returningResidentPolicy.isReturning(checkedInBefore.getId(), companyA)).isTrue();
    }

    @Test
    @DisplayName("Not set for a first-time applicant or a resident of another company")
    void isReturning_neverLivedThere_isFalse() {
        Student firstTime = student();
        Student elsewhere = student();
        Student onlyOffered = student();
        allocation(elsewhere, companyB, roomB, "ACTIVE");
        allocation(onlyOffered, companyA, roomA, "DECLINED");

        assertThat(returningResidentPolicy.isReturning(firstTime.getId(), companyA)).isFalse();
        assertThat(returningResidentPolicy.isReturning(elsewhere.getId(), companyA)).isFalse();
        assertThat(returningResidentPolicy.isReturning(elsewhere.getId(), companyB)).isTrue();
        assertThat(returningResidentPolicy.isReturning(onlyOffered.getId(), companyA)).isFalse();
    }

    private Long room(Long tenantId) {
        Long building = jdbc.queryForObject("INSERT INTO building (name, address, tenant_id) VALUES (?, '1 Street', ?) "
                + "RETURNING id", Long.class, "Return Hall " + System.nanoTime(), tenantId);
        return jdbc.queryForObject("INSERT INTO room (room_number, capacity, current_occupancy, status, building_id, "
                + "tenant_id) VALUES (?, 2, 0, 'AVAILABLE', ?, ?) RETURNING id", Long.class,
                "R" + System.nanoTime() % 100000, building, tenantId);
    }

    private void allocation(Student student, Long tenantId, Long roomId, String status) {
        jdbc.update("INSERT INTO room_allocation (student_id, room_id, tenant_id, status, allocation_date, "
                + "created_at, updated_at, version) VALUES (?, ?, ?, ?, NOW(), NOW(), NOW(), 0)",
                student.getId(), roomId, tenantId, status);
    }

    private void checkedInApplication(Student student, Long tenantId) {
        jdbc.update("INSERT INTO application (application_code, application_date, status, student_id, tenant_id, "
                + "academic_year_id, intake_id, version) VALUES (?, ?, 'CHECKED_IN', ?, ?, ?, ?, 0)",
                "APP-" + System.nanoTime(), LocalDate.now(), student.getId(), tenantId,
                testCalendar().yearId(), testCalendar().intakeId());
    }

    private Student student() {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("returning." + System.nanoTime() + "@test.com")
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
