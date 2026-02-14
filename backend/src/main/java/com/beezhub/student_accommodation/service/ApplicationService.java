package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.ApplicationWindowClosedException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException;
import com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.AllocationEligibility;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.StudentProfile;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.service.application.ApplicationAdminService;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse.CommentResponse;

import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse.DocumentChecklistItem;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;

import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.entity.DocumentComment;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentCommentRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;

import com.beezhub.student_accommodation.repository.StudentRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.ALLOCATED;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.APPROVED;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.AWAITING_ALLOCATION;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.CHANGES_REQUESTED;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.PENDING;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.REJECTED;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.UNDER_REVIEW;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RequiredArgsConstructor
@Slf4j
@Service
public class ApplicationService {

        private final ApplicationRepository applicationRepository;
        private final StudentRepository studentRepository;
        private final BuildingRepository buildingRepository;
        private final DocumentRepository documentRepository;
        private final DocumentTypeRepository documentTypeRepository;
        private final DocumentCommentRepository documentCommentRepository;

        private final AppUserRepository appUserRepository;
        private final ApplicationMapper applicationMapper;
        private final AuthService authService;
        private final NotificationService notificationService;
        private final UserNotificationService userNotificationService;
        private final AcademicYearService academicYearService;
        private final RoomAllocationService roomAllocationService;
        private final AllocationScoringService allocationScoringService;
        private final SystemSettingService systemSettingService;
        @Lazy
        private final ApplicationAdminService applicationAdminService;

        @Transactional
        @LogActivity(action = "CREATE", entityType = "APPLICATION", description = "Submitted application for student {0}", entityIdParamIndex = 0)
        public ApplicationResponse submitApplication(@Valid ApplicationRequest request) {
                Long studentId = request.getStudentId();
                log.info("Submitting application for student with ID: {}", studentId);

                // Get student
                Student student = studentRepository.findStudentWithDetailsById(studentId)
                                .orElseThrow(() -> new StudentNotFoundException(
                                                "Student not found with ID: " + studentId));

                // Verify authorization - student can only submit for themselves
                AppUser currentUser = authService.getCurrentUser();

                if (!student.getAppUser().getId().equals(currentUser.getId()) &&
                                !currentUser.getUserRole().equals(UserRole.ADMIN)) {
                        throw new UnauthorizedAccessException("You can only submit applications for yourself");
                }

                // Check for existing pending application
                if (applicationRepository.existsByStudent_IdAndStatus(studentId, PENDING)) {
                        throw new DuplicateApplicationException("Student already has a pending application");
                }

                // Validate application window is open
                com.beezhub.student_accommodation.model.entity.AcademicYear currentYear = academicYearService
                                .getCurrentAcademicYearEntity();
                if (!academicYearService.isApplicationWindowOpen(currentYear)) {
                        throw new ApplicationWindowClosedException(
                                        "Applications are not currently open. Window: " +
                                                        currentYear.getApplicationOpenDate() + " to " +
                                                        currentYear.getApplicationCloseDate());
                }

                // Validate and get preferred building if provided
                Building preferredBuilding = null;
                if (request.getPreferredBuildingId() != null) {
                        preferredBuilding = buildingRepository.findById(request.getPreferredBuildingId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Building not found with ID: "
                                                                        + request.getPreferredBuildingId()));
                        log.info("Setting preferred building {} for application", preferredBuilding.getName());
                }

                // Create and save application
                Application application = Application.builder()
                                .applicationCode(generateApplicationCode())
                                .applicationDate(LocalDate.now())
                                .status(PENDING)
                                .student(student)
                                .requiresAccessibility(
                                                request.getRequiresAccessibility() != null
                                                                ? request.getRequiresAccessibility()
                                                                : false)
                                .specialRequirements(request.getSpecialRequirements())
                                .institutionName(request.getInstitutionName())
                                .courseName(request.getCourseName())
                                .preferredBuilding(preferredBuilding)
                                .academicYear(currentYear)
                                .build();

                Application savedApplication = applicationRepository.save(application);
                log.info("Application submitted successfully with code: {}", savedApplication.getApplicationCode());

                return applicationMapper.toResponse(savedApplication);
        }

        private String generateApplicationCode() {
                int year = LocalDate.now().getYear();
                String prefix = "APP-" + year + "-";
                long nextSequence = applicationRepository.findMaxApplicationCodeByPrefix(prefix)
                                .map(maxCode -> {
                                        String sequencePart = maxCode.substring(prefix.length());
                                        return Long.parseLong(sequencePart) + 1;
                                })
                                .orElse(1L);
                return prefix + String.format("%03d", nextSequence);
        }

        public ApplicationResponse getApplicationById(Long applicationId) {
                log.info("Retrieving application with ID: {}", applicationId);

                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                // Verify authorization
                AppUser currentUser = authService.getCurrentUser();

                if (!application.getStudent().getAppUser().getId().equals(currentUser.getId()) &&
                                !currentUser.getUserRole().equals(UserRole.ADMIN)) {
                        throw new UnauthorizedAccessException("You are not authorized to view this application");
                }

                return applicationMapper.toResponse(application);
        }

        public List<ApplicationResponse> getApplicationsByStudentId(Long studentId) {
                log.info("Retrieving applications for student with ID: {}", studentId);

                // Verify student exists
                Student student = studentRepository.findStudentWithUserById(studentId)
                                .orElseThrow(() -> new StudentNotFoundException(
                                                "Student not found with ID: " + studentId));

                // Verify authorization
                AppUser currentUser = authService.getCurrentUser();

                if (!student.getAppUser().getId().equals(currentUser.getId()) &&
                                !currentUser.getUserRole().equals(UserRole.ADMIN)) {
                        throw new UnauthorizedAccessException("You are not authorized to view these applications");
                }

                return applicationRepository.findByStudent_Id(studentId)
                                .stream()
                                .map(applicationMapper::toResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        @LogActivity(action = "DELETE", entityType = "APPLICATION", description = "Canceled application {0}", entityIdParamIndex = 0)
        public void cancelApplication(Long applicationId) {
                log.info("Canceling application with ID: {}", applicationId);

                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                // Verify authorization
                AppUser currentUser = authService.getCurrentUser();

                if (!application.getStudent().getAppUser().getId().equals(currentUser.getId())) {
                        throw new UnauthorizedAccessException("You can only cancel your own applications");
                }

                // Only pending applications can be canceled
                if (!application.getStatus().equals(PENDING)) {
                        throw new IllegalStateException("Only pending applications can be canceled");
                }

                applicationRepository.delete(application);
                log.info("Application {} canceled successfully", applicationId);
        }

        public List<ApplicationResponse> getMyApplications() {
                log.info("Retrieving applications for current user");

                AppUser currentUser = authService.getCurrentUser();

                Student student = studentRepository.findByAppUser_Id(currentUser.getId())
                                .orElseThrow(() -> new StudentNotFoundException("Student profile not found for user"));

                return applicationRepository.findByStudent_Id(student.getId())
                                .stream()
                                .map(applicationMapper::toResponse)
                                .collect(Collectors.toList());
        }

        // ==================== Admin Operations (Delegated) ====================

        /**
         * Delegates to ApplicationAdminService for backward compatibility.
         * 
         * @deprecated Use ApplicationAdminService.updateApplicationStatus directly
         */
        @Deprecated
        public ApplicationResponse updateApplicationStatus(Long applicationId,
                        @Valid ApplicationStatusUpdateRequest request) {
                return applicationAdminService.updateApplicationStatus(applicationId, request);
        }

        /**
         * Delegates to ApplicationAdminService for backward compatibility.
         * 
         * @deprecated Use ApplicationAdminService.markUnderReview directly
         */
        @Deprecated
        public ApplicationResponse markUnderReview(Long applicationId) {
                return applicationAdminService.markUnderReview(applicationId);
        }

        /**
         * Delegates to ApplicationAdminService for backward compatibility.
         * 
         * @deprecated Use ApplicationAdminService.requestChanges directly
         */
        @Deprecated
        public ApplicationResponse requestChanges(Long applicationId, String reason) {
                return applicationAdminService.requestChanges(applicationId, reason);
        }

        /**
         * Delegates to ApplicationAdminService for backward compatibility.
         * 
         * @deprecated Use ApplicationAdminService.saveReviewProgress directly
         */
        @Deprecated
        public ReviewProgressResponse saveReviewProgress(Long applicationId, ReviewProgressRequest request,
                        String username) {
                return applicationAdminService.saveReviewProgress(applicationId, request, username);
        }

        /**
         * Delegates to ApplicationAdminService for backward compatibility.
         * 
         * @deprecated Use ApplicationAdminService.moveToAllocationPhaseByUsername
         *             directly
         */
        @Deprecated
        public ApplicationResponse moveToAllocationPhaseByUsername(Long applicationId, String username) {
                return applicationAdminService.moveToAllocationPhaseByUsername(applicationId, username);
        }

        // ==================== Required Documents Checklist ====================

        /**
         * Gets the required documents checklist for an application.
         * Cross-checks required document types against student's uploaded documents.
         *
         * @param applicationId The application ID
         * @return RequiredDocumentsChecklistResponse with document status breakdown
         */
        public RequiredDocumentsChecklistResponse getRequiredDocumentsChecklist(Long applicationId) {
                return applicationAdminService.getRequiredDocumentsChecklist(applicationId);
        }

        // ==================== Allocation Phase Transition ====================

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
                return applicationAdminService.moveToAllocationPhase(applicationId, adminId);
        }

        // ==================== Application Full Details ====================

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
                return applicationAdminService.getApplicationFullDetails(applicationId);
        }

        // ==================== Validate Document Requirements ====================

        /**
         * Validates that all required documents are approved before allowing
         * transition to AWAITING_ALLOCATION status.
         *
         * @param applicationId The application ID
         * @return true if all required documents are approved
         */
        public boolean areAllRequiredDocumentsApproved(Long applicationId) {
                return applicationAdminService.areAllRequiredDocumentsApproved(applicationId);
        }

        // ==================== Enhanced Application Full Details ====================

        /**
         * Gets comprehensive application details including student profile,
         * documents with nested comments, required documents checklist,
         * and allocation eligibility status.
         *
         * @param applicationId The application ID
         * @param username      The username of the requesting user
         * @return ApplicationFullDetailsResponse with all details
         */
        public ApplicationFullDetailsResponse getApplicationFullDetailsEnhanced(Long applicationId, String username) {
                log.info("Getting enhanced full details for application {} by user {}", applicationId, username);

                AppUser currentUser = authService.getCurrentUser();
                Application application = applicationRepository.findById(applicationId)
                                .orElseThrow(() -> new ApplicationNotFoundException(
                                                "Application not found with ID: " + applicationId));

                // Authorization check: admin or application owner
                boolean isAdmin = currentUser.getUserRole().equals(UserRole.ADMIN);
                boolean isOwner = application.getStudent().getAppUser().getId().equals(currentUser.getId());

                if (!isAdmin && !isOwner) {
                        throw new UnauthorizedAccessException(
                                        "You are not authorized to view this application's full details");
                }

                Student student = application.getStudent();
                AppUser studentUser = student.getAppUser();

                // Build student profile
                StudentProfile studentProfile = StudentProfile.builder()
                                .id(student.getId())
                                .studentNumber(student.getStudentNumber())
                                .firstName(studentUser.getFirstName())
                                .lastName(studentUser.getLastName())
                                .fullName(studentUser.getFirstName() + " " + studentUser.getLastName())
                                .email(studentUser.getEmail())
                                .phoneNumber(student.getPhoneNumber())
                                .gender(student.getGender())
                                .specialRequirements(student.getSpecialRequirements())
                                .institutionName(student.getInstitution() != null ? student.getInstitution().getName()
                                                : null)
                                .yearOfStudy(student.getYearOfStudy().getYearOfStudy())
                                .build();

                // Get documents with comments for this application
                List<Document> applicationDocuments = documentRepository.findByApplicationId(applicationId);
                List<DocumentWithCommentsResponse> documentsWithComments = applicationDocuments.stream()
                                .map(this::mapDocumentWithComments)
                                .collect(Collectors.toList());

                // Get required documents checklist
                RequiredDocumentsChecklistResponse checklist = getRequiredDocumentsChecklist(applicationId);

                // Calculate allocation eligibility
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

                AllocationEligibility allocationEligibility = AllocationEligibility.builder()
                                .eligible(eligible)
                                .applicationApproved(applicationApproved)
                                .allDocumentsApproved(allDocsApproved)
                                .reason(eligibilityReason)
                                .build();

                // Build response
                ApplicationFullDetailsResponse response = ApplicationFullDetailsResponse.builder()
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

                log.info("Returning enhanced full details for application {}", applicationId);
                return response;
        }

        /**
         * Maps a Document entity to DocumentWithCommentsResponse including nested
         * comments.
         */
        private DocumentWithCommentsResponse mapDocumentWithComments(Document document) {
                List<DocumentComment> comments = documentCommentRepository
                                .findByDocument_IdOrderByCreatedAtDesc(document.getId());

                List<CommentResponse> commentResponses = comments.stream()
                                .map(comment -> CommentResponse.builder()
                                                .id(comment.getId())
                                                .adminName(comment.getAdmin().getFirstName() + " "
                                                                + comment.getAdmin().getLastName())
                                                .text(comment.getCommentText())
                                                .createdAt(comment.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());

                return DocumentWithCommentsResponse.builder()
                                .id(document.getId())
                                .fileName(document.getDocumentName())
                                .type(document.getDocumentType().getTypeName())
                                .status(document.getDocumentStatus().name())
                                .uploadedAt(document.getCreatedAt())
                                .documentTypeId(document.getDocumentType().getId())
                                .documentTypeName(document.getDocumentType().getTypeName())
                                .comments(commentResponses)
                                .build();
        }

        // ==================== Save Review Progress ====================

        // ==================== Auto-Allocation ====================

        /**
         * Attempts to automatically allocate a room for an approved application.
         *
         * This method is triggered when an application status changes to APPROVED
         * and the auto-allocation feature flag is enabled.
         *
         * Flow:
         * 1. Check if auto-allocation feature is enabled
         * 2. Validate all required documents are approved
         * 3. Score available rooms using AllocationScoringService
         * 4. Create room allocation for the best-matching room
         * 5. Fall back to AWAITING_ALLOCATION status if no rooms available
         *
         * If auto-allocation fails for any reason, the application can be
         * manually allocated by an admin.
         *
         * @param application The approved application to allocate
         */
        public void attemptAutoAllocation(Application application) {
                log.info("Attempting auto-allocation for application {}", application.getId());

                // Check if auto-allocation is enabled
                if (!systemSettingService.isAutoAllocationEnabled()) {
                        log.info("Auto-allocation feature is disabled, skipping auto-allocation for application {}",
                                        application.getId());
                        return;
                }

                // Check if all required documents are approved
                if (!areAllRequiredDocumentsApproved(application.getId())) {
                        log.info("Documents not yet approved for application {}, setting status to AWAITING_ALLOCATION",
                                        application.getId());
                        application.setStatus(AWAITING_ALLOCATION);
                        applicationRepository.save(application);
                        return;
                }

                // Get student and preferred building
                Student student = application.getStudent();
                Building preferredBuilding = application.getPreferredBuilding();

                // Score available rooms
                List<com.beezhub.student_accommodation.model.dto.ScoredRoom> scoredRooms = allocationScoringService
                                .scoreAvailableRooms(student, application, preferredBuilding);

                // Check if any rooms are available
                if (scoredRooms.isEmpty()) {
                        log.info("No rooms available for application {}, adding to waiting list", application.getId());

                        // Set status to AWAITING_ALLOCATION
                        application.setStatus(AWAITING_ALLOCATION);
                        applicationRepository.save(application);

                        // Add to waiting list
                        try {
                                roomAllocationService.enqueueWaitingList(student.getId(), application.getId());
                                log.info("Added student {} to waiting list for application {}", student.getId(),
                                                application.getId());

                                // Send waiting list notification
                                Long studentUserId = student.getAppUser().getId();
                                userNotificationService.createWaitingListNotification(studentUserId,
                                                application.getId());
                        } catch (Exception e) {
                                log.error("Failed to add student {} to waiting list for application {}",
                                                student.getId(),
                                                application.getId(), e);
                        }
                        return;
                }

                // Get the highest scored room (first in list)
                com.beezhub.student_accommodation.model.dto.ScoredRoom bestRoom = scoredRooms.get(0);
                log.info("Best match for application {}: Room {} with score {}", application.getId(),
                                bestRoom.getRoomNumber(), bestRoom.getTotalScore());

                // Create room allocation request
                RoomAllocationRequest allocationRequest = new RoomAllocationRequest();
                allocationRequest.setStudentId(student.getId());
                allocationRequest.setRoomId(bestRoom.getRoomId());
                allocationRequest.setApplicationId(application.getId());

                try {
                        // Create the allocation
                        roomAllocationService.createRoomAllocation(allocationRequest);

                        // Set allocation method to AUTO
                        application.setAllocationMethod(
                                        com.beezhub.student_accommodation.model.enums.AllocationMethod.AUTO);
                        applicationRepository.save(application);

                        log.info("Successfully auto-allocated room {} to student {} for application {} with score {}",
                                        bestRoom.getRoomNumber(), student.getId(), application.getId(),
                                        bestRoom.getTotalScore());

                } catch (RoomCapacityExceededException | StudentAlreadyAllocatedException e) {
                        // Race condition - room became full or student already has allocation
                        log.warn(
                                        "Auto-allocation failed for application {} due to: {}. Adding to waiting list instead.",
                                        application.getId(), e.getMessage());

                        // Move to AWAITING_ALLOCATION and add to waiting list
                        application.setStatus(AWAITING_ALLOCATION);
                        applicationRepository.save(application);

                        try {
                                roomAllocationService.enqueueWaitingList(student.getId(), application.getId());
                                Long studentUserId = student.getAppUser().getId();
                                userNotificationService.createWaitingListNotification(studentUserId,
                                                application.getId());
                        } catch (Exception waitingListError) {
                                log.error("Failed to add student {} to waiting list after allocation failure",
                                                student.getId(), waitingListError);
                        }

                } catch (Exception e) {
                        // Unexpected error - log and set to AWAITING_ALLOCATION for manual handling
                        log.error("Unexpected error during auto-allocation for application {}, will require manual allocation",
                                        application.getId(), e);

                        application.setStatus(AWAITING_ALLOCATION);
                        applicationRepository.save(application);
                }
        }

        // ==================== Move to Allocation Phase (Username version)
        // ====================

}
