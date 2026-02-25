package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying the v2.0 role hierarchy configuration.
 *
 * <p>Hierarchy: PLATFORM_ADMIN > BUILDING_OWNER > BUILDING_STAFF
 * STUDENT is intentionally disconnected from the admin hierarchy.
 */
@DisplayName("SecurityConfig role hierarchy tests")
class SecurityConfigTest {

    private RoleHierarchy roleHierarchy;

    @BeforeEach
    void setUp() {
        // Mirror exactly what SecurityConfig.roleHierarchy() configures
        roleHierarchy = RoleHierarchyImpl.fromHierarchy(
                "ROLE_PLATFORM_ADMIN > ROLE_BUILDING_OWNER \n ROLE_BUILDING_OWNER > ROLE_BUILDING_STAFF");
    }

    @Test
    @DisplayName("PLATFORM_ADMIN inherits BUILDING_OWNER and BUILDING_STAFF roles")
    void platformAdmin_hasAllLowerRoles() {
        Collection<? extends GrantedAuthority> reachable =
                roleHierarchy.getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "ROLE_PLATFORM_ADMIN",
                        "ROLE_BUILDING_OWNER",
                        "ROLE_BUILDING_STAFF");
    }

    @Test
    @DisplayName("BUILDING_OWNER inherits BUILDING_STAFF but not PLATFORM_ADMIN")
    void buildingOwner_hasBuildingStaffButNotPlatformAdmin() {
        Collection<? extends GrantedAuthority> reachable =
                roleHierarchy.getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority("ROLE_BUILDING_OWNER")));

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_BUILDING_OWNER", "ROLE_BUILDING_STAFF")
                .doesNotContain("ROLE_PLATFORM_ADMIN");
    }

    @Test
    @DisplayName("BUILDING_STAFF has only its own role (C4)")
    void buildingStaff_hasOnlyOwnRole() {
        Collection<? extends GrantedAuthority> reachable =
                roleHierarchy.getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority("ROLE_BUILDING_STAFF")));

        assertThat(reachable)
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_BUILDING_STAFF");
    }

    @Test
    @DisplayName("STUDENT has only its own role and is not in the admin hierarchy")
    void student_hasOnlyOwnRole_notInAdminHierarchy() {
        Collection<? extends GrantedAuthority> reachable =
                roleHierarchy.getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        assertThat(reachable)
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .doesNotContainAnyElementsOf(List.of(
                        "ROLE_PLATFORM_ADMIN", "ROLE_BUILDING_OWNER", "ROLE_BUILDING_STAFF"));
    }
}
