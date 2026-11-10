package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.DocumentLifecycleService;
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
import org.springframework.mock.web.MockMultipartFile;
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
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private IntakeRepository intakeRepository;
    @Autowired private DocumentLifecycleService documentLifecycleService;
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
        documentRepository.deleteAll();
        documentTypeRepository.deleteAll();
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

        // GH-495: a company takes applications only for its own years, so tenant A has a year
        // containing today with one Annual intake whose window is open.
        seedOpenYear(tenantA);

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
        student.setPhoneNumber("0123456789");
        student.setDateOfBirth(LocalDate.of(2000, 5, 14));
        student.setGender("Female");
        // GH-239: a complete profile now includes an emergency contact, and submitApplication
        // refuses without one. Every case in this class submits an application, so the fixture
        // carries a contact rather than each test setting one.
        student.setEmergencyContactName("Thandi Mokoena");
        student.setEmergencyContactPhone("+27 82 123 4567");
        student = studentRepository.save(student);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of()));
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
    }

    /** A year of the company containing today, with one Annual intake whose window is open. */
    private Intake seedOpenYear(Tenant company) {
        LocalDate today = LocalDate.now(com.beezhub.student_accommodation.config.ClockConfig.CALENDAR_ZONE);
        AcademicYear year = academicYearRepository.save(AcademicYear.builder()
                .name("Y" + System.nanoTime() % 1_000_000_000L)
                .startDate(today.minusMonths(1))
                .endDate(today.plusMonths(10))
                .applicationOpenDate(today.minusMonths(2))
                .applicationCloseDate(today.plusMonths(1))
                .tenantId(company.getId())
                .build());
        return intakeRepository.save(Intake.builder()
                .tenantId(company.getId())
                .academicYear(year)
                .intakeType(IntakeType.ANNUAL)
                .name("Annual")
                .stayStartDate(year.getStartDate())
                .stayEndDate(year.getEndDate())
                .applicationOpenDate(year.getApplicationOpenDate())
                .applicationCloseDate(year.getApplicationCloseDate())
                .build());
    }

    /** The one application a single-company submission creates. */
    private ApplicationResponse submitOne(ApplicationRequest request) {
        List<ApplicationResponse> created = applicationService.submitApplication(request);
        assertThat(created).hasSize(1);
        return created.get(0);
    }

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll();
        documentRepository.deleteAll();
        documentTypeRepository.deleteAll();
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

        ApplicationResponse response = submitOne(request);

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
    @DisplayName("GH-495: buildings at two companies create one application each, with only their own buildings")
    void submitApplication_twoCompanies_createsOneApplicationPerCompany() {
        Tenant tenantB = tenantRepository.save(Tenant.builder()
                .name("Property Co B " + System.currentTimeMillis())
                .isActive(true)
                .build());
        createdTenantIds.add(tenantB.getId());
        seedOpenYear(tenantB);

        Building buildingB = new Building();
        buildingB.setName("Beta House " + System.currentTimeMillis());
        buildingB.setTenantId(tenantB.getId());
        buildingRepository.save(buildingB);
        createdBuildingIds.add(buildingB.getId());

        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingB.getId(), buildingA.getId()));

        List<ApplicationResponse> created = applicationService.submitApplication(request);

        assertThat(created).extracting(ApplicationResponse::getTenantId)
                .containsExactly(tenantB.getId(), tenantA.getId());
        assertThat(created.get(0).getPreferredBuildings()).singleElement().satisfies(p -> {
            assertThat(p.getBuildingId()).isEqualTo(buildingB.getId());
            assertThat(p.getPreferenceOrder()).isEqualTo(1);
        });
        assertThat(created.get(1).getPreferredBuildings()).singleElement().satisfies(p -> {
            assertThat(p.getBuildingId()).isEqualTo(buildingA.getId());
            assertThat(p.getPreferenceOrder()).isEqualTo(1);
        });
        assertThat(created).allSatisfy(a -> assertThat(a.getIntakeName()).isEqualTo("Annual"));
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
        seedOpenYear(tenantB);

        Building buildingB = new Building();
        buildingB.setName("Beta House 2 " + System.currentTimeMillis());
        buildingB.setTenantId(tenantB.getId());
        buildingRepository.save(buildingB);
        createdBuildingIds.add(buildingB.getId());

        // Submit to tenant B — should succeed even though student has pending app for tenant A
        ApplicationRequest requestB = new ApplicationRequest();
        requestB.setStudentId(student.getId());
        requestB.setPreferredBuildingIds(List.of(buildingB.getId()));
        ApplicationResponse responseB = submitOne(requestB);

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
        ApplicationResponse submitted = submitOne(request);

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
        ApplicationResponse submitted = submitOne(request);

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

    // ── Pre-submission document completeness check ──────────────────────────────

    @Test
    @DisplayName("submitApplication throws when required document type is missing")
    void submitApplication_rejectsMissingRequiredDocument() {
        DocumentType requiredType = new DocumentType();
        requiredType.setTypeName("National ID_" + System.currentTimeMillis());
        requiredType.setIsRequired(true);
        requiredType.setTenantId(tenantA.getId());
        documentTypeRepository.save(requiredType);

        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));
        request.setCourseName("CS");
        request.setInstitutionName("Test U");
        request.setRequiresAccessibility(false);

        assertThatThrownBy(() -> applicationService.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required documents");
    }

    @Test
    @DisplayName("submitApplication succeeds after required document is uploaded")
    void submitApplication_succeedsAfterRequiredDocumentUploaded() {
        DocumentType requiredType = new DocumentType();
        requiredType.setTypeName("Proof of Enrollment_" + System.currentTimeMillis());
        requiredType.setIsRequired(true);
        requiredType.setTenantId(tenantA.getId());
        requiredType = documentTypeRepository.save(requiredType);

        // Upload the required document for the student
        MockMultipartFile file = new MockMultipartFile(
                "file", "enrollment.pdf", "application/pdf", pdfBytes("enrollment proof content"));
        documentLifecycleService.uploadSingleDocument(file, student.getAppUser().getEmail(), requiredType.getId());

        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));
        request.setCourseName("CS");
        request.setInstitutionName("Test U");
        request.setRequiresAccessibility(false);

        ApplicationResponse response = submitOne(request);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationCode()).isNotBlank();
    }

    // ── Task 7.7: platform-type document satisfies tenant shadow (name-based match) ──

    @Test
    @DisplayName("Document uploaded against platform type satisfies tenant shadow of same name")
    void submitApplication_platformDocumentSatisfiesTenantShadow() {
        long ts = System.currentTimeMillis();

        // Platform type "Passport_<ts>" — required at platform level
        DocumentType platformType = new DocumentType();
        platformType.setTypeName("Passport_" + ts);
        platformType.setIsRequired(true);
        platformType.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        platformType = documentTypeRepository.save(platformType);

        // Tenant A shadow of the same type name — also required (shadow wins by name)
        DocumentType tenantShadow = new DocumentType();
        tenantShadow.setTypeName("Passport_" + ts);  // same name
        tenantShadow.setIsRequired(true);
        tenantShadow.setTenantId(tenantA.getId());
        documentTypeRepository.save(tenantShadow);

        // Student uploads document using the PLATFORM type id
        MockMultipartFile file = new MockMultipartFile(
                "file", "passport.pdf", "application/pdf", pdfBytes("passport content"));
        documentLifecycleService.uploadSingleDocument(file, student.getAppUser().getEmail(), platformType.getId());

        // Application to Tenant A — name-based check must find the document via the platform type name
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));

        ApplicationResponse response = submitOne(request);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationCode()).isNotBlank();
    }

    // ── Task 7.8: student not blocked by another tenant's custom required type ──

    @Test
    @DisplayName("Student applying to Tenant A is not blocked by Tenant B's custom required type")
    void submitApplication_notBlockedByOtherTenantRequiredType() {
        Tenant tenantB = tenantRepository.save(Tenant.builder()
                .name("Property Co B7_" + System.currentTimeMillis())
                .isActive(true)
                .build());
        createdTenantIds.add(tenantB.getId());

        // Tenant B has a custom required type
        DocumentType tenantBType = new DocumentType();
        tenantBType.setTypeName("TenantB_Lease_" + System.currentTimeMillis());
        tenantBType.setIsRequired(true);
        tenantBType.setTenantId(tenantB.getId());
        documentTypeRepository.save(tenantBType);

        // No documents uploaded for the student — Tenant A has no required types
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));

        // Should succeed: Tenant A has no required types, so Tenant B's types are irrelevant
        ApplicationResponse response = submitOne(request);

        assertThat(response).isNotNull();
        assertThat(response.getApplicationCode()).isNotBlank();
    }

    // ── Task 7.9: student blocked by tenant's own custom required type not uploaded ──

    @Test
    @DisplayName("Student applying to Tenant A is blocked by Tenant A custom required type not uploaded")
    void submitApplication_blockedByTenantCustomRequiredType() {
        // Tenant A has a custom required type that only applies to Tenant A
        DocumentType tenantAOnlyType = new DocumentType();
        tenantAOnlyType.setTypeName("TenantA_Lease_" + System.currentTimeMillis());
        tenantAOnlyType.setIsRequired(true);
        tenantAOnlyType.setTenantId(tenantA.getId());
        documentTypeRepository.save(tenantAOnlyType);

        // Student has NOT uploaded the document
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setPreferredBuildingIds(List.of(buildingA.getId()));

        assertThatThrownBy(() -> applicationService.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required documents");
    }

    /**
     * A minimal but genuine PDF. Uploads are validated against the file bytes now, so a fixture
     * whose content is prose is correctly rejected (GH-144).
     */
    private static byte[] pdfBytes(String marker) {
        return ("%PDF-1.7\n% " + marker + "\n%%EOF\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
