package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.event.ApplicationStatusChangedEvent;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.*;

/**
 * Service handling admin-facing application operations.
 * Extracted from ApplicationService as part of TASK-2.04 decomposition.
 * 
 * Responsibilities:
 * - Status transitions (approve, reject, mark under review)
 * - Document review progress
 * - Required documents checklist
 * - Moving applications to allocation phase
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationAdminService {

    private final ApplicationRepository applicationRepository;

    private final AppUserRepository appUserRepository;
    private final ApplicationMapper applicationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserNotificationService userNotificationService;
    private final AuthService authService;
    private final RoomAllocationService roomAllocationService;
    private final ApplicationDocumentService applicationDocumentService;
    private final ApplicationQueryService applicationQueryService;
    private final com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    /**
     * Updates application status with validation and notifications.
     * 
     * @param applicationId Application ID
     * @param request       Status update request
     * @return Updated application response
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Updated application {0} status to {1}", entityIdParamIndex = 0)
    public ApplicationResponse updateApplicationStatus(Long applicationId,
            @Valid ApplicationStatusUpdateRequest request) {
        log.info("Updating application {} to status: {}", applicationId, request.getStatus());

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with ID: " + applicationId));
        tenantOwnershipGuard.assertOwned(application, "Application", applicationId);

        ApplicationStatus oldStatus = application.getStatus();
        ApplicationStatus newStatus = parseStatus(request.getStatus());

        // Validate status transition
        validateStatusTransition(application, newStatus);

        application.setStatus(newStatus);
        application.setRemarks(request.getRemarks());

        Application updatedApplication = applicationRepository.save(application);
        log.info("Application {} status updated from {} to: {}", applicationId, oldStatus, newStatus);

        // Send notification if status changed to APPROVED or REJECTED
        if (newStatus != oldStatus && (newStatus == APPROVED || newStatus == REJECTED)) {
            try {
                log.info("Triggering notification for application {} with status {}", applicationId,
                        newStatus);
                // Send email notification via event
                eventPublisher
                        .publishEvent(new ApplicationStatusChangedEvent(updatedApplication, oldStatus, newStatus));

                // Create in-app notification
                Long studentUserId = application.getStudent().getAppUser().getId();
                userNotificationService.createApplicationStatusNotification(
                        studentUserId,
                        applicationId,
                        newStatus == APPROVED,
                        request.getRemarks());
            } catch (Exception e) {
                // Log error but don't fail the request - notification will be retried
                log.error(
                        "Failed to trigger notification for application {} - notification will be retried by scheduler",
                        applicationId, e);
            }
        }

        // Attempt auto-allocation if approved
        if (newStatus == APPROVED) {
            try {
                roomAllocationService.attemptAutoAllocation(updatedApplication);
                // Re-fetch to get updated status (might have changed to AWAITING_ALLOCATION)
                updatedApplication = applicationRepository.findById(applicationId)
                        .orElse(updatedApplication);
            } catch (Exception e) {
                log.error("Auto-allocation failed for application {}, will require manual allocation",
                        applicationId, e);
                // Don't fail the approval - admin can manually allocate later
            }
        }

        return applicationMapper.toResponse(updatedApplication);
    }

    /**
     * Marks an application as under review.
     * 
     * @param applicationId Application ID
     * @return Updated application response
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Marked application {0} under review", entityIdParamIndex = 0)
    public ApplicationResponse markUnderReview(Long applicationId) {
        log.info("Marking application {} as UNDER_REVIEW", applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with ID: " + applicationId));
        tenantOwnershipGuard.assertOwned(application, "Application", applicationId);

        validateStatusTransition(application, UNDER_REVIEW);

        application.setStatus(UNDER_REVIEW);
        Application saved = applicationRepository.save(application);

        return applicationMapper.toResponse(saved);
    }

    /**
     * Requests changes on an application with notification to student.
     * 
     * @param applicationId Application ID
     * @param reason        Reason for requesting changes
     * @return Updated application response
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Requested changes for application {0}", entityIdParamIndex = 0)
    public ApplicationResponse requestChanges(Long applicationId, String reason) {
        log.info("Requesting changes for application {} with reason: {}", applicationId, reason);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with ID: " + applicationId));
        tenantOwnershipGuard.assertOwned(application, "Application", applicationId);

        ApplicationStatus oldStatus = application.getStatus();
        validateStatusTransition(application, CHANGES_REQUESTED);

        application.setStatus(CHANGES_REQUESTED);
        application.setRemarks(reason);
        Application saved = applicationRepository.save(application);

        // Notify student
        try {
            Long studentUserId = application.getStudent().getAppUser().getId();
            userNotificationService.createApplicationStatusNotification(
                    studentUserId,
                    applicationId,
                    false,
                    "Changes requested: " + reason);

            eventPublisher.publishEvent(new ApplicationStatusChangedEvent(saved, oldStatus, CHANGES_REQUESTED));
        } catch (Exception e) {
            log.error("Failed to notify student of changes requested for application {}", applicationId, e);
        }

        return applicationMapper.toResponse(saved);
    }

    /**
     * Gets the required documents checklist for an application.
     * Cross-checks required document types against student's uploaded documents.
     *
     * @param applicationId The application ID
     * @return RequiredDocumentsChecklistResponse with document status breakdown
     */
    public RequiredDocumentsChecklistResponse getRequiredDocumentsChecklist(Long applicationId) {
        return applicationDocumentService.getRequiredDocumentsChecklist(applicationId);
    }

    /**
     * Moves an application to the allocation phase.
     * Validates that application is APPROVED and all required documents are
     * approved.
     *
     * @param applicationId The application ID
     * @param adminId       The admin user ID performing the action
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Moved application {0} to allocation phase", entityIdParamIndex = 0)
    public ApplicationResponse moveToAllocationPhase(Long applicationId, Long adminId) {
        log.info("Moving application {} to allocation phase by admin {}", applicationId, adminId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with ID: " + applicationId));
        tenantOwnershipGuard.assertOwned(application, "Application", applicationId);

        // Validate current status is APPROVED
        if (application.getStatus() != APPROVED) {
            throw new InvalidStatusException(
                    "Application must be APPROVED before moving to allocation phase. Current status: "
                            + application.getStatus());
        }

        // Validate all required documents are approved
        RequiredDocumentsChecklistResponse checklist = getRequiredDocumentsChecklist(applicationId);
        if (!Boolean.TRUE.equals(checklist.getAllApproved())) {
            throw new InvalidStatusException(
                    "All required documents must be approved before moving to allocation phase. " +
                            "Approved: " + checklist.getApproved() + "/"
                            + checklist.getTotalRequired());
        }

        // Update status to AWAITING_ALLOCATION
        application.setStatus(AWAITING_ALLOCATION);
        Application updatedApplication = applicationRepository.save(application);

        log.info("Application {} moved to AWAITING_ALLOCATION phase", applicationId);

        // Send notification to student
        try {
            Long studentUserId = application.getStudent().getAppUser().getId();
            userNotificationService.createApplicationStatusNotification(
                    studentUserId,
                    applicationId,
                    true,
                    "Your application is now ready for room allocation.");
        } catch (Exception e) {
            log.error("Failed to send notification for application {} allocation phase transition",
                    applicationId, e);
        }

        return applicationMapper.toResponse(updatedApplication);
    }

    /**
     * Moves an application to the allocation phase.
     * Validates that application is APPROVED and all required documents are
     * approved.
     *
     * @param applicationId The application ID
     * @param username      The admin username performing the action
     * @return ApplicationResponse with new status
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Moved application {0} to allocation phase", entityIdParamIndex = 0)
    public ApplicationResponse moveToAllocationPhaseByUsername(Long applicationId, String username) {
        log.info("Moving application {} to allocation phase by {}", applicationId, username);

        // Get admin user
        AppUser admin = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!admin.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Only admins can move applications to allocation phase");
        }

        return moveToAllocationPhase(applicationId, admin.getId());
    }

    /**
     * Gets full application details including student profile, documents with
     * comments,
     * checklist, and preferred building details.
     * For admin use only.
     *
     * @param applicationId The application ID
     * @return ApplicationResponse with full details
     */
    public ApplicationResponse getApplicationFullDetails(Long applicationId) {
        log.info("Getting full details for application {}", applicationId);

        // Verify admin access
        AppUser currentUser = authService.getCurrentUser();
        if (!currentUser.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Only admins can access full application details");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with ID: " + applicationId));
        tenantOwnershipGuard.assertOwned(application, "Application", applicationId);

        ApplicationResponse response = applicationMapper.toResponse(application);

        log.info("Returning full details for application {}", applicationId);
        return response;
    }

    /**
     * Saves document review progress atomically.
     * Updates document statuses and adds comments without completing the review.
     *
     * @param applicationId The application ID
     * @param request       The review progress request
     * @param username      The admin username
     * @return ReviewProgressResponse with operation result
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Saved review progress for application {0}", entityIdParamIndex = 0)
    public ReviewProgressResponse saveReviewProgress(Long applicationId, ReviewProgressRequest request,
            String username) {
        return applicationDocumentService.saveReviewProgress(applicationId, request, username);
    }

    /**
     * Checks if all required documents for an application are approved.
     *
     * @param applicationId The application ID
     * @return true if all required documents are approved
     */
    public boolean areAllRequiredDocumentsApproved(Long applicationId) {
        return applicationDocumentService.areAllRequiredDocumentsApproved(applicationId);
    }

    /**
     * Validates status transition and document requirements.
     *
     * @param application The application
     * @param newStatus   The target status
     * @throws InvalidStatusException if transition is invalid
     */
    private void validateStatusTransition(Application application, ApplicationStatus newStatus) {
        ApplicationStatus currentStatus = application.getStatus();

        // Check if transition is allowed
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusException(
                    "Cannot transition from " + currentStatus + " to " + newStatus);
        }

        // Additional validation for AWAITING_ALLOCATION transition
        if (newStatus == AWAITING_ALLOCATION) {
            RequiredDocumentsChecklistResponse checklist = getRequiredDocumentsChecklist(
                    application.getId());
            if (!Boolean.TRUE.equals(checklist.getAllApproved())) {
                throw new InvalidStatusException(
                        "Cannot move to AWAITING_ALLOCATION: Not all required documents are approved. "
                                +
                                "Approved: " + checklist.getApproved() + "/"
                                + checklist.getTotalRequired());
            }
        }
    }
    // ==================== Enhanced Application Full Details ====================

    /**
     * Gets comprehensive application details including student profile,
     * documents with nested comments, required documents checklist,
     * and allocation eligibility status.
     * 
     * Delegates to ApplicationQueryService for complex data assembly.
     *
     * @param applicationId The application ID
     * @param username      The username of the requesting user
     * @return ApplicationFullDetailsResponse with all details
     */
    /**
     * The bare valueOf leaked the enum fully-qualified class name to the caller, because its
     * IllegalArgumentException reached the conflict handler which echoed the message (GH-149). It
     * also threw NullPointerException on a null status rather than saying what was wrong.
     */
    private static ApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new InvalidStatusException("A status is required");
        }
        try {
            return ApplicationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStatusException("Invalid application status: " + status);
        }
    }

    public ApplicationFullDetailsResponse getApplicationFullDetailsEnhanced(Long applicationId, String username) {
        return applicationQueryService.getApplicationFullDetailsEnhanced(applicationId, username);
    }
}
