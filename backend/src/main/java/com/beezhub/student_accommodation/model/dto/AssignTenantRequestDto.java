package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;

/** The property company to assign a stranded admin account to (GH-212). */
public record AssignTenantRequestDto(
        @NotNull(message = "A property company must be chosen") Long tenantId
) {}
