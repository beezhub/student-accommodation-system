package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.config.JwtConfig;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that:
 * 1. A TENANT_ADMIN can access admin endpoints for their own tenant.
 * 2. A TENANT_ADMIN cannot access resources belonging to another tenant (404).
 * 3. A TENANT_ADMIN receives 403 on platform-only endpoints.
 */
@DisplayName("TenantAdmin access control (integration)")
class TenantAdminAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired JwtUtil jwtUtil;

    private Tenant tenant;
    private AppUser tenantAdmin;
    private String tenantAdminToken;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(0L);
        SecurityContextHolder.clearContext();

        // Create a real tenant
        tenant = tenantRepository.save(Tenant.builder()
                .name("Test University " + System.currentTimeMillis())
                .isActive(true)
                .build());

        // Create a TENANT_ADMIN for that tenant
        tenantAdmin = appUserRepository.save(AppUser.builder()
                .email("tenant.admin." + System.currentTimeMillis() + "@test.com")
                .firstName("Tenant")
                .lastName("Admin")
                .userPassword("$2a$10$hashed")
                .userRole(UserRole.TENANT_ADMIN)
                .tenantId(tenant.getId())
                .emailVerified(true)
                .mustChangePassword(false)
                .build());

        // Generate a real JWT for the tenant admin
        AppUserDetails userDetails = new AppUserDetails(
                tenantAdmin.getEmail(), "", "TENANT_ADMIN", tenant.getId());
        tenantAdminToken = jwtUtil.generateToken(userDetails);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        appUserRepository.deleteById(tenantAdmin.getId());
        tenantRepository.deleteById(tenant.getId());
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tenantAdminToken);
        return headers;
    }

    // ── Task 9.2: TENANT_ADMIN can access tenant-scoped endpoints ─────────────

    @Test
    @DisplayName("TENANT_ADMIN GET /api/building — 200 (tenant-scoped list)")
    void tenantAdmin_canAccessBuildingsEndpoint() {
        // There is no /api/admin/buildings route; the buildings list lives at
        // /api/building and is tenant-scoped by the Hibernate tenant filter.
        ResponseEntity<String> response = rest.exchange(
                "/api/building",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        // 200 OK — returns empty list for new tenant (no buildings yet)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/applications — 200 (tenant-scoped list)")
    void tenantAdmin_canAccessApplicationsEndpoint() {
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/applications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TENANT_ADMIN GET /api/room-allocation — 200 (tenant-scoped list)")
    void tenantAdmin_canAccessRoomAllocationsEndpoint() {
        // Real path is /api/room-allocation (no /api/admin/room-allocations route);
        // GET is explicitly @PreAuthorize'd for PLATFORM_ADMIN and TENANT_ADMIN.
        ResponseEntity<String> response = rest.exchange(
                "/api/room-allocation",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── Task 9.3: TENANT_ADMIN cannot access another tenant's resource ────────

    @Test
    @DisplayName("TENANT_ADMIN GET /api/building/{id} for another tenant — 404")
    void tenantAdmin_cannotAccessOtherTenantBuilding() {
        // Building ID 999999 does not exist for this tenant (filter will exclude it)
        ResponseEntity<String> response = rest.exchange(
                "/api/building/999999",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        // Either 404 (not found in tenant scope) or 400 — must NOT be 200
        assertThat(response.getStatusCode())
                .isNotEqualTo(HttpStatus.OK)
                .isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * GH-111: the check above passes even with no isolation at all, because id 999999 does not
     * exist anywhere. This seeds a real building owned by a second tenant and proves the first
     * tenant's admin cannot update or delete it. Hibernate's filter does not apply to findById,
     * so only the explicit ownership guard stops these.
     */
    @Test
    @DisplayName("TENANT_ADMIN cannot mutate a building that really belongs to another tenant")
    void tenantAdmin_cannotMutateRealBuildingOfAnotherTenant() {
        Tenant otherTenant = tenantRepository.save(Tenant.builder()
                .name("Other University " + System.currentTimeMillis())
                .isActive(true)
                .build());
        Building building = new Building();
        building.setName("Other Tenant Hall");
        building.setAddress("1 Other Street");
        building.setTotalFloors(3);
        building.setTenantId(otherTenant.getId());
        Building otherBuilding = buildingRepository.save(building);
        try {
            HttpHeaders headers = bearerHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = """
                    {"name":"Hijacked","address":"1 Other Street","totalFloors":3}""";

            ResponseEntity<String> update = rest.exchange(
                    "/api/building/" + otherBuilding.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, headers),
                    String.class);
            assertThat(update.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            ResponseEntity<String> delete = rest.exchange(
                    "/api/building/" + otherBuilding.getId(),
                    HttpMethod.DELETE,
                    new HttpEntity<>(bearerHeaders()),
                    String.class);
            assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            // The row is untouched: same name, still owned by the other tenant.
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            Building reloaded = buildingRepository.findById(otherBuilding.getId()).orElseThrow();
            assertThat(reloaded.getName()).isEqualTo("Other Tenant Hall");
            assertThat(reloaded.getTenantId()).isEqualTo(otherTenant.getId());
        } finally {
            TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
            buildingRepository.deleteById(otherBuilding.getId());
            tenantRepository.deleteById(otherTenant.getId());
            TenantContext.clear();
        }
    }

    // ── Task 9.4: TENANT_ADMIN receives 403 on platform-only endpoints ────────

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/tenants — 403")
    void tenantAdmin_tenantManagementForbidden() {
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/tenants",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/system-settings — 200 (tenant-scoped cascade view)")
    void tenantAdmin_systemSettingsTenantScopedAccess() {
        // Intentional design (system-settings cascade model): TENANT_ADMIN gets a
        // tenant-scoped view with platform defaults overlaid by tenant overrides.
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/system-settings",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TENANT_ADMIN GET /api/platform/system-settings — 403 (platform-only)")
    void tenantAdmin_platformSystemSettingsForbidden() {
        ResponseEntity<String> response = rest.exchange(
                "/api/platform/system-settings",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/features — 403")
    void tenantAdmin_featureFlagsForbidden() {
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/features",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/activity-logs — 200 (tenant-scoped view)")
    void tenantAdmin_activityLogsTenantScopedAccess() {
        // Intentional design: activity logs are visible to TENANT_ADMIN
        // (tenant-scoped); the endpoint is @PreAuthorize'd for both admin roles.
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/activity-logs",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("TENANT_ADMIN GET /api/admin/users — 403")
    void tenantAdmin_userManagementForbidden() {
        ResponseEntity<String> response = rest.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
