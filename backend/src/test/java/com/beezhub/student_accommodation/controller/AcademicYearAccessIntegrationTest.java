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
 * Academic years are one platform-wide calendar (GH-439). Applications always take the single
 * current year and its window, whichever company they go to, so a company admin reads the calendar
 * and only platform roles change it. These run over HTTP with real tokens, because the tenant
 * filter and the role check are the behaviour under test.
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
    private String platformStaffToken;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        SecurityContextHolder.clearContext();
        jdbc.update("UPDATE academic_year SET is_current = FALSE WHERE is_current = TRUE");

        companyId = saveCompany();
        otherCompanyId = saveCompany();
        companyAdminToken = tokenFor(UserRole.TENANT_ADMIN, companyId);
        platformAdminToken = tokenFor(UserRole.PLATFORM_ADMIN, TenantContext.PLATFORM_TENANT_ID);
        platformStaffToken = tokenFor(UserRole.PLATFORM_STAFF, TenantContext.PLATFORM_TENANT_ID);

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
    void getAll_companyAdmin_seesPlatformAndOwnYearsButNotAnotherCompanys() {
        String platformYear = uniqueName();
        String ownYear = uniqueName();
        String otherCompanyYear = uniqueName();
        insertYear(platformYear, TenantContext.PLATFORM_TENANT_ID, true);
        insertYear(ownYear, companyId, false);
        insertYear(otherCompanyYear, otherCompanyId, false);

        ResponseEntity<String> response = get(PATH, companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(platformYear, ownYear).doesNotContain(otherCompanyYear);
    }

    /** The first seeded year sits on tenant 1, and is current for every company if it is still there. */
    @Test
    void getAll_currentYearOwnedByAnotherCompany_isStillListed() {
        String currentYear = uniqueName();
        insertYear(currentYear, otherCompanyId, true);

        ResponseEntity<String> response = get(PATH, companyAdminToken);

        assertThat(response.getBody()).contains(currentYear);
    }

    @Test
    void getAll_platformAdmin_seesEveryCompanysYears() {
        String ownedByCompany = uniqueName();
        String ownedByOtherCompany = uniqueName();
        insertYear(ownedByCompany, companyId, false);
        insertYear(ownedByOtherCompany, otherCompanyId, false);

        ResponseEntity<String> response = get(PATH, platformAdminToken);

        assertThat(response.getBody()).contains(ownedByCompany, ownedByOtherCompany);
    }

    @Test
    void getCurrent_companyAdmin_returnsThePlatformCurrentYear() {
        String platformYear = uniqueName();
        insertYear(platformYear, TenantContext.PLATFORM_TENANT_ID, true);

        ResponseEntity<String> response = get(PATH + "/current", companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(platformYear);
    }

    @Test
    void create_companyAdminWithNameUsedElsewhere_isForbiddenAndCreatesNothing() {
        String takenName = uniqueName();
        insertYear(takenName, TenantContext.PLATFORM_TENANT_ID, false);

        ResponseEntity<String> response = post(PATH, yearJson(takenName, false), companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(countByName(takenName)).isEqualTo(1);
    }

    @Test
    void setCurrent_companyAdminOnOwnYear_isForbiddenAndPlatformYearStaysCurrent() {
        Long platformYear = insertYear(uniqueName(), TenantContext.PLATFORM_TENANT_ID, true);
        Long ownYear = insertYear(uniqueName(), companyId, false);

        ResponseEntity<String> response = post(PATH + "/" + ownYear + "/set-current", "{}", companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(isCurrent(platformYear)).isTrue();
        assertThat(isCurrent(ownYear)).isFalse();
    }

    /** With no current year to collide with, the write used to succeed and set the window for every company. */
    @Test
    void setCurrent_companyAdminWhenNoYearIsCurrent_isForbiddenAndNothingBecomesCurrent() {
        Long ownYear = insertYear(uniqueName(), companyId, false);

        ResponseEntity<String> response = post(PATH + "/" + ownYear + "/set-current", "{}", companyAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(isCurrent(ownYear)).isFalse();
    }

    @Test
    void update_companyAdminOnOwnYear_isForbiddenAndLeavesTheRow() {
        String ownName = uniqueName();
        Long ownYear = insertYear(ownName, companyId, false);

        ResponseEntity<String> response = rest.exchange(PATH + "/" + ownYear, HttpMethod.PUT,
                new HttpEntity<>(yearJson(uniqueName(), false), jsonHeaders(companyAdminToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(countByName(ownName)).isEqualTo(1);
    }

    @Test
    void delete_companyAdminOnOwnYear_isForbiddenAndLeavesTheRow() {
        String ownName = uniqueName();
        Long ownYear = insertYear(ownName, companyId, false);

        ResponseEntity<String> response = rest.exchange(PATH + "/" + ownYear, HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders(companyAdminToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(countByName(ownName)).isEqualTo(1);
    }

    @Test
    void create_platformAdminWithNameUsedByACompany_isAConflictNotADatabaseError() {
        String takenName = uniqueName();
        insertYear(takenName, companyId, false);

        ResponseEntity<String> response = post(PATH, yearJson(takenName, false), platformAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void setCurrent_platformAdmin_movesCurrentOffACompanyOwnedYear() {
        Long companyOwnedCurrent = insertYear(uniqueName(), companyId, true);
        Long platformYear = insertYear(uniqueName(), TenantContext.PLATFORM_TENANT_ID, false);

        ResponseEntity<String> response = post(PATH + "/" + platformYear + "/set-current", "{}", platformAdminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isCurrent(platformYear)).isTrue();
        assertThat(isCurrent(companyOwnedCurrent)).isFalse();
    }

    @Test
    void setCurrent_platformStaff_isAllowed() {
        Long platformYear = insertYear(uniqueName(), TenantContext.PLATFORM_TENANT_ID, false);

        ResponseEntity<String> response = post(PATH + "/" + platformYear + "/set-current", "{}", platformStaffToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isCurrent(platformYear)).isTrue();
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

    private Long insertYear(String name, Long tenantId, boolean current) {
        return jdbc.queryForObject("""
                INSERT INTO academic_year
                    (name, start_date, end_date, application_open_date, application_close_date, is_current, tenant_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, name,
                Date.valueOf(LocalDate.of(2030, 2, 1)), Date.valueOf(LocalDate.of(2030, 11, 30)),
                Date.valueOf(LocalDate.of(2029, 9, 1)), Date.valueOf(LocalDate.of(2030, 1, 31)),
                current, tenantId);
    }

    private boolean isCurrent(Long id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT is_current FROM academic_year WHERE id = ?", Boolean.class, id));
    }

    private int countByName(String name) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM academic_year WHERE name = ?", Integer.class, name);
        return count == null ? 0 : count;
    }

    private String uniqueName() {
        return "AY-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String yearJson(String name, boolean current) {
        return """
                {"name":"%s","startDate":"2031-02-01","endDate":"2031-11-30",
                 "applicationOpenDate":"2030-09-01","applicationCloseDate":"2031-01-31","isCurrent":%s}
                """.formatted(name, current);
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
