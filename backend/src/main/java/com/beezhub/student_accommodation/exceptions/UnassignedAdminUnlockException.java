package com.beezhub.student_accommodation.exceptions;

/**
 * Thrown when someone tries to unlock a tenant-scoped admin account that still has no company.
 *
 * <p>#412: such an account sits on the platform tenant, which turns off the tenant filter and makes
 * {@code TenantOwnershipGuard} wave it through, so it could read every company's data (GH-212). Its
 * lock is the only thing holding that back. The way out is assigning it a company, which unlocks it
 * in the same step ({@code UnassignedAdminService.assignTenant}).
 */
public class UnassignedAdminUnlockException extends RuntimeException {
    public UnassignedAdminUnlockException(String message) {
        super(message);
    }
}
