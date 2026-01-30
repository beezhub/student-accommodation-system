package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    private String preferredAccommodationType;
    private LocalDate preferredMoveInDate;
    private Integer preferredDurationMonths;
    private Boolean requiresAccessibility;
    private String specialRequirements;
    private String institutionName;
    private String courseName;

    // Building preference
    private Long preferredBuildingId;
}
