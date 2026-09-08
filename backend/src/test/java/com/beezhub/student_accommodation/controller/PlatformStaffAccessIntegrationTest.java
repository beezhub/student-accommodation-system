package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Access control for PLATFORM_STAFF (GH-238).
 *
 * <p>Staff exist to work across companies without holding the platform's governance surfaces. The
 * whole role is expressed in two places and nowhere else: the hierarchy edge
 * {@code PLATFORM_ADMIN > PLATFORM_STAFF > TENANT_ADMIN} in {@code SecurityConfig}, which hands
 * staff every endpoint written as {@code hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')}, and the
 * {@code tenant_id = 0} stamp, which turns the Hibernate tenant filter off so that surface spans
 * every company.
 *
 * <p>That is a lot of reach granted implicitly. Nothing in the code says "staff may not manage
 * users" — it follows only from those endpoints happening to be spelled
 * {@code hasRole('PLATFORM_ADMIN')}, and a future endpoint spelled the shared way would silently
 * be staff-reachable. This test writes the boundary down: every platform-only endpoint is
 * enumerated and asserted 403, and the shared surface is asserted reachable and cross-company.
 */
@DisplayName("PlatformStaff access control (integration)")
class PlatformStaffAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired JwtUtil jwtUtil;

    private AppUser staff;
    private String staffToken;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        SecurityContextHolder.clearContext();

        staff = appUserRepository.save(AppUser.builder()
                .email("platform.staff." + System.nanoTime() + "@test.com")
                .firstName("Platform")
                .lastName("Staff")
                .userPassword("$2a$10$hashed")
                .userRole(UserRole.PLATFORM_STAFF)
                .tenantId(TenantContext.PLATFORM_TENANT_ID)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());

        staffToken = jwtUtil.generateToken(new AppUserDetails(
                staff.getEmail(), "", UserRole.PLATFORM_STAFF.name(), TenantContext.PLATFORM_TENANT_ID));

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        appUserRepository.deleteById(staff.getId());
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(staffToken);
        return headers;
    }

    private ResponseEntity<String> get(String path) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearerHeaders()), String.class);
    }

    // Governance surfaces: platform admin only

    /**
     * Every endpoint annotated {@code hasRole('PLATFORM_ADMIN')} that answers a GET. Adding a
     * governance endpoint means adding a row here; a governance endpoint written with the shared
     * annotation by mistake will not show up as a failure, which is exactly why the list is
     * explicit rather than derived.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/tenants",
            "/api/admin/tenants/1/admins",
            "/api/admin/unassigned-admins",
            "/api/admin/users",
            "/api/admin/users/role/TENANT_ADMIN",
            "/api/admin/features",
            "/api/platform/system-settings"
    })
    @DisplayName("PLATFORM_STAFF is refused every platform-only endpoint")
    void platformStaff_governanceEndpointsForbidden(String path) {
        assertThat(get(path).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("PLATFORM_STAFF cannot create a user")
    void platformStaff_cannotCreateUsers() {
        HttpHeaders headers = bearerHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"new.admin@test.com","firstName":"New","lastName":"Admin",
                 "password":"Sup3rSecret!","userRole":"Tenant_Admin","tenantId":1}""";

        ResponseEntity<String> response = rest.exchange(
                "/api/admin/users", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("PLATFORM_STAFF cannot flip a feature flag")
    void platformStaff_cannotToggleFeatureFlags() {
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/features/auto-allocation/enable",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // Shared surface: reachable, and it spans companies

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin/applications",
            "/api/admin/system-settings",
            "/api/admin/activity-logs",
            "/api/room-allocation"
    })
    @DisplayName("PLATFORM_STAFF reaches the shared admin surface")
    void platformStaff_sharedAdminEndpointsAllowed(String path) {
        // Granted by the hierarchy edge alone: these are spelled
        // hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN'), and PLATFORM_STAFF > TENANT_ADMIN.
        assertThat(get(path).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * The cross-company half of the role. A TENANT_ADMIN gets 404 on another company's building
     * (see {@code TenantAdminAccessIntegrationTest}); staff sit on tenant 0, so
     * {@code TenantFilterAspect} skips the Hibernate filter and {@code TenantOwnershipGuard} waves
     * the write through. GH-238 chose a shared surface including writes, so this passing is the
     * decision, not an oversight — if staff should ever become read-only, this is the test that
     * has to change.
     */
    @Test
    @DisplayName("PLATFORM_STAFF reads and writes a building owned by a company")
    void platformStaff_reachesAnotherCompanysRows() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        Tenant company = tenantRepository.save(Tenant.builder()
                .name("Company " + System.nanoTime())
                .isActive(true)
                .build());
        Building building = new Building();
        building.setName("Company Hall");
        building.setAddress("1 Company Street");
        building.setTotalFloors(3);
        building.setTenantId(company.getId());
        Building saved = buildingRepository.save(building);
        TenantContext.clear();

        try {
            assertThat(get("/api/building/" + saved.getId()).getStatusCode()).isEqualTo(HttpStatus.OK);

            HttpHeaders headers = bearerHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = """
                    {"name":"Renamed By Staff","address":"1 Company Street","totalFloors":3}""";
            ResponseEntity<String> update = rest.exchange(
                    "/api/building/" + saved.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, headers),
                    String.class);
            assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);

            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            Building reloaded = buildingRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getName()).isEqualTo("Renamed By Staff");
            // Editing on a company's behalf must not move the row onto the platform tenant.
            assertThat(reloaded.getTenantId()).isEqualTo(company.getId());
        } finally {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            buildingRepository.deleteById(saved.getId());
            tenantRepository.deleteById(company.getId());
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("PLATFORM_STAFF gets the 4h admin token lifetime, not the 10h student one")
    void platformStaff_tokenUsesTheAdminLifetime() {
        // UserRole.isAdmin() drives JwtUtil's expiry choice, so a role left out of it would hand
        // an account with cross-company reach the longer student lifetime.
        assertThat(UserRole.PLATFORM_STAFF.isAdmin()).isTrue();
        assertThat(jwtUtil.extractExpiration(staffToken))
                .isBefore(new java.util.Date(System.currentTimeMillis() + 5 * 60 * 60 * 1000L));
    }
}
