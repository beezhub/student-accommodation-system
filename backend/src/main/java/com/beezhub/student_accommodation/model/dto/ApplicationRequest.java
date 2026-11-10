package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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

    // Ordered building preferences, max 3 in total, from any companies (GH-495). On submit they
    // are grouped into one application per company, keeping the student's order.
    @Size(max = 3, message = "Maximum of 3 preferred buildings allowed")
    private List<Long> preferredBuildingIds;

    /**
     * The intake chosen at each company, keyed by company (tenant) id. A company left out uses its
     * one open intake, so a client that does not send intakes keeps working while only one is open.
     */
    private Map<Long, Long> intakeIds;
}
