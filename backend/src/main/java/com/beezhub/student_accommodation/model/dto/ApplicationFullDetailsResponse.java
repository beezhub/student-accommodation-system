package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive DTO for full application details including student profile,
 * documents with comments, required documents checklist, and allocation eligibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationFullDetailsResponse {

    // Application data
    private Long id;
    private String applicationCode;
    private String status;
    private LocalDate applicationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String remarks;

    // Preferences
    private String preferredAccommodationType;
    private LocalDate preferredMoveInDate;
    private Integer preferredDurationMonths;
    private String preferredRoomType;
    private String specialRequirements;
    private String institutionName;
    private String courseName;
    private Long preferredBuildingId;
    private String preferredBuildingName;
    private String allocationMethod;

    // Student profile
    private StudentProfile studentProfile;

    // Documents with nested comments
    private List<DocumentWithCommentsResponse> documents;

    // Required documents checklist
    private RequiredDocumentsChecklistResponse requiredDocumentsChecklist;

    // Allocation eligibility status
    private AllocationEligibility allocationEligibility;

    /**
     * Nested student profile information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentProfile {
        private Long id;
        private String studentNumber;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String gender;
        private String specialRequirements;
        private String institutionName;
        private String yearOfStudy;
    }

    /**
     * Allocation eligibility status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationEligibility {
        private Boolean eligible;
        private Boolean applicationApproved;
        private Boolean allDocumentsApproved;
        private String reason;
    }
}
