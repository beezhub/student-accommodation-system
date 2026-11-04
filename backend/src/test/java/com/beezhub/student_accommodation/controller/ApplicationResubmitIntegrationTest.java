package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may resubmit an application after an admin asked for changes (GH-454).
 *
 * <p>Students are tenant 0, so the tenant filter does nothing for them. The explicit ownership
 * check in the service is the only thing that stops one student acting on another's application.
 */
@DisplayName("Application resubmit (integration)")
class ApplicationResubmitIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired JwtUtil jwtUtil;

    private final List<Application> applications = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<AppUser> users = new ArrayList<>();

    private Tenant tenant;
    private Student student;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        tenant = tenantRepository.save(Tenant.builder()
                .name("Resubmit Company " + System.nanoTime())
                .isActive(true)
                .build());
        owner = saveUser(UserRole.STUDENT, TenantContext.PLATFORM_TENANT_ID);
        student = saveStudent(owner);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        // By id: the copies held here are stale once a request has moved the row, and deleting a
        // stale copy fails the version check.
        applications.forEach(application -> applicationRepository.deleteById(application.getId()));
        studentRepository.deleteAll(students);
        appUserRepository.deleteAll(users);
        tenantRepository.delete(tenant);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("The owner moves their application back to PENDING")
    void resubmit_owner_returnsOkAndMovesToPending() {
        Application application = saveApplication(ApplicationStatus.CHANGES_REQUESTED);

        ResponseEntity<String> response = post(application, tokenFor(owner));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reload(application).getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("Another student is refused")
    void resubmit_anotherStudent_returnsForbidden() {
        Application application = saveApplication(ApplicationStatus.CHANGES_REQUESTED);
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        AppUser other = saveUser(UserRole.STUDENT, TenantContext.PLATFORM_TENANT_ID);
        saveStudent(other);
        TenantContext.clear();

        ResponseEntity<String> response = post(application, tokenFor(other));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reload(application).getStatus()).isEqualTo(ApplicationStatus.CHANGES_REQUESTED);
    }

    @Test
    @DisplayName("An application under review cannot be pulled back by the student")
    void resubmit_underReview_returnsBadRequest() {
        Application application = saveApplication(ApplicationStatus.UNDER_REVIEW);

        ResponseEntity<String> response = post(application, tokenFor(owner));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reload(application).getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("An admin is refused: resubmitting is the student's statement")
    void resubmit_platformAdmin_returnsForbidden() {
        Application application = saveApplication(ApplicationStatus.CHANGES_REQUESTED);
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        AppUser admin = saveUser(UserRole.PLATFORM_ADMIN, TenantContext.PLATFORM_TENANT_ID);
        TenantContext.clear();

        ResponseEntity<String> response = post(application, tokenFor(admin));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reload(application).getStatus()).isEqualTo(ApplicationStatus.CHANGES_REQUESTED);
    }

    private ResponseEntity<String> post(Application application, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange("/api/applications/" + application.getId() + "/resubmit", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
    }

    private Application reload(Application application) {
        return applicationRepository.findById(application.getId()).orElseThrow();
    }

    private String tokenFor(AppUser user) {
        return jwtUtil.generateToken(new AppUserDetails(
                user.getEmail(), "", user.getUserRole().name(), user.getTenantId()));
    }

    private AppUser saveUser(UserRole role, Long tenantId) {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "." + System.nanoTime() + "@test.com")
                .firstName("Test")
                .lastName("User")
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
        saved.setDateOfBirth(new Date());
        saved.setGender("Female");
        saved = studentRepository.save(saved);
        students.add(saved);
        return saved;
    }

    private Application saveApplication(ApplicationStatus status) {
        Application application = applicationRepository.save(Application.builder()
                .applicationCode("APP-" + System.nanoTime())
                .applicationDate(LocalDate.now())
                .status(status)
                .student(student)
                .tenantId(tenant.getId())
                .remarks("Proof of registration is for the wrong year")
                .build());
        applications.add(application);
        return application;
    }
}
