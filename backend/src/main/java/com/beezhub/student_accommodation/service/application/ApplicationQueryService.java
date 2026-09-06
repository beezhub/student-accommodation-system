package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.AllocationEligibility;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.StudentProfile;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.*;

/**
 * Service handling complex application query operations.
 * Extracted from ApplicationAdminService as part of TASK-2.04 decomposition.
 * 
 * Responsibilities:
 * - Complex data assembly and aggregation
 * - Enhanced application details with nested data
 * - Read-only query operations that combine multiple data sources
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationQueryService {

    private final ApplicationRepository applicationRepository;
    private final AuthService authService;
    private final ApplicationDocumentService applicationDocumentService;

    /**
     * Gets comprehensive application details including student profile,
     * documents with nested comments, required documents checklist,
     * and allocation eligibility status.
     *
     * @param applicationId The application ID
     * @param username      The username of the requesting user
     * @return ApplicationFullDetailsResponse with all details
     */
    @Transactional(readOnly = true)
    public ApplicationFullDetailsResponse getApplicationFullDetailsEnhanced(Long applicationId, String username) {
        log.info("Getting enhanced full details for application {} by user {}", applicationId, username);

        AppUser currentUser = authService.getCurrentUser();
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with ID: " + applicationId));

        // Authorization check: admin or application owner
        boolean isAdmin = currentUser.getUserRole().isAdmin();
        boolean isOwner = application.getStudent().getAppUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to view this application's full details");
        }

        Student student = application.getStudent();
        AppUser studentUser = student.getAppUser();

        // Build student profile
        StudentProfile studentProfile = buildStudentProfile(student, studentUser);

        // Get documents with comments for this application (Delegated)
        List<DocumentWithCommentsResponse> documentsWithComments = applicationDocumentService
                .getDocumentsWithComments(applicationId);

        // Get required documents checklist (Delegated)
        RequiredDocumentsChecklistResponse checklist = applicationDocumentService
                .getRequiredDocumentsChecklist(applicationId);

        // Calculate allocation eligibility
        AllocationEligibility allocationEligibility = calculateAllocationEligibility(application, checklist);

        // Build response
        ApplicationFullDetailsResponse response = buildApplicationFullDetailsResponse(
                application, studentProfile, documentsWithComments, checklist, allocationEligibility);

        log.info("Returning enhanced full details for application {}", applicationId);
        return response;
    }

    /**
     * Builds student profile from student and user entities.
     */
    private StudentProfile buildStudentProfile(Student student, AppUser studentUser) {
        return StudentProfile.builder()
                .id(student.getId())
                .studentNumber(student.getStudentNumber())
                .firstName(studentUser.getFirstName())
                .lastName(studentUser.getLastName())
                .fullName(studentUser.getFirstName() + " " + studentUser.getLastName())
                .email(studentUser.getEmail())
                .phoneNumber(student.getPhoneNumber())
                .gender(student.getGender())
                .specialRequirements(student.getSpecialRequirements())
                .institutionName(student.getInstitution() != null ? student.getInstitution().getName() : null)
                .yearOfStudy(student.getYearOfStudy() != null ? student.getYearOfStudy().getYearOfStudy() : null)
                .build();
    }

    /**
     * Calculates allocation eligibility based on application status and document approval.
     */
    private AllocationEligibility calculateAllocationEligibility(
            Application application, RequiredDocumentsChecklistResponse checklist) {
        
        boolean applicationApproved = application.getStatus() == APPROVED ||
                application.getStatus() == AWAITING_ALLOCATION ||
                application.getStatus() == ALLOCATED;
        boolean allDocsApproved = Boolean.TRUE.equals(checklist.getAllApproved());
        boolean eligible = applicationApproved && allDocsApproved;

        String eligibilityReason = null;
        if (!applicationApproved) {
            eligibilityReason = "Application must be APPROVED first. Current status: "
                    + application.getStatus();
        } else if (!allDocsApproved) {
            eligibilityReason = "Not all required documents are approved. Approved: " +
                    checklist.getApproved() + "/" + checklist.getTotalRequired();
        }

        return AllocationEligibility.builder()
                .eligible(eligible)
                .applicationApproved(applicationApproved)
                .allDocumentsApproved(allDocsApproved)
                .reason(eligibilityReason)
                .build();
    }

    /**
     * Builds the complete application full details response.
     */
    private ApplicationFullDetailsResponse buildApplicationFullDetailsResponse(
            Application application,
            StudentProfile studentProfile,
            List<DocumentWithCommentsResponse> documentsWithComments,
            RequiredDocumentsChecklistResponse checklist,
            AllocationEligibility allocationEligibility) {
        
        return ApplicationFullDetailsResponse.builder()
                .id(application.getId())
                .applicationCode(application.getApplicationCode())
                .status(application.getStatus().name())
                .applicationDate(application.getApplicationDate())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .remarks(application.getRemarks())
                .requiresAccessibility(application.getRequiresAccessibility())
                .specialRequirements(application.getSpecialRequirements())
                .institutionName(application.getInstitutionName())
                .courseName(application.getCourseName())
                .preferredBuildingId(
                        application.getPreferredBuilding() != null
                                ? application.getPreferredBuilding().getId()
                                : null)
                .preferredBuildingName(
                        application.getPreferredBuilding() != null
                                ? application.getPreferredBuilding().getName()
                                : null)
                .allocationMethod(
                        application.getAllocationMethod() != null
                                ? application.getAllocationMethod().name()
                                : null)
                .studentProfile(studentProfile)
                .documents(documentsWithComments)
                .requiredDocumentsChecklist(checklist)
                .allocationEligibility(allocationEligibility)
                .build();
    }
}
