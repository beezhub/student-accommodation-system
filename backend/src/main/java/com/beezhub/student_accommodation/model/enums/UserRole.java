package com.beezhub.student_accommodation.model.enums;

import lombok.*;

import java.util.Arrays;


@ToString
@Getter
public enum UserRole {
    STUDENT("Student"),
    PLATFORM_ADMIN("Platform_Admin"),
    BUILDING_OWNER("Building_Owner"),
    BUILDING_STAFF("Building_Staff"),
    @Deprecated ADMIN("Admin"),
    @Deprecated LANDLORD("Landlord");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    /**
     * Resolves a UserRole by its display name field (case-insensitive).
     * Matches against the {@code role} display string, NOT the enum constant name.
     * Examples:
     * <ul>
     *   <li>{@code enumRoleOf("Platform_Admin")} → {@code PLATFORM_ADMIN}</li>
     *   <li>{@code enumRoleOf("PLATFORM_ADMIN")} → {@code PLATFORM_ADMIN} (case-insensitive)</li>
     *   <li>{@code enumRoleOf("Admin")} → {@code ADMIN} (deprecated, backward compat)</li>
     * </ul>
     */
    public static UserRole enumRoleOf(String role) {
        return Arrays.stream(UserRole.values())
                .filter(userRole -> userRole.getRole().equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + role));
    }

    /**
     * Returns true if this role has administrative privileges.
     * Covers both {@code PLATFORM_ADMIN} and the deprecated {@code ADMIN} during the migration window.
     */
    public boolean isAdmin() {
        return this == PLATFORM_ADMIN || this == ADMIN;
    }
}
