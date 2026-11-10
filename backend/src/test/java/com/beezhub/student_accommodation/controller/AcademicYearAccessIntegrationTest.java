package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
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
 * Academic years belong to each company (GH-494). A company admin sees and manages their own
 * company's years, can reuse a name another company uses, and has no set-current action, because
 * the current year follows the dates (GH-439). These run over HTTP with real tokens, because the
 * tenant filter and the role check are the behaviour under test.
 */
class AcademicYearAccessIntegrationTest extends AbstractIntegrationTest {

    private static final String PATH = "/api/academic-years";

    @Autowired TestRestTemplate rest;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired JwtUtil jwtUtil;
    @Autowired JdbcTemplate jdbc;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> tenantIds = new ArrayList<>();

    private Long companyId;
    private Long otherCompanyId;
    private String companyAdminToken;
    private String platformAdminToken;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        SecurityContextHolder.clearContext();

        companyId = saveCompany();
        otherCompanyId = saveCompany();
        companyAdminToken = tokenFor(UserRole.TENANT_ADMIN, companyId);
        platformAdminToken = tokenFor(UserRole.PLATFORM_ADMIN, TenantContext.PLATFORM_TENANT_ID);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        tenantIds.forEach(id -> jdbc.update("DELETE FROM academic_year WHERE tenant_id = ?", id));
        jdbc.update("DELETE FROM academic_year WHERE name LIKE 'AY-%'");
        userIds.forEach(appUserRepository::deleteById);
        tenantIds.forEach(tenantRepository::deleteById);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAll_companyAdmin_seesOwnYearsOnly() {
        String platformYear = uniqueName();
        String ownYear = uniqueName();
        String otherCompanyYear = uniqueName();
        insertYear(platformYear, TenantContext.PLATFORM_TENANT_ID, 2030);
        insertYear(ownYear, companyId, 2030);
        insertYear(otherCompanyYear, otherCompanyId, 2030);

        ResponseEntity<String> response = get(PATH, companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(ownYear, "\"totalElements\"")
                .doesNotContain(otherCompanyYear, platformYear);
    }

    @Test
    void getAll_platformAdmin_seesEveryCompanysYears() {
        String ownedByCompany = uniqueName();
        String ownedByOtherCompany = uniqueName();
        insertYear(ownedByCompany, companyId, 2030);
        insertYear(ownedByOtherCompany, otherCompanyId, 2030);

        ResponseEntity<String> response = get(PATH + "?size=500", platformAdminToken);

        assertThat(response.getBody()).contains(ownedByCompany, ownedByOtherCompany);
    }

    /** GH-439 comment: GET /current returned 404 for a company admin. */
    @Test
    void getCurrent_companyAdmin_returnsTheCompanysYearContainingToday() {
        String ownYear = uniqueName();
        insertYear(ownYear, companyId, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(6));
        insertYear(uniqueName(), companyId, LocalDate.now().plusMonths(7), LocalDate.now().plusMonths(18));

        ResponseEntity<String> response = get(PATH + "/current", companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(ownYear);
    }

    @Test
    void create_companyAdminWithNameAnotherCompanyUses_isCreatedForTheCompany() {
        String sharedName = uniqueName();
        insertYear(sharedName, otherCompanyId, 2030);

        ResponseEntity<String> response = post(PATH, yearJson(sharedName, 2031), companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(countByNameAndTenant(sharedName, companyId)).isEqualTo(1);
        assertThat(countByNameAndTenant(sharedName, otherCompanyId)).isEqualTo(1);
    }

    @Test
    void create_companyAdminReusingOwnName_isAConflictWithAPlainMessage() {
        String ownName = uniqueName();
        insertYear(ownName, companyId, 2030);

        ResponseEntity<String> response = post(PATH, yearJson(ownName, 2031), companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("All institutions");
    }

    @Test
    void create_companyAdminOverlappingOwnYear_isAConflictNamingTheYear() {
        String ownName = uniqueName();
        insertYear(ownName, companyId, 2031);

        ResponseEntity<String> response = post(PATH, yearJson(uniqueName(), 2031), companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains(ownName);
    }

    /** The current year follows the dates; the endpoint that set it for every company is gone. */
    @Test
    void setCurrent_companyAdmin_hasNoSuchAction() {
        Long ownYear = insertYear(uniqueName(), companyId, 2030);

        ResponseEntity<String> response = post(PATH + "/" + ownYear + "/set-current", "{}", companyAdminToken);

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    void update_companyAdminOnOwnYear_isSaved() {
        Long ownYear = insertYear(uniqueName(), companyId, 2030);
        String newName = uniqueName();

        ResponseEntity<String> response = rest.exchange(PATH + "/" + ownYear, HttpMethod.PUT,
                new HttpEntity<>(yearJson(newName, 2030), jsonHeaders(companyAdminToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(countByNameAndTenant(newName, companyId)).isEqualTo(1);
    }

    @Test
    void update_companyAdminOnAnotherCompanysYear_isNotFoundAndLeavesTheRow() {
        String otherName = uniqueName();
        Long otherYear = insertYear(otherName, otherCompanyId, 2030);

        ResponseEntity<String> response = rest.exchange(PATH + "/" + otherYear, HttpMethod.PUT,
                new HttpEntity<>(yearJson(uniqueName(), 2030), jsonHeaders(companyAdminToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(countByNameAndTenant(otherName, otherCompanyId)).isEqualTo(1);
    }

    @Test
    void delete_companyAdminOnAnotherCompanysYear_isNotFoundAndLeavesTheRow() {
        String otherName = uniqueName();
        Long otherYear = insertYear(otherName, otherCompanyId, 2030);

        ResponseEntity<String> response = rest.exchange(PATH + "/" + otherYear, HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders(companyAdminToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(countByNameAndTenant(otherName, otherCompanyId)).isEqualTo(1);
    }

    @Test
    void delete_companyAdminOnOwnYearWithoutApplications_isDeleted() {
        String ownName = uniqueName();
        Long ownYear = insertYear(ownName, companyId, 2030);

        ResponseEntity<String> response = rest.exchange(PATH + "/" + ownYear, HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders(companyAdminToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(countByNameAndTenant(ownName, companyId)).isZero();
    }

    private Long saveCompany() {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Company " + UUID.randomUUID())
                .isActive(true)
                .build());
        tenantIds.add(tenant.getId());
        return tenant.getId();
    }

    private String tokenFor(UserRole role, Long tenantId) {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "." + UUID.randomUUID() + "@test.com")
                .firstName("Test")
                .lastName("Admin")
                .userPassword("$2a$10$hashed")
                .userRole(role)
                .tenantId(tenantId)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        userIds.add(user.getId());
        return jwtUtil.generateToken(new AppUserDetails(user.getEmail(), "", role.name(), tenantId));
    }

    /** A year running from February to November of {@code year}. */
    private Long insertYear(String name, Long tenantId, int year) {
        return insertYear(name, tenantId, LocalDate.of(year, 2, 1), LocalDate.of(year, 11, 30));
    }

    private Long insertYear(String name, Long tenantId, LocalDate start, LocalDate end) {
        return jdbc.queryForObject("""
                INSERT INTO academic_year
                    (name, start_date, end_date, application_open_date, application_close_date, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, name, Date.valueOf(start), Date.valueOf(end),
                Date.valueOf(start.minusMonths(4)), Date.valueOf(start.minusDays(14)), tenantId);
    }

    private int countByNameAndTenant(String name, Long tenantId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM academic_year WHERE name = ? AND tenant_id = ?",
                Integer.class, name, tenantId);
        return count == null ? 0 : count;
    }

    private String uniqueName() {
        return "AY-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String yearJson(String name, int year) {
        return """
                {"name":"%s","startDate":"%d-02-01","endDate":"%d-11-30",
                 "applicationOpenDate":"%d-09-01","applicationCloseDate":"%d-01-31"}
                """.formatted(name, year, year, year - 1, year);
    }

    private ResponseEntity<String> get(String path, String token) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(jsonHeaders(token)), String.class);
    }

    private ResponseEntity<String> post(String path, String body, String token) {
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, jsonHeaders(token)), String.class);
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
