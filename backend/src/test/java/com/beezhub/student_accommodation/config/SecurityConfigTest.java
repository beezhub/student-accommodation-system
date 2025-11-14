package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test class to verify role hierarchy configuration without Spring context.
 */
class SecurityConfigTest {

    private RoleHierarchy roleHierarchy;

    @BeforeEach
    void setUp() {
        // Create role hierarchy matching SecurityConfig
        roleHierarchy = RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_LANDLORD \n ROLE_LANDLORD > ROLE_STUDENT");
    }

    @Test
    void testRoleHierarchyConfiguration() {
        // Given - Admin role
        GrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Collection<? extends GrantedAuthority> adminReachableAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(adminAuthority));

        // Then - Admin should have ADMIN, LANDLORD, and STUDENT roles
        assertThat(adminReachableAuthorities)
                .hasSize(3)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_LANDLORD", "ROLE_STUDENT");
    }

    @Test
    void testLandlordRoleHierarchy() {
        // Given - Landlord role
        GrantedAuthority landlordAuthority = new SimpleGrantedAuthority("ROLE_LANDLORD");
        Collection<? extends GrantedAuthority> landlordReachableAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(landlordAuthority));

        // Then - Landlord should have LANDLORD and STUDENT roles
        assertThat(landlordReachableAuthorities)
                .hasSize(2)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_LANDLORD", "ROLE_STUDENT");
    }

    @Test
    void testStudentRoleHierarchy() {
        // Given - Student role
        GrantedAuthority studentAuthority = new SimpleGrantedAuthority("ROLE_STUDENT");
        Collection<? extends GrantedAuthority> studentReachableAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(studentAuthority));

        // Then - Student should only have STUDENT role
        assertThat(studentReachableAuthorities)
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void testAdminCanAccessLandlordEndpoints() {
        // Given - Admin role
        GrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Collection<? extends GrantedAuthority> adminAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(adminAuthority));

        // Then - Admin authorities should include LANDLORD role
        assertThat(adminAuthorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_LANDLORD");
    }

    @Test
    void testAdminCanAccessStudentEndpoints() {
        // Given - Admin role
        GrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Collection<? extends GrantedAuthority> adminAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(adminAuthority));

        // Then - Admin authorities should include STUDENT role
        assertThat(adminAuthorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_STUDENT");
    }

    @Test
    void testLandlordCanAccessStudentEndpoints() {
        // Given - Landlord role
        GrantedAuthority landlordAuthority = new SimpleGrantedAuthority("ROLE_LANDLORD");
        Collection<? extends GrantedAuthority> landlordAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(landlordAuthority));

        // Then - Landlord authorities should include STUDENT role
        assertThat(landlordAuthorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_STUDENT");
    }

    @Test
    void testStudentCannotAccessLandlordEndpoints() {
        // Given - Student role
        GrantedAuthority studentAuthority = new SimpleGrantedAuthority("ROLE_STUDENT");
        Collection<? extends GrantedAuthority> studentAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(studentAuthority));

        // Then - Student authorities should NOT include LANDLORD role
        assertThat(studentAuthorities)
                .extracting(GrantedAuthority::getAuthority)
                .doesNotContain("ROLE_LANDLORD");
    }

    @Test
    void testStudentCannotAccessAdminEndpoints() {
        // Given - Student role
        GrantedAuthority studentAuthority = new SimpleGrantedAuthority("ROLE_STUDENT");
        Collection<? extends GrantedAuthority> studentAuthorities = 
                roleHierarchy.getReachableGrantedAuthorities(List.of(studentAuthority));

        // Then - Student authorities should NOT include ADMIN role
        assertThat(studentAuthorities)
                .extracting(GrantedAuthority::getAuthority)
                .doesNotContain("ROLE_ADMIN");
    }
}
