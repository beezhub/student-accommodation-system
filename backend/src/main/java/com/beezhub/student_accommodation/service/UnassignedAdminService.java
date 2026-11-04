package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.UnassignedAdminResponseDto;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds and repairs admin accounts stranded on the platform tenant.
 *
 * <p>GH-212: PR #211 stopped new accounts being stamped with the platform tenant but could not
 * repair the ones already stamped — an admin account has no column naming the company that created
 * it. Tenant 0 is the sentinel that disables the Hibernate tenant filter and makes
 * {@code TenantOwnershipGuard} return without checking, so every {@code TENANT_ADMIN} and
 * {@code BUILDING_STAFF} account stamped 0 could read every company's data.
 *
 * <p>{@code V2026_09_06_1500__lock_unassigned_tenant_admins.sql} locks those accounts. This service
 * is how a platform admin works through the list: it names each account and assigns it a company,
 * which also unlocks it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnassignedAdminService {

    /**
     * Roles that must never sit on the platform tenant.
     *
     * <p>The deprecated {@code ADMIN} is included so the list surfaces it for review, but the
     * migration does not lock it: {@code UserRole.isAdmin} groups ADMIN with PLATFORM_ADMIN and
     * {@code V2026_03_19_0002} deliberately moved platform operators to tenant 0, so an ADMIN row
     * there is most likely a legitimate operator rather than a mis-stamped tenant admin.
     */
    private static final Set<UserRole> TENANT_SCOPED_ROLES =
            EnumSet.of(UserRole.TENANT_ADMIN, UserRole.BUILDING_STAFF, UserRole.ADMIN);

    /** Revocation markers must outlive the longest token lifetime (10h student tokens). */
    private static final long TOKEN_REVOCATION_MARKER_TTL_SECONDS = 12 * 60 * 60L;

    private final AppUserRepository appUserRepository;
    private final TenantService tenantService;
    private final TokenBlacklistService tokenBlacklistService;

    /** Every admin account still sitting on the platform tenant, newest first. */
    @Transactional(readOnly = true)
    public List<UnassignedAdminResponseDto> listUnassignedAdmins() {
        return appUserRepository
                .findByTenantIdAndUserRoleIn(TenantContext.PLATFORM_TENANT_ID, TENANT_SCOPED_ROLES)
                .stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(UnassignedAdminService::toResponse)
                .toList();
    }

    /**
     * Assigns an account to a company and returns it to service.
     *
     * <p>Locking is lifted here rather than in a separate call, because an account that is assigned
     * but still locked is the same operational problem in a different place — someone has to
     * remember to come back for it.
     *
     * <p>The account's existing tokens are revoked. A JWT carries the tenant claim it was issued
     * with, so without this the account would keep its platform-wide access until that token
     * expired, which is up to four hours after the reassignment.
     */
    @Transactional
    public UnassignedAdminResponseDto assignTenant(Long userId, Long tenantId) {
        if (TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            throw new IllegalArgumentException(
                    "The platform tenant is the value being repaired here; choose a property company");
        }
        // Not found, or deactivated: both refused, as everything that gives a company new records
        // is (GH-267). This is now how a company gets a new admin (#413), so it must not be the
        // one route that ignores a switched-off company.
        Tenant tenant = tenantService.requireTenantAcceptingNewRecords(tenantId);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!TenantContext.PLATFORM_TENANT_ID.equals(user.getTenantId())) {
            throw new IllegalStateException(
                    "User " + userId + " already belongs to a property company");
        }
        if (!TENANT_SCOPED_ROLES.contains(user.getUserRole())) {
            throw new IllegalStateException(
                    "User " + userId + " is a " + user.getUserRole()
                            + " and belongs on the platform tenant");
        }

        user.setTenantId(tenantId);
        user.setAccountLockedUntil(null);
        AppUser saved = appUserRepository.save(user);

        tokenBlacklistService.revokeTokensIssuedBefore(
                saved.getEmail(), Instant.now(), TOKEN_REVOCATION_MARKER_TTL_SECONDS);

        log.info("action=adminTenantAssigned userId={} tenantId={} tenantName={}",
                userId, tenantId, tenant.getName());
        return toResponse(saved);
    }

    private static UnassignedAdminResponseDto toResponse(AppUser user) {
        return new UnassignedAdminResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getUserRole(),
                user.getTenantId(),
                user.getCreatedAt(),
                user.getAccountLockedUntil());
    }
}
