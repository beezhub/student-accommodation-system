package com.beezhub.student_accommodation.model.enums;

import lombok.*;

import java.util.Arrays;


@ToString
@Getter
public enum UserRole {
    STUDENT("Student"),
    PLATFORM_ADMIN("Platform_Admin"),
    TENANT_ADMIN("Tenant_Admin"),
    BUILDING_STAFF("Building_Staff"),
    /**
     * Platform employees who work across companies: reviewing applications and documents, chasing
     * allocations, helping companies with their data (GH-238).
     *
     * <p>Stamped {@code tenant_id = 0} like {@code PLATFORM_ADMIN}, which is what makes it
     * cross-company — {@code TenantFilterAspect} skips the Hibernate filter for the platform
     * sentinel. It sits between PLATFORM_ADMIN and TENANT_ADMIN in the hierarchy, so it holds the
     * whole shared admin surface and none of the governance ones: it cannot create or delete
     * users, mint companies, flip feature flags, or write the platform defaults every company
     * inherits.
     *
     * <p>Not to be confused with the deprecated {@code ADMIN}, which is on its way out and which
     * every scoping decision in the codebase treats as company-scoped.
     */
    PLATFORM_STAFF("Platform_Staff"),
    @Deprecated ADMIN("Admin");

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
     *
     * <p>{@code PLATFORM_STAFF} is included, and that matters in more places than it looks:
     * {@code JwtUtil.isAdminRole} derives the shorter 4h token lifetime from it, and
     * {@code DocumentAccessGuard.assertCanAccessDocument} refuses outright unless it is true —
     * so omitting it here would lock platform staff out of documents entirely (GH-238).
     */
    public boolean isAdmin() {
        return this == PLATFORM_ADMIN || this == PLATFORM_STAFF || this == TENANT_ADMIN
                || this == ADMIN;
    }

    /**
     * Whether this role lives at the platform tenant rather than being scoped to one company.
     *
     * <p>The deprecated {@code ADMIN} is deliberately excluded even though {@link #isAdmin()}
     * groups it with PLATFORM_ADMIN. Every place that decides tenant scoping treats an ADMIN row
     * as company-scoped — {@code UserManagementService.resolveTenantForRole} refuses to create one
     * without a real tenant, {@code DocumentAccessGuard.tenantScopeOf} narrows its document view
     * to that tenant, and {@code UnassignedAdminService} lists it for assignment. Including it
     * here would flip all three at once and hand a legacy role cross-company reach it never had
     * (GH-238).
     */
    public boolean isPlatformLevel() {
        return this == PLATFORM_ADMIN || this == PLATFORM_STAFF;
    }
}
