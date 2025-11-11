package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|APPROVED|REJECTED", message = "Status must be PENDING, APPROVED, or REJECTED")
    private String status;
    
    private String remarks;
}
