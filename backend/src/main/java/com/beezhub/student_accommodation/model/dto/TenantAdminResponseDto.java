package com.beezhub.student_accommodation.model.dto;

import java.time.LocalDateTime;

public record TenantAdminResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        LocalDateTime createdAt,
        Boolean mustChangePassword,
        LocalDateTime accountLockedUntil
) {}
