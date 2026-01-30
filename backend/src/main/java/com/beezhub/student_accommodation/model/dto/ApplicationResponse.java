package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String preferredAccommodationType;
    private LocalDate preferredMoveInDate;
    private Integer preferredDurationMonths;
    private Boolean requiresAccessibility;
    private String specialRequirements;
    private String institutionName;
    private String courseName;

    // Building preference
    private Long preferredBuildingId;
    private String preferredBuildingName;

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
}