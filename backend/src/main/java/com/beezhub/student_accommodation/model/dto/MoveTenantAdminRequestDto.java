package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;

/** The property company a tenant admin is being moved to. */
public record MoveTenantAdminRequestDto(
        @NotNull(message = "A property company must be chosen") Long targetTenantId
) {}
