package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private String applicationCode;
    private LocalDate applicationDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Student details
    private Long studentId;
    private String studentNumber;
    private String studentName;
    private String studentEmail;

    // Preferences
    private Boolean requiresAccessibility;
    private String specialRequirements;
    private String institutionName;
    private String courseName;

    // Building preference
    private Long preferredBuildingId;
    private String preferredBuildingName;

    // Ordered building preferences (new multi-preference)
    private List<BuildingPreferenceResponse> preferredBuildings;

    // Allocation method
    private String allocationMethod;

    // Remarks (for rejection reasons, etc.)
    private String remarks;

    // Review Status & Stats
    // @Column(precision = 10, scale = 4) in StagedAllocation, so BigDecimal here
    private java.math.BigDecimal allocationScore;

    private Integer documentCompletionPercent;
    private Integer approvedDocuments;
    private Integer totalRequiredDocuments;

    private String allocationStatus;

    /** A required or other expiring document of the student has expired at this company (GH-498). */
    private Boolean hasExpiredDocuments;

    /** The student had been checked in at this company when they applied (GH-499). */
    private Boolean isReturning;

    private Long tenantId;

    /** The company the application is with (GH-495). */
    private String companyName;

    // The intake applied for and its year (GH-495). Empty only on rows step 3 could not repair.
    private Long academicYearId;
    private String academicYearName;
    private Long intakeId;
    private String intakeName;
    private String intakeType;
    private LocalDate intakeStayStartDate;
    private LocalDate intakeStayEndDate;

    /** The student's institution when they applied. */
    private Long institutionId;
}