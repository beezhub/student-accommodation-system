package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.config.ClockConfig;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Intakes are company data (GH-494): company A cannot read or change company B's intakes, and a
 * company admin can build their own calendar from a pattern. Runs over HTTP with real tokens, so
 * the tenant filter, the role check and {@code TenantOwnershipGuard} are all in play.
 */
class IntakeAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired JwtUtil jwtUtil;
    @Autowired JdbcTemplate jdbc;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> tenantIds = new ArrayList<>();

    private Long companyA;
    private Long companyB;
    private String adminA;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        SecurityContextHolder.clearContext();
        companyA = saveCompany();
        companyB = saveCompany();
        adminA = tokenFor(companyA);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        tenantIds.forEach(id -> jdbc.update("DELETE FROM academic_year WHERE tenant_id = ?", id));
        userIds.forEach(appUserRepository::deleteById);
        tenantIds.forEach(tenantRepository::deleteById);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_companyAdmin_doesNotSeeAnotherCompanysIntakes() {
        Long yearB = insertYear(companyB, 2030);
        String secretName = "Hidden " + UUID.randomUUID().toString().substring(0, 6);
        insertIntake(yearB, companyB, secretName, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 11, 30));

        ResponseEntity<String> response = exchange(HttpMethod.GET, "/api/academic-years?size=500", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain(secretName);
    }

    @Test
    void update_anotherCompanysIntake_isNotFoundAndUnchanged() {
        Long yearB = insertYear(companyB, 2030);
        Long intakeB = insertIntake(yearB, companyB, "Annual", LocalDate.of(2030, 2, 1), LocalDate.of(2030, 11, 30));

        ResponseEntity<String> response = exchange(HttpMethod.PUT, "/api/intakes/" + intakeB, """
                {"intakeType":"ANNUAL","name":"Taken over","stayStartDate":"2030-02-01","stayEndDate":"2030-11-30"}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(intakeName(intakeB)).isEqualTo("Annual");
    }

    @Test
    void deleteAndClose_anotherCompanysIntake_areNotFoundAndLeaveTheRow() {
        Long yearB = insertYear(companyB, 2030);
        Long intakeB = insertIntake(yearB, companyB, "Annual", LocalDate.of(2030, 2, 1), LocalDate.of(2030, 11, 30));

        assertThat(exchange(HttpMethod.DELETE, "/api/intakes/" + intakeB, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange(HttpMethod.POST, "/api/intakes/" + intakeB + "/close-applications", null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(intakeName(intakeB)).isEqualTo("Annual");
    }

    @Test
    void addIntakeOrCopy_anotherCompanysYear_isNotFound() {
        Long yearB = insertYear(companyB, 2030);

        assertThat(exchange(HttpMethod.POST, "/api/academic-years/" + yearB + "/intakes", """
                {"intakeType":"SEMESTER","name":"Semester 9","stayStartDate":"2030-03-01","stayEndDate":"2030-05-01"}
                """).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange(HttpMethod.POST, "/api/academic-years/" + yearB + "/copy", "{}").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createFromPattern_companyAdmin_createsTheYearWithIntakesAndWindows() {
        ResponseEntity<String> response = exchange(HttpMethod.POST, "/api/academic-years/from-pattern", """
                {"name":"UX 2031","startDate":"2031-02-01","endDate":"2031-11-30",
                 "intakeTypes":["ANNUAL","SEMESTER"]}
                """);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Annual", "Semester 1", "Semester 2");
        Integer intakes = jdbc.queryForObject("""
                SELECT COUNT(*) FROM intake i JOIN academic_year y ON y.id = i.academic_year_id
                WHERE y.tenant_id = ? AND y.name = 'UX 2031' AND i.tenant_id = ?
                """, Integer.class, companyA, companyA);
        assertThat(intakes).isEqualTo(3);
        Date annualOpen = jdbc.queryForObject("""
                SELECT i.application_open_date FROM intake i JOIN academic_year y ON y.id = i.academic_year_id
                WHERE y.tenant_id = ? AND i.name = 'Annual'
                """, Date.class, companyA);
        assertThat(annualOpen.toLocalDate()).isEqualTo(LocalDate.of(2031, 2, 1).minusDays(120));
    }

    @Test
    void copy_ownYear_movesEveryDateOnByAYear() {
        Long yearA = insertYear(companyA, 2030);
        insertIntake(yearA, companyA, "Annual", LocalDate.of(2030, 2, 1), LocalDate.of(2030, 11, 30));

        ResponseEntity<String> response = exchange(HttpMethod.POST, "/api/academic-years/" + yearA + "/copy", "{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Date copiedStart = jdbc.queryForObject("""
                SELECT i.stay_start_date FROM intake i JOIN academic_year y ON y.id = i.academic_year_id
                WHERE y.tenant_id = ? AND y.name = '2031'
                """, Date.class, companyA);
        assertThat(copiedStart.toLocalDate()).isEqualTo(LocalDate.of(2031, 2, 1));
    }

    @Test
    void closeApplicationsNow_ownIntake_setsTheCloseDateToToday() {
        LocalDate today = LocalDate.now(ClockConfig.CALENDAR_ZONE);
        Long yearA = jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, application_open_date, application_close_date, tenant_id)
                VALUES ('Now', ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, Date.valueOf(today.plusMonths(2)), Date.valueOf(today.plusMonths(12)),
                Date.valueOf(today.minusDays(10)), Date.valueOf(today.plusMonths(1)), companyA);
        Long intakeA = jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, 'ANNUAL', 'Annual', ?, ?, ?, ?) RETURNING id
                """, Long.class, companyA, yearA, Date.valueOf(today.plusMonths(2)), Date.valueOf(today.plusMonths(12)),
                Date.valueOf(today.minusDays(10)), Date.valueOf(today.plusMonths(1)));

        ResponseEntity<String> response = exchange(HttpMethod.POST, "/api/intakes/" + intakeA + "/close-applications", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Date closeDate = jdbc.queryForObject("SELECT application_close_date FROM intake WHERE id = ?", Date.class, intakeA);
        assertThat(closeDate.toLocalDate()).isEqualTo(today);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────

    private Long insertYear(Long tenantId, int year) {
        return jdbc.queryForObject("""
                INSERT INTO academic_year (name, start_date, end_date, application_open_date, application_close_date, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, String.valueOf(year), Date.valueOf(LocalDate.of(year, 2, 1)),
                Date.valueOf(LocalDate.of(year, 11, 30)), Date.valueOf(LocalDate.of(year - 1, 10, 1)),
                Date.valueOf(LocalDate.of(year, 1, 18)), tenantId);
    }

    private Long insertIntake(Long yearId, Long tenantId, String name, LocalDate start, LocalDate end) {
        return jdbc.queryForObject("""
                INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                                    application_open_date, application_close_date)
                VALUES (?, ?, 'ANNUAL', ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, tenantId, yearId, name, Date.valueOf(start), Date.valueOf(end),
                Date.valueOf(start.minusDays(120)), Date.valueOf(start.minusDays(14)));
    }

    private String intakeName(Long id) {
        return jdbc.queryForObject("SELECT name FROM intake WHERE id = ?", String.class, id);
    }

    private Long saveCompany() {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Company " + UUID.randomUUID())
                .isActive(true)
                .build());
        tenantIds.add(tenant.getId());
        return tenant.getId();
    }

    private String tokenFor(Long tenantId) {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("tenant_admin." + UUID.randomUUID() + "@test.com")
                .firstName("Test")
                .lastName("Admin")
                .userPassword("$2a$10$hashed")
                .userRole(UserRole.TENANT_ADMIN)
                .tenantId(tenantId)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        userIds.add(user.getId());
        return jwtUtil.generateToken(new AppUserDetails(user.getEmail(), "", UserRole.TENANT_ADMIN.name(), tenantId));
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminA);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
