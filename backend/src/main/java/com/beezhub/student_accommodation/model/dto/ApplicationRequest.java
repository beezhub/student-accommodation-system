package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    private Boolean requiresAccessibility;
    private String specialRequirements;
    private String institutionName;
    private String courseName;

    // Legacy single building preference (kept for backward compatibility)
    private Long preferredBuildingId;

    // New: ordered multi-building preferences (max 3)
    @Size(max = 3, message = "Maximum of 3 preferred buildings allowed")
    private List<Long> preferredBuildingIds;
}
