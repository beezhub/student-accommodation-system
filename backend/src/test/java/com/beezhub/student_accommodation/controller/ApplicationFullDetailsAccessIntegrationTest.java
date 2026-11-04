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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may open an application's full details, the call behind the admin review dialog (GH-437).
 *
 * <p>Two routes serve the same data. {@code /api/applications/{id}/full-details} is the one the
 * frontend calls, and it used to refuse tenant admins, so they could not review anything.
 * {@code /api/admin/applications/{id}/full-details} let them in but never checked the tenant, and
 * Hibernate's filter does not apply to {@code findById}, so any tenant admin could read another
 * company's application by changing the id. Every tenant case here runs against both routes.
 */
@DisplayName("Application full details access (integration)")
class ApplicationFullDetailsAccessIntegrationTest extends AbstractIntegrationTest {

    private static final String PUBLIC_ROUTE = "/api/applications/%d/full-details";
    private static final String ADMIN_ROUTE = "/api/admin/applications/%d/full-details";

    @Autowired TestRestTemplate rest;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired JwtUtil jwtUtil;

    private final List<Application> applications = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<AppUser> users = new ArrayList<>();
    private final List<Tenant> tenants = new ArrayList<>();

    private Tenant ownTenant;
    private Tenant otherTenant;
    private Application ownApplication;
    private Application otherApplication;
    private AppUser owningStudent;
    private String tenantAdminToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);

        ownTenant = saveTenant("Own Company");
        otherTenant = saveTenant("Other Company");

        owningStudent = saveUser(UserRole.STUDENT, TenantContext.PLATFORM_TENANT_ID);
        Student student = saveStudent(owningStudent);
        ownApplication = saveApplication(student, ownTenant);
        otherApplication = saveApplication(saveStudent(saveUser(UserRole.STUDENT, TenantContext.PLATFORM_TENANT_ID)),
                otherTenant);

        tenantAdminToken = tokenFor(saveUser(UserRole.TENANT_ADMIN, ownTenant.getId()));

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        applicationRepository.deleteAll(applications);
        studentRepository.deleteAll(students);
        appUserRepository.deleteAll(users);
        tenantRepository.deleteAll(tenants);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {PUBLIC_ROUTE, ADMIN_ROUTE})
    @DisplayName("TENANT_ADMIN opens an application in their own tenant")
    void fullDetails_tenantAdminOwnTenant_returnsOk(String route) {
        ResponseEntity<String> response = get(route, ownApplication, tenantAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(ownApplication.getApplicationCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {PUBLIC_ROUTE, ADMIN_ROUTE})
    @DisplayName("TENANT_ADMIN gets 404 for an application in another tenant")
    void fullDetails_tenantAdminOtherTenant_returnsNotFound(String route) {
        ResponseEntity<String> response = get(route, otherApplication, tenantAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).doesNotContain(otherApplication.getApplicationCode());
    }

    @Test
    @DisplayName("PLATFORM_STAFF opens an application in any tenant")
    void fullDetails_platformStaff_returnsOk() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        String staffToken = tokenFor(saveUser(UserRole.PLATFORM_STAFF, TenantContext.PLATFORM_TENANT_ID));
        TenantContext.clear();

        assertThat(get(PUBLIC_ROUTE, otherApplication, staffToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PLATFORM_ADMIN opens an application in any tenant")
    void fullDetails_platformAdmin_returnsOk() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        String adminToken = tokenFor(saveUser(UserRole.PLATFORM_ADMIN, TenantContext.PLATFORM_TENANT_ID));
        TenantContext.clear();

        assertThat(get(PUBLIC_ROUTE, otherApplication, adminToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("A student opens their own application")
    void fullDetails_owningStudent_returnsOk() {
        assertThat(get(PUBLIC_ROUTE, ownApplication, tokenFor(owningStudent)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("A student cannot open another student's application")
    void fullDetails_otherStudent_returnsForbidden() {
        assertThat(get(PUBLIC_ROUTE, otherApplication, tokenFor(owningStudent)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> get(String route, Application application, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(route.formatted(application.getId()), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }

    private String tokenFor(AppUser user) {
        return jwtUtil.generateToken(new AppUserDetails(
                user.getEmail(), "", user.getUserRole().name(), user.getTenantId()));
    }

    private Tenant saveTenant(String name) {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(name + " " + System.nanoTime())
                .isActive(true)
                .build());
        tenants.add(tenant);
        return tenant;
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
        Student student = new Student();
        student.setAppUser(user);
        student.setStudentNumber("S-" + System.nanoTime());
        student.setPhoneNumber("0123456789");
        student.setDateOfBirth(LocalDate.of(2000, 5, 14));
        student.setGender("Female");
        student = studentRepository.save(student);
        students.add(student);
        return student;
    }

    private Application saveApplication(Student student, Tenant tenant) {
        Application application = applicationRepository.save(Application.builder()
                .applicationCode("APP-" + System.nanoTime())
                .applicationDate(LocalDate.now())
                .status(ApplicationStatus.PENDING)
                .student(student)
                .tenantId(tenant.getId())
                .build());
        applications.add(application);
        return application;
    }
}
