package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectAllocationRequest {

    @NotBlank(message = "Reason for rejection is required")
    private String reason;
}
