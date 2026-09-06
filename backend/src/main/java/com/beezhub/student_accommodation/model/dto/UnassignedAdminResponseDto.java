package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.UserRole;
import java.time.LocalDateTime;

/**
 * An admin account still sitting on the platform tenant, which gives it cross-company read access
 * it was never meant to have (GH-212).
 *
 * @param accountLockedUntil the far-future sentinel while the account is locked pending
 *                           reassignment, null once it has been assigned a company
 */
public record UnassignedAdminResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole userRole,
        Long tenantId,
        LocalDateTime createdAt,
        LocalDateTime accountLockedUntil
) {}
