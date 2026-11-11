package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.config.ClockConfig;
import com.beezhub.student_accommodation.exceptions.ApplicationWindowClosedException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GH-495 against PostgreSQL: one form across companies, one application per company, all or
 * nothing, and the building filter of the application form.
 */
class ApplicationSubmissionIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ApplicationService applicationService;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TestRestTemplate rest;

    private final List<Long> tenants = new ArrayList<>();
    private final List<Long> users = new ArrayList<>();
    private final List<Long> institutions = new ArrayList<>();
    private final LocalDate today = LocalDate.now(ClockConfig.CALENDAR_ZONE);
    private Student student;
    private Long universityX;
    private Long companyA;
    private Long companyB;
    private Long residence1;
    private Long residence2;
    private Long residence3;

    @BeforeEach
    void setUp() {
        universityX = institution("University X");
        companyA = tenant("Company A");
        companyB = tenant("Company B");
        residence1 = building(companyA, "Residence 1");
        residence2 = building(companyB, "Residence 2");
        residence3 = building(companyA, "Residence 3");
        openYear(companyA, null, today.minusMonths(1), today.plusMonths(10));
        openYear(companyB, null, today.minusMonths(1), today.plusMonths(10));
        student = student(universityX);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student.getAppUser().getEmail(), null, List.of()));
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        for (Long tenant : tenants) {
            jdbc.update("DELETE FROM application_building_preference WHERE application_id IN "
                    + "(SELECT id FROM application WHERE tenant_id = ?)", tenant);
            jdbc.update("DELETE FROM application WHERE tenant_id = ?", tenant);
            jdbc.update("DELETE FROM building WHERE tenant_id = ?", tenant);
            jdbc.update("DELETE FROM academic_year WHERE tenant_id = ?", tenant);
            jdbc.update("DELETE FROM document_type WHERE tenant_id = ?", tenant);
        }
        if (student != null) {
            jdbc.update("DELETE FROM student WHERE id = ?", student.getId());
        }
        users.forEach(id -> jdbc.update("DELETE FROM app_user WHERE id = ?", id));
        tenants.forEach(id -> jdbc.update("DELETE FROM tenant WHERE id = ?", id));
        institutions.forEach(id -> jdbc.update("DELETE FROM institution WHERE id = ?", id));
    }

    // ── One application per company ───────────────────────────────────────────

    @Test
    void submit_buildingsAtTwoCompanies_createsTwoApplicationsEachWithItsOwnBuildingsInOrder() {
        List<ApplicationResponse> created = applicationService.submitApplication(request(residence1, residence2, residence3));

        assertThat(created).extracting(ApplicationResponse::getTenantId).containsExactly(companyA, companyB);
        assertThat(preferences(created.get(0).getId())).containsExactly(residence1, residence3);
        assertThat(preferences(created.get(1).getId())).containsExactly(residence2);
        assertThat(jdbc.queryForObject("SELECT institution_id FROM application WHERE id = ?", Long.class,
                created.get(0).getId())).isEqualTo(universityX);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application WHERE intake_id IS NULL AND tenant_id IN (?, ?)",
                Integer.class, companyA, companyB)).isZero();
    }

    @Test
    void submit_twoCompanies_neitherCompanyAdminSeesTheOthersApplication() {
        List<ApplicationResponse> created = applicationService.submitApplication(request(residence1, residence2));
        String codeA = created.get(0).getApplicationCode();
        String codeB = created.get(1).getApplicationCode();

        String seenByA = adminList(companyA);
        String seenByB = adminList(companyB);

        assertThat(seenByA).contains(codeA).doesNotContain(codeB);
        assertThat(seenByB).contains(codeB).doesNotContain(codeA);
    }

    /** An older client sends no intakes; the company's one open intake is used. */
    @Test
    void submit_missingIntakeIdWithOneOpenIntake_submits() {
        ApplicationRequest request = request(residence1);
        request.setIntakeIds(null);

        assertThat(applicationService.submitApplication(request)).singleElement()
                .satisfies(a -> assertThat(a.getIntakeName()).isEqualTo("Annual"));
    }

    // ── All or nothing ─────────────────────────────────────────────────────────

    @Test
    void submit_closedWindowAtOneCompany_savesNothingAndNamesIt() {
        jdbc.update("UPDATE intake SET application_open_date = ?, application_close_date = ? WHERE tenant_id = ?",
                Date.valueOf(today.plusDays(10)), Date.valueOf(today.plusDays(40)), companyB);

        assertThatThrownBy(() -> applicationService.submitApplication(request(residence1, residence2)))
                .isInstanceOf(ApplicationWindowClosedException.class)
                .hasMessageContaining("Company B");
        assertThat(applicationCount()).isZero();
    }

    @Test
    void submit_missingRequiredDocumentAtOneCompany_savesNothingAndNamesIt() {
        jdbc.update("INSERT INTO document_type (type_name, is_required, tenant_id) VALUES (?, true, ?)",
                "Proof of address " + UUID.randomUUID().toString().substring(0, 6), companyB);

        assertThatThrownBy(() -> applicationService.submitApplication(request(residence1, residence2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company B");
        assertThat(applicationCount()).isZero();
    }

    @Test
    void submit_activeApplicationForTheYearAtOneCompany_savesNothingAndNamesIt() {
        applicationService.submitApplication(request(residence2));

        assertThatThrownBy(() -> applicationService.submitApplication(request(residence1, residence2)))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("Company B");
        assertThat(applicationCount()).isEqualTo(1);
    }

    // ── One active application per company per year ────────────────────────────

    @Test
    void submit_sameCompanyDifferentYear_isAllowed() {
        applicationService.submitApplication(request(residence1));
        Long nextYearIntake = openYear(companyA, null, today.plusMonths(11), today.plusMonths(22));
        ApplicationRequest nextYear = request(residence3);
        nextYear.setIntakeIds(Map.of(companyA, nextYearIntake));

        assertThat(applicationService.submitApplication(nextYear)).singleElement()
                .satisfies(a -> assertThat(a.getIntakeId()).isEqualTo(nextYearIntake));
        assertThat(applicationCount()).isEqualTo(2);
    }

    @Test
    void submit_sameCompanySameYear_isRefusedNamingTheYear() {
        applicationService.submitApplication(request(residence1));

        assertThatThrownBy(() -> applicationService.submitApplication(request(residence3)))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageStartingWith("You already applied to Company A");
    }

    /**
     * Two submits for the same company and year at once can both pass the pre-check; the unique
     * index refuses one of them, and the student sees the duplicate message rather than a 500.
     */
    @Test
    void submit_concurrentForTheSameCompanyAndYear_onlyOneIsSaved() throws Exception {
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        java.util.concurrent.Callable<Throwable> submit = () -> {
            SecurityContextHolder.getContext().setAuthentication(auth);
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            start.await();
            try {
                applicationService.submitApplication(request(residence1));
                return null;
            } catch (Throwable t) {
                return t;
            } finally {
                SecurityContextHolder.clearContext();
                TenantContext.clear();
            }
        };
        var first = pool.submit(submit);
        var second = pool.submit(submit);
        start.countDown();
        List<Throwable> outcomes = new ArrayList<>();
        outcomes.add(first.get(30, java.util.concurrent.TimeUnit.SECONDS));
        outcomes.add(second.get(30, java.util.concurrent.TimeUnit.SECONDS));
        pool.shutdown();

        assertThat(outcomes).filteredOn(java.util.Objects::isNull).hasSize(1);
        assertThat(outcomes).filteredOn(java.util.Objects::nonNull).singleElement()
                .isInstanceOf(DuplicateApplicationException.class);
        assertThat(applicationCount()).isEqualTo(1);
    }

    // ── The intake ─────────────────────────────────────────────────────────────

    @Test
    void submit_intakeOfAnotherCompany_isRefused() {
        ApplicationRequest request = request(residence1);
        request.setIntakeIds(Map.of(companyA, intakeOf(companyB)));

        assertThatThrownBy(() -> applicationService.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company A");
        assertThat(applicationCount()).isZero();
    }

    @Test
    void submit_intakeOfAnotherInstitution_isRefused() {
        Long collegeY = institution("College Y");
        Long collegeIntake = openYear(companyA, collegeY, today.plusMonths(11), today.plusMonths(20));
        ApplicationRequest request = request(residence1);
        request.setIntakeIds(Map.of(companyA, collegeIntake));

        assertThatThrownBy(() -> applicationService.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("another institution");
    }

    // ── Building filter ────────────────────────────────────────────────────────

    @Test
    void applyBuildings_onlyThoseThatCanTakeTheStudentsInstitution() {
        Long collegeY = institution("College Y");
        Long companyC = tenant("Company C");
        Long noYearForX = building(companyC, "Residence 4");
        openYear(companyC, collegeY, today.minusMonths(1), today.plusMonths(10));
        Long listsOnlyCollege = building(companyA, "Residence 5");
        jdbc.update("INSERT INTO building_institution (building_id, institution_id) VALUES (?, ?)", listsOnlyCollege, collegeY);
        Long listsUniversity = building(companyB, "Residence 6");
        jdbc.update("INSERT INTO building_institution (building_id, institution_id) VALUES (?, ?)", listsUniversity, universityX);

        String body = asStudent("/api/apply/buildings").getBody();

        assertThat(body).contains("Residence 1", "Residence 6")
                .doesNotContain("Residence 4", "Residence 5");
    }

    @Test
    void applyIntakes_linksAnExistingApplicationInsteadOfOfferingItsYear() {
        applicationService.submitApplication(request(residence1));

        ResponseEntity<String> response = asStudent("/api/apply/buildings/" + residence3 + "/intakes");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"openIntakes\":[]").contains("\"existingApplications\":[{");
    }

    // ── Fixtures ───────────────────────────────────────────────────────────────

    private ApplicationRequest request(Long... buildings) {
        ApplicationRequest request = new ApplicationRequest();
        request.setStudentId(student.getId());
        request.setCourseName("CS");
        request.setPreferredBuildingIds(List.of(buildings));
        return request;
    }

    private int applicationCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM application WHERE student_id = ?", Integer.class, student.getId());
    }

    private List<Long> preferences(Long applicationId) {
        return jdbc.queryForList("SELECT building_id FROM application_building_preference WHERE application_id = ? "
                + "ORDER BY preference_order", Long.class, applicationId);
    }

    private Long intakeOf(Long company) {
        return jdbc.queryForObject("SELECT MIN(id) FROM intake WHERE tenant_id = ?", Long.class, company);
    }

    private String adminList(Long company) {
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("admin." + UUID.randomUUID() + "@test.com").firstName("Admin").lastName("X")
                .userPassword("$2a$10$hashed").userRole(UserRole.TENANT_ADMIN).tenantId(company)
                .emailVerified(true).mustChangePassword(false).build());
        users.add(admin.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtUtil.generateToken(new AppUserDetails(admin.getEmail(), "", "TENANT_ADMIN", company)));
        ResponseEntity<String> response = rest.exchange("/api/admin/applications", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<String> asStudent(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtUtil.generateToken(new AppUserDetails(student.getAppUser().getEmail(), "", "STUDENT",
                TenantContext.PLATFORM_TENANT_ID)));
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private Long tenant(String name) {
        Long id = jdbc.queryForObject("INSERT INTO tenant (name, is_active) VALUES (?, true) RETURNING id", Long.class,
                name + " " + UUID.randomUUID().toString().substring(0, 8));
        tenants.add(id);
        return id;
    }

    private Long institution(String name) {
        Institution institution = new Institution();
        institution.setName(name + " " + UUID.randomUUID().toString().substring(0, 8));
        Long id = institutionRepository.save(institution).getId();
        institutions.add(id);
        return id;
    }

    private Long building(Long company, String name) {
        return jdbc.queryForObject("INSERT INTO building (name, tenant_id) VALUES (?, ?) RETURNING id", Long.class,
                name + " " + UUID.randomUUID().toString().substring(0, 8), company);
    }

    /** A year of the company with one Annual intake whose window is open today; returns the intake. */
    private Long openYear(Long company, Long institution, LocalDate start, LocalDate end) {
        Long year = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, tenant_id, institution_id)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """, Long.class, "Y" + UUID.randomUUID().toString().substring(0, 8), Date.valueOf(start),
                Date.valueOf(end), company, institution);
        return jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, 'ANNUAL', 'Annual', ?, ?, ?, ?) RETURNING id
                """, Long.class, company, year, Date.valueOf(start), Date.valueOf(end),
                Date.valueOf(today.minusDays(5)), Date.valueOf(today.plusDays(30)));
    }

    private Student student(Long institutionId) {
        AppUser user = new AppUser();
        user.setEmail("apply-" + UUID.randomUUID() + "@example.com");
        user.setUserPassword("secret");
        user.setFirstName("Apply");
        user.setLastName("Tester");
        user.setUserRole(UserRole.STUDENT);
        user.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        user.setEmailVerified(true);
        appUserRepository.save(user);
        users.add(user.getId());

        Student s = new Student();
        s.setAppUser(user);
        s.setStudentNumber("S-" + UUID.randomUUID().toString().substring(0, 8));
        s.setPhoneNumber("0123456789");
        s.setDateOfBirth(LocalDate.of(2000, 5, 14));
        s.setGender("Female");
        s.setEmergencyContactName("Thandi Mokoena");
        s.setEmergencyContactPhone("+27 82 123 4567");
        s.setInstitution(institutionRepository.findById(institutionId).orElseThrow());
        return studentRepository.save(s);
    }
}
