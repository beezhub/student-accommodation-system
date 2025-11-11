package com.beezhub.student_accommodation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Example Admin Controller demonstrating role-based access control.
 * All endpoints in this controller require ADMIN role due to the role hierarchy configuration.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * Admin-only endpoint.
     * Only users with ADMIN role can access this endpoint.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAdminDashboard() {
        return ResponseEntity.ok("Admin Dashboard - Only accessible by ADMIN role");
    }

    /**
     * Endpoint accessible by ADMIN and LANDLORD roles.
     * Due to role hierarchy, ADMIN can also access this.
     */
    @GetMapping("/landlords")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<String> manageLandlords() {
        return ResponseEntity.ok("Landlord Management - Accessible by ADMIN and LANDLORD roles");
    }

    /**
     * Endpoint accessible by all authenticated users.
     * Due to role hierarchy, ADMIN, LANDLORD, and STUDENT can all access this.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<String> getProfile() {
        return ResponseEntity.ok("User Profile - Accessible by all authenticated users");
    }

    /**
     * Endpoint with multiple role requirements.
     * Only ADMIN or LANDLORD can access this endpoint.
     */
    @PostMapping("/properties")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
    public ResponseEntity<String> manageProperties() {
        return ResponseEntity.ok("Property Management - Accessible by ADMIN and LANDLORD roles");
    }

    /**
     * Endpoint demonstrating permission-based access.
     * Using hasAuthority instead of hasRole (doesn't add ROLE_ prefix).
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok("User deleted - Only accessible by ADMIN role");
    }
}
