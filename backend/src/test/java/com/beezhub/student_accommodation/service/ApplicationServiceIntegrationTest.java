package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ApplicationService applicationService;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TestRestTemplate rest;

    // Created per-test — cleaned up in tearDown
    private final List<Long> createdTenantIds = new ArrayList<>();
    private final List<Long> createdBuildingIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    private Student student;
    private Tenant tenantA;
    private Building buildingA;

    @BeforeEach
    void setUp() {
        applicationRepository.deleteAll();
        academicYearRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        buildingRepository.deleteAll();
        createdTenantIds.clear();
        createdBuildingIds.clear();
        createdUserIds.clear();

        // Seed tenant A
        tenantA = tenantRepository.save(Tenant.builder()
                .name("Property Co A " + System.currentTimeMillis())
                .isActive(true)
                .build());
        createdTenantIds.add(tenantA.getId());

        // Seed a building belonging to tenant A
        Building b = new Building();
        b.setName("Alpha House " + System.currentTimeMillis());
        b.setTenantId(tenantA.getId());
        buildingA = buildingRepository.save(b);
        createdBuildingIds.add(buildingA.getId());

        // Seed a current AcademicYear — platform-level so no filter applies during service calls
        AcademicYear year = AcademicYear.builder()
                .name("2026 Test " + System.currentTimeMillis() % 10000)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .applicationOpenDate(LocalDate.of(2026, 1, 1))
                .applicationCloseDate(LocalDate.of(2026, 12, 31))
                .isCurrent(true)
                .tenantId(TenantContext.PLATFORM_TENANT_ID)
                .build();
        academicYearRepository.save(year);

        // Seed a platform-level student
        AppUser user = new AppUser();
        user.setEmail("app-test-" + System.currentTimeMillis() + "@example.com");
        user.setUserPassword("secret");
        user.setFirstName("App");
        user.setLastName("Tester");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        user.setEmailVerified(true);
        appUserRepository.save(user);
        createdUserIds.add(user.getId());

        student = new Student();
        student.setAppUser(user);
        student.setStudentNumber("S-" + System.currentTimeMillis());
        student = studentRepository.save(student);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll();
        studentRepository.deleteAll();
        appUserRepository.deleteAll();
        buildingRepository.deleteAll();
        academicYearRepository.deleteAll();
        createdTenantIds.forEach(id -> tenantRepository.findById(id).ifPresent(tenantRepository::delete));
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ── Application submission ──────────────────────────────────────────────────

    @Test
    @DisplayName("Application is persisted with tenant_id derived from building preference")
    void submitApplication_setsTenantIdFromBuilding() {
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setSpecialRequirements("Near elevator");
        request.setInstitutionName("Test Institute");
        request.setCourseName("CS");
        request.setRequiresAccessibility(false);
        request.setPreferredBuildingIds(List.of(buildingA.getId()));

        ApplicationResponse response = applicationService.submitApplication(request);

        assertThat(response.getSpecialRequirements()).isEqualTo("Near elevator");
        var saved = applicationRepository.findAll().stream()
                .filter(a -> a.getApplicationCode().equals(response.getApplicationCode()))
                .findFirst().orElseThrow();
        assertThat(saved.getTenantId()).isEqualTo(tenantA.getId());
    }

    @Test
    @DisplayName("Submission with no building preference throws IllegalArgumentException")
    void submitApplication_rejectsNoBuildingPreference() {
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        // No building preference provided

        assertThatThrownBy(() -> applicationService.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("building preference");
    }

    @Test
    @DisplayName("Submission with buildings from two different tenants throws IllegalArgumentException")
    void submitApplication_rejectsCrossTenantPreferences() {
        Tenant tenantB = tenantRepository.save(Tenant.builder()
                .name("Property Co B " + System.currentTimeMillis())
                .isActive(true)
                .build());
        createdTenantIds.add(tenantB.getId());

        Building buildingB = new Building();
        buildingB.setName("Beta House " + System.currentTimeMillis());
        buildingB.setTenantId(tenantB.getId());
        buildingRepository.save(buildingB);
        createdBuildingIds.add(buildingB.getId());

        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId(), buildingB.getId()));

        assertThatThrownBy(() -> applicationService.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same property company");
    }

    @Test
    @DisplayName("Student can have one pending application per tenant (per-tenant duplicate check)")
    void submitApplication_allowsMultiplePendingApplicationsToDifferentTenants() {
        // Submit to tenant A
        ApplicationRequest requestA = new ApplicationRequest();
        requestA.setStudentId(student.getId());
        requestA.setPreferredBuildingIds(List.of(buildingA.getId()));
        applicationService.submitApplication(requestA);

        // Seed tenant B
        Tenant tenantB = tenantRepository.save(Tenant.builder()
                .name("Property Co B2 " + System.currentTimeMillis())
                .isActive(true)
                .build());
        createdTenantIds.add(tenantB.getId());

        Building buildingB = new Building();
        buildingB.setName("Beta House 2 " + System.currentTimeMillis());
        buildingB.setTenantId(tenantB.getId());
        buildingRepository.save(buildingB);
        createdBuildingIds.add(buildingB.getId());

        // Submit to tenant B — should succeed even though student has pending app for tenant A
        ApplicationRequest requestB = new ApplicationRequest();
        requestB.setStudentId(student.getId());
        requestB.setPreferredBuildingIds(List.of(buildingB.getId()));
        ApplicationResponse responseB = applicationService.submitApplication(requestB);

        assertThat(responseB).isNotNull();
        assertThat(responseB.getApplicationCode()).isNotNull();
    }

    @Test
    @DisplayName("Student cannot submit a second application to the same tenant")
    void submitApplication_rejectsDuplicateForSameTenant() {
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));
        applicationService.submitApplication(request);

        // Second submission to same tenant should be rejected
        assertThatThrownBy(() -> applicationService.submitApplication(request))
                .isInstanceOf(DuplicateApplicationException.class);
    }

    // ── Task 4.1: TENANT_ADMIN sees only their own tenant's applications ────────

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/applications returns only their tenant's applications")
    void tenantAdmin_seesOnlyOwnTenantApplications() {
        // Submit application to tenant A
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));
        ApplicationResponse submitted = applicationService.submitApplication(request);

        // Seed tenant B with a building but no application
        Tenant tenantB = tenantRepository.save(Tenant.builder()
                .name("Property Co B3 " + System.currentTimeMillis())
                .isActive(true)
                .build());
        createdTenantIds.add(tenantB.getId());

        // Create a TENANT_ADMIN for tenant A
        AppUser adminA = appUserRepository.save(AppUser.builder()
                .email("admin.a." + System.currentTimeMillis() + "@test.com")
                .firstName("Admin").lastName("A")
                .userPassword("$2a$10$hashed")
                .userRole(UserRole.TENANT_ADMIN)
                .tenantId(tenantA.getId())
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        createdUserIds.add(adminA.getId());

        String tokenA = jwtUtil.generateToken(
                new AppUserDetails(adminA.getEmail(), "", "TENANT_ADMIN", tenantA.getId()));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/applications", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // The response must contain the submitted application code
        assertThat(response.getBody()).contains(submitted.getApplicationCode());
    }

    // ── Task 4.2: TENANT_ADMIN gets 404 for another tenant's application ───────

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/applications/{id} for other tenant returns 404")
    void tenantAdmin_cannotAccessOtherTenantApplicationById() {
        // Submit an application to tenant A
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));
        ApplicationResponse submitted = applicationService.submitApplication(request);

        Long appId = applicationRepository.findAll().stream()
                .filter(a -> a.getApplicationCode().equals(submitted.getApplicationCode()))
                .findFirst().orElseThrow().getId();

        // Create tenant B admin
        Tenant tenantB = tenantRepository.save(Tenant.builder()
                .name("Property Co B4 " + System.currentTimeMillis())
                .isActive(true)
                .build());
        createdTenantIds.add(tenantB.getId());

        AppUser adminB = appUserRepository.save(AppUser.builder()
                .email("admin.b." + System.currentTimeMillis() + "@test.com")
                .firstName("Admin").lastName("B")
                .userPassword("$2a$10$hashed")
                .userRole(UserRole.TENANT_ADMIN)
                .tenantId(tenantB.getId())
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        createdUserIds.add(adminB.getId());

        String tokenB = jwtUtil.generateToken(
                new AppUserDetails(adminB.getEmail(), "", "TENANT_ADMIN", tenantB.getId()));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenB);
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/applications/" + appId, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        // Tenant B admin must not see tenant A's application
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
