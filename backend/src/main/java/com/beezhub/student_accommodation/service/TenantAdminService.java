package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.TenantNotFoundException;
import com.beezhub.student_accommodation.model.dto.TenantAdminRequestDto;
import com.beezhub.student_accommodation.model.dto.TenantAdminResponseDto;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAdminService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final TokenBlacklistService tokenBlacklistService;

    /** Revocation markers must outlive the longest token lifetime (10h student tokens). */
    private static final long TOKEN_REVOCATION_MARKER_TTL_SECONDS = 12 * 60 * 60L;

    @Transactional
    public TenantAdminResponseDto createTenantAdmin(Long tenantId, TenantAdminRequestDto request) {
        // Was a local check throwing DuplicateResourceException, which the handler answers with
        // 409 although nothing is duplicated, and which the other route to creating a tenant admin
        // did not perform at all (GH-267). One implementation now, shared with building and user
        // creation, so the two routes cannot disagree.
        tenantService.requireTenantAcceptingNewRecords(tenantId);

        if (appUserRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already in use: " + request.email());
        }

        AppUser admin = AppUser.builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .userPassword(passwordEncoder.encode(request.password()))
                .userRole(UserRole.TENANT_ADMIN)
                .tenantId(tenantId)
                .emailVerified(true)
                .mustChangePassword(true)
                .build();

        AppUser saved = appUserRepository.save(admin);
        log.info("Created TENANT_ADMIN user {} for tenant {}", saved.getEmail(), tenantId);

        notificationService.sendWelcomeEmail(saved);

        return toResponse(saved);
    }

    public List<TenantAdminResponseDto> listTenantAdmins(Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new TenantNotFoundException("Tenant not found with id: " + tenantId);
        }
        return appUserRepository.findByTenantIdAndUserRole(tenantId, UserRole.TENANT_ADMIN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deactivateTenantAdmin(Long tenantId, Long userId) {
        AppUser admin = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!tenantId.equals(admin.getTenantId()) || admin.getUserRole() != UserRole.TENANT_ADMIN) {
            throw new ResourceNotFoundException(
                    "TENANT_ADMIN user " + userId + " not found for tenant " + tenantId);
        }

        admin.setAccountLockedUntil(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        appUserRepository.save(admin);

        // Locking only stops future logins; tokens already issued stay valid until they expire.
        // Retire them now (GH-122). The marker outlives the longest token lifetime.
        tokenBlacklistService.revokeTokensIssuedBefore(
                admin.getEmail(), Instant.now(), TOKEN_REVOCATION_MARKER_TTL_SECONDS);

        log.info("Deactivated TENANT_ADMIN user {} for tenant {}", userId, tenantId);
    }

    /**
     * Moves a tenant admin to another property company.
     *
     * <p>{@code UnassignedAdminService.assignTenant} only repairs accounts stranded on the platform
     * tenant, so an admin created under the wrong company had no fix short of a SQL update. The
     * account's existing tokens are revoked because each JWT carries the tenant claim it was issued
     * with: without this the admin keeps the old company's data until the token expires.
     */
    @Transactional
    public TenantAdminResponseDto moveTenantAdmin(Long tenantId, Long userId, Long targetTenantId) {
        if (TenantContext.PLATFORM_TENANT_ID.equals(targetTenantId)) {
            throw new IllegalArgumentException("Tenant admins cannot be moved to the platform tenant");
        }
        if (tenantId.equals(targetTenantId)) {
            throw new IllegalArgumentException("User " + userId + " already belongs to tenant " + tenantId);
        }

        AppUser admin = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!tenantId.equals(admin.getTenantId()) || admin.getUserRole() != UserRole.TENANT_ADMIN) {
            throw new ResourceNotFoundException(
                    "TENANT_ADMIN user " + userId + " not found for tenant " + tenantId);
        }

        tenantService.requireTenantAcceptingNewRecords(targetTenantId);

        admin.setTenantId(targetTenantId);
        AppUser saved = appUserRepository.save(admin);

        tokenBlacklistService.revokeTokensIssuedBefore(
                saved.getEmail(), Instant.now(), TOKEN_REVOCATION_MARKER_TTL_SECONDS);

        log.info("action=tenantAdminMoved userId={} fromTenantId={} toTenantId={}",
                userId, tenantId, targetTenantId);
        return toResponse(saved);
    }

    private TenantAdminResponseDto toResponse(AppUser user) {
        return new TenantAdminResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt(),
                user.getMustChangePassword(),
                user.getAccountLockedUntil());
    }
}
