package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AllocateByApplicationRequest {
    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotNull(message = "Room ID is required")
    private Long roomId;
}
