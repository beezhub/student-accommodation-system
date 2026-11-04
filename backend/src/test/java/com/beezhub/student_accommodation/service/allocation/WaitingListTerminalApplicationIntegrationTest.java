package com.beezhub.student_accommodation.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.application.ApplicationAdminService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A rejected application used to leave its row WAITING, and the student could not be queued again.
 *
 * <p>Runs on real PostgreSQL as a tenant admin, so the tenant filter is on for the lookup of the
 * row, the same as the real request.
 */
class WaitingListTerminalApplicationIntegrationTest extends AbstractIntegrationTest {

    private static final String REPAIR_MIGRATION =
            "db/migration/V2026_11_04_1500__cancel_waiting_entries_of_closed_applications.sql";

    @Autowired private ApplicationAdminService applicationAdminService;
    @Autowired private WaitingListService waitingListService;
    @Autowired private WaitingListRepository waitingListRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long tenantId;
    private Student student;

    @BeforeEach
    void seed() {
        tenantId = jdbcTemplate.queryForObject(
                "INSERT INTO tenant (name, config, is_active, created_at, updated_at) "
                        + "VALUES (?, '{}', true, current_timestamp, current_timestamp) RETURNING id",
                Long.class, "Waiting List Test " + System.nanoTime());
        TenantContext.setTenantId(tenantId);

        AppUser user = new AppUser();
        user.setEmail("waiting-" + System.nanoTime() + "@test.local");
        user.setUserPassword("not-a-real-password");
        user.setFirstName("Waiting");
        user.setLastName("Test");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        user = appUserRepository.save(user);

        student = new Student();
        student.setAppUser(user);
        student.setRegistrationDate(new Date());
        student = studentRepository.save(student);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
    }

    /**
     * {@code fk_waiting_list_student} and {@code fk_waiting_list_application} have no ON DELETE, and
     * other classes sharing these containers delete every application and student. The tenant row
     * stays: notifications written after commit may still point at it.
     */
    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM waiting_list_entry WHERE student_id = ?", student.getId());
        jdbcTemplate.update("DELETE FROM application WHERE student_id = ?", student.getId());
        jdbcTemplate.update("DELETE FROM student WHERE id = ?", student.getId());
        jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", student.getAppUser().getId());
    }

    @Test
    void updateApplicationStatus_rejectedWhileWaiting_rowLeavesQueueAndStudentCanQueueAgain() {
        Application rejected = givenApplication(ApplicationStatus.AWAITING_ALLOCATION);
        Long rejectedRow = givenEntry(rejected, "WAITING");

        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus(ApplicationStatus.REJECTED.name());
        request.setRemarks("Not eligible");
        applicationAdminService.updateApplicationStatus(rejected.getId(), request);

        assertThat(entryStatus(rejectedRow)).isEqualTo("CANCELLED");

        Application second = givenApplication(ApplicationStatus.AWAITING_ALLOCATION);
        transactionTemplate.executeWithoutResult(status ->
                waitingListService.enqueueWaitingList(student, second));

        assertThat(jdbcTemplate.queryForList(
                "SELECT application_id FROM waiting_list_entry WHERE student_id = ? AND status = 'WAITING'",
                Long.class, student.getId()))
                .containsExactly(second.getId());
    }

    @Test
    void repairMigration_waitingRowOfClosedApplication_cancelledAndOthersLeftAlone() throws Exception {
        Long stuck = givenEntry(givenApplication(ApplicationStatus.REJECTED), "WAITING");
        Long served = givenEntry(givenApplication(ApplicationStatus.CANCELLED), "ALLOCATED");
        Long stillWaiting = givenEntry(givenApplication(ApplicationStatus.AWAITING_ALLOCATION), "WAITING");

        jdbcTemplate.execute(new ClassPathResource(REPAIR_MIGRATION).getContentAsString(StandardCharsets.UTF_8));

        assertThat(entryStatus(stuck)).isEqualTo("CANCELLED");
        assertThat(entryStatus(served)).isEqualTo("ALLOCATED");
        assertThat(entryStatus(stillWaiting)).isEqualTo("WAITING");
    }

    private String entryStatus(Long entryId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM waiting_list_entry WHERE id = ?", String.class, entryId);
    }

    private Application givenApplication(ApplicationStatus status) {
        Application application = new Application();
        application.setApplicationCode("WL-" + System.nanoTime());
        application.setStudent(student);
        application.setStatus(status);
        application.setTenantId(tenantId);
        return applicationRepository.save(application);
    }

    private Long givenEntry(Application application, String status) {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus(status);
        entry.setTenantId(tenantId);
        return waitingListRepository.save(entry).getId();
    }
}
