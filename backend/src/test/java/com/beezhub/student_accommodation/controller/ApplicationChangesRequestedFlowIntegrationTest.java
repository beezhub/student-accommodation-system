package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The review loop over HTTP, with the real listener, template and SMTP server (GH-455).
 *
 * <p>The listener used to mark the notification SENT without sending anything, so what matters here
 * is that the row only reaches SENT by going through the real template and mail server.
 */
@DisplayName("Changes requested flow (integration)")
class ApplicationChangesRequestedFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String REASON = "Proof of registration is for the wrong year";

    @Autowired TestRestTemplate rest;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired JwtUtil jwtUtil;

    private Tenant tenant;
    private AppUser studentUser;
    private AppUser adminUser;
    private Student student;
    private Application application;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        tenant = tenantRepository.save(Tenant.builder()
                .name("Changes Company " + System.nanoTime())
                .isActive(true)
                .build());
        studentUser = saveUser(UserRole.STUDENT, TenantContext.PLATFORM_TENANT_ID);
        adminUser = saveUser(UserRole.TENANT_ADMIN, tenant.getId());
        student = new Student();
        student.setAppUser(studentUser);
        student.setStudentNumber("S-" + System.nanoTime());
        student.setPhoneNumber("0123456789");
        student.setGender("Female");
        student = studentRepository.save(student);
        application = applicationRepository.save(Application.builder()
                .applicationCode("APP-" + System.nanoTime())
                .applicationDate(LocalDate.now())
                .status(ApplicationStatus.PENDING)
                .student(student)
                .tenantId(tenant.getId())
                .build());
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        jdbcTemplate.update("DELETE FROM notification WHERE recipient = ?", studentUser.getEmail());
        jdbcTemplate.update("DELETE FROM user_notifications WHERE user_id = ?", studentUser.getId());
        applicationRepository.deleteById(application.getId());
        studentRepository.delete(student);
        appUserRepository.deleteAll(List.of(studentUser, adminUser));
        tenantRepository.delete(tenant);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Admin requests changes, the student is emailed, the student resubmits")
    void reviewLoop_requestChangesThenResubmit_emailsTheReasonAndRequeues() throws InterruptedException {
        String adminToken = tokenFor(adminUser);
        assertThat(post("/mark-under-review", adminToken, null).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> requested = post("/request-changes", adminToken, Map.of("reason", REASON));
        assertThat(requested.getStatusCode()).isEqualTo(HttpStatus.OK);

        Notification notification = awaitSentNotification();
        assertThat(notification.getType()).isEqualTo(NotificationType.APPLICATION_CHANGES_REQUESTED);
        assertThat(notification.getTemplateData())
                .containsEntry("templateName", "application-changes-requested")
                .containsEntry("changesReason", REASON)
                .containsEntry("portalUrl", "http://localhost:4200/applications/" + application.getId());

        ResponseEntity<String> resubmitted = post("/resubmit", tokenFor(studentUser), null);
        assertThat(resubmitted.getStatusCode()).isEqualTo(HttpStatus.OK);

        Application requeued = applicationRepository.findById(application.getId()).orElseThrow();
        assertThat(requeued.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(requeued.getRemarks()).isEqualTo(REASON);
        assertThat(notificationRepository.findByRecipient(studentUser.getEmail())).hasSize(1);
    }

    /** The listener runs on the notification executor after commit, so the row arrives shortly. */
    private Notification awaitSentNotification() throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            List<Notification> rows = notificationRepository.findByRecipient(studentUser.getEmail());
            if (!rows.isEmpty() && rows.get(0).getStatus() != NotificationStatus.PENDING) {
                assertThat(rows.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
                return rows.get(0);
            }
            Thread.sleep(200);
        }
        throw new AssertionError("The notification never reached a final status");
    }

    private ResponseEntity<String> post(String action, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange("/api/applications/" + application.getId() + action, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
    }

    private String tokenFor(AppUser user) {
        return jwtUtil.generateToken(new AppUserDetails(
                user.getEmail(), "", user.getUserRole().name(), user.getTenantId()));
    }

    private AppUser saveUser(UserRole role, Long tenantId) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "." + System.nanoTime() + "@test.com")
                .firstName("Test")
                .lastName("User")
                .userPassword("$2a$10$hashed")
                .userRole(role)
                .tenantId(tenantId)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
    }
}
