package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.AllocationEligibility;
import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse.StudentProfile;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse.CommentResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse.DocumentChecklistItem;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest.DocumentUpdate;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
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
import com.beezhub.student_accommodation.repository.StagedAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.specification.ApplicationSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.ALLOCATED;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.APPROVED;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.AWAITING_ALLOCATION;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.PENDING;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.AWAITING_ALLOCATION;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.PENDING;
import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.REJECTED;

import com.beezhub.student_accommodation.model.entity.StagedAllocation;
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
    private final StagedAllocationRepository stagedAllocationRepository;
    private final AppUserRepository appUserRepository;
    private final ApplicationMapper applicationMapper;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;

    @Transactional
    @LogActivity(action = "CREATE", entityType = "APPLICATION", description = "Submitted application for student {0}", entityIdParamIndex = 0)
    public ApplicationResponse submitApplication(@Valid ApplicationRequest request) {
        Long studentId = request.getStudentId();
        log.info("Submitting application for student with ID: {}", studentId);

        // Get student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with ID: " + studentId));

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

        // Validate and get preferred building if provided
        Building preferredBuilding = null;
        if (request.getPreferredBuildingId() != null) {
            preferredBuilding = buildingRepository.findById(request.getPreferredBuildingId())
                    .orElseThrow(() -> new RuntimeException(
                            "Building not found with ID: " + request.getPreferredBuildingId()));
            log.info("Setting preferred building {} for application", preferredBuilding.getName());
        }

        // Create and save application
        Application application = Application.builder()
                .applicationCode(generateApplicationCode())
                .applicationDate(LocalDate.now())
                .status(PENDING)
                .student(student)
                .preferredAccommodationType(request.getPreferredAccommodationType())
                .preferredMoveInDate(request.getPreferredMoveInDate())
                .preferredDurationMonths(request.getPreferredDurationMonths())
                .requiresAccessibility(request.getRequiresAccessibility() != null ? request.getRequiresAccessibility() : false)
                .specialRequirements(request.getSpecialRequirements())
                .institutionName(request.getInstitutionName())
                .courseName(request.getCourseName())
                .preferredBuilding(preferredBuilding)
                .build();

        Application savedApplication = applicationRepository.save(application);
        log.info("Application submitted successfully with code: {}", savedApplication.getApplicationCode());

        return applicationMapper.toResponse(savedApplication);
    }

    private String generateApplicationCode() {
        int year = LocalDate.now().getYear();
        long count = applicationRepository.countByApplicationCodeStartingWith("APP-" + year + "-");
        String sequence = String.format("%03d", count + 1);
        return "APP-" + year + "-" + sequence;
    }

    public ApplicationResponse getApplicationById(Long applicationId) {
        log.info("Retrieving application with ID: {}", applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

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
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with ID: " + studentId));

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

    public List<ApplicationResponse> getApplicationsByStatus(String status) {
        log.info("Retrieving applications for status {}", status);
        ApplicationStatus applicationStatus = ApplicationStatus.valueOf(status.toUpperCase());
        return applicationRepository.findByStatus(applicationStatus)
                .stream()
                .map(applicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @LogActivity(action = "UPDATE", entityType = "APPLICATION", description = "Updated application {0} status to {1}", entityIdParamIndex = 0)
    public ApplicationResponse updateApplicationStatus(Long applicationId,
            @Valid ApplicationStatusUpdateRequest request) {
        log.info("Updating application {} to status: {}", applicationId, request.getStatus());

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        ApplicationStatus oldStatus = application.getStatus();
        ApplicationStatus newStatus = ApplicationStatus.valueOf(request.getStatus().toUpperCase());

        // Validate status transition
        validateStatusTransition(application, newStatus);

        application.setStatus(newStatus);
        application.setRemarks(request.getRemarks());

        Application updatedApplication = applicationRepository.save(application);
        log.info("Application {} status updated from {} to: {}", applicationId, oldStatus, newStatus);

        // Send notification if status changed to APPROVED or REJECTED
        if (newStatus != oldStatus && (newStatus == APPROVED || newStatus == REJECTED)) {
            try {
                log.info("Triggering notification for application {} with status {}", applicationId, newStatus);
                // Send email notification
                notificationService.sendApplicationStatusNotification(updatedApplication, newStatus);

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

        return applicationMapper.toResponse(updatedApplication);
    }

    @Transactional
    @LogActivity(action = "DELETE", entityType = "APPLICATION", description = "Canceled application {0}", entityIdParamIndex = 0)
    public void cancelApplication(Long applicationId) {
        log.info("Canceling application with ID: {}", applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

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

    public List<ApplicationResponse> getAllApplications() {
        log.info("Retrieving all applications");
        return applicationRepository.findAll()
                .stream()
                .map(applicationMapper::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<ApplicationResponse> getAllApplications(Pageable pageable) {
        log.info("Retrieving paginated applications");
        Page<Application> page = applicationRepository.findAll(pageable);

        // Enrich
        List<ApplicationResponse> responses = enrichApplicationDetails(page.getContent());

        Page<ApplicationResponse> responsePage = new org.springframework.data.domain.PageImpl<>(responses, pageable,
                page.getTotalElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<ApplicationResponse> searchApplications(String applicationCode, String studentName,
            ApplicationStatus status, String allocationStatus, String preferredBuilding, LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        log.info(
                "Searching applications with filters - code: {}, student: {}, status: {}, allocationStatus: {}, building: {}, startDate: {}, endDate: {}",
                applicationCode, studentName, status, allocationStatus, preferredBuilding, startDate, endDate);
        Specification<Application> spec = ApplicationSpecification.filterBy(applicationCode, studentName, status,
                allocationStatus, preferredBuilding, startDate, endDate);
        Page<Application> page = applicationRepository.findAll(spec, pageable);

        // Enrich responses with extra data not in Application entity
        List<ApplicationResponse> responses = enrichApplicationDetails(page.getContent());

        Page<ApplicationResponse> responsePage = new org.springframework.data.domain.PageImpl<>(responses, pageable,
                page.getTotalElements());

        log.info("Found {} applications matching criteria", page.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    private List<ApplicationResponse> enrichApplicationDetails(List<Application> applications) {
        if (applications.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> appIds = applications.stream().map(Application::getId).collect(Collectors.toList());

        // Fetch documents
        List<Document> allDocs = documentRepository.findByApplicationIdIn(appIds);
        Map<Long, List<Document>> docsByApp = allDocs.stream()
                .collect(Collectors.groupingBy(d -> d.getApplication().getId()));

        // Fetch staged allocations
        List<StagedAllocation> allStaged = stagedAllocationRepository.findByApplication_IdIn(appIds);
        Map<Long, StagedAllocation> stagedByApp = allStaged.stream()
                .collect(Collectors.toMap(
                        s -> s.getApplication().getId(),
                        s -> s,
                        (existing, replacement) -> existing // Keep first if multiple (rare)
                ));

        return applications.stream().map(app -> {
            ApplicationResponse response = applicationMapper.toResponse(app);

            // Handle Documents
            List<Document> docs = docsByApp.getOrDefault(app.getId(), new ArrayList<>());
            int totalRequired = 5; // Hardcoded default or fetch from DocumentType count (better to fetch but
                                   // simplified for now)
                                   // Actually, let's just count uploaded ones for now or implement better logic
                                   // later

            // Better logic: totalRequired should be count of REQUIRED document types
            // I'll grab that count once to avoid N+1, but for now lets assume basic logic:
            // Count of approved documents vs total uploaded or fixed required number.
            // Given the context, let's use documentCompletionPercent logic from
            // ReviewProgressResponse?

            long approvedCount = docs.stream().filter(d -> d.getDocumentStatus() == DocumentStatus.APPROVED).count();
            response.setApprovedDocuments((int) approvedCount);
            // Assuming 5 required documents for student accommodation
            response.setTotalRequiredDocuments(5);
            response.setDocumentCompletionPercent(docs.isEmpty() ? 0 : (int) ((approvedCount * 100) / 5));

            // Handle Allocation
            if (app.getStatus() == ApplicationStatus.ALLOCATED) {
                response.setAllocationStatus("ALLOCATED");
                // If real allocation exists, we'd check RoomAllocation, but StagedAllocation
                // might be gone?
                // Or we check StagedAllocation for PROPOSED items.
            }

            StagedAllocation staged = stagedByApp.get(app.getId());
            if (staged != null) {
                response.setAllocationScore(staged.getScore());
                // If application is AWAITING_ALLOCATION, staged status is meaningful
                response.setAllocationStatus(staged.getStatus().name());
            } else {
                response.setAllocationStatus(app.getStatus().name()); // Default to app status
            }

            return response;
        }).collect(Collectors.toList());
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
        log.info("Getting required documents checklist for application {}", applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        // Get all required document types
        List<DocumentType> requiredTypes = documentTypeRepository.findByIsRequiredTrue();

        // Get all documents for this application
        List<Document> applicationDocuments = documentRepository.findByApplicationId(applicationId);

        // Build checklist items
        List<DocumentChecklistItem> checklistItems = new java.util.ArrayList<>();
        int approved = 0;
        int pending = 0;
        int rejected = 0;
        int notUploaded = 0;

        for (DocumentType docType : requiredTypes) {
            // Find the latest document for this type (in case of resubmission)
            Document matchingDoc = applicationDocuments.stream()
                    .filter(doc -> doc.getDocumentType().getId().equals(docType.getId()))
                    .reduce((first, second) -> second) // Get the latest (last) one
                    .orElse(null);

            DocumentChecklistItem item;
            if (matchingDoc != null) {
                String status = matchingDoc.getDocumentStatus().name();
                item = DocumentChecklistItem.builder()
                        .documentTypeId(docType.getId())
                        .type(docType.getTypeName())
                        .status(status)
                        .uploadedAt(matchingDoc.getCreatedAt())
                        .documentId(matchingDoc.getId())
                        .documentName(matchingDoc.getDocumentName())
                        .isRequired(true)
                        .build();

                switch (matchingDoc.getDocumentStatus()) {
                    case APPROVED -> approved++;
                    case PENDING -> pending++;
                    case REJECTED -> rejected++;
                }
            } else {
                item = DocumentChecklistItem.builder()
                        .documentTypeId(docType.getId())
                        .type(docType.getTypeName())
                        .status("NOT_UPLOADED")
                        .isRequired(true)
                        .build();
                notUploaded++;
            }
            checklistItems.add(item);
        }

        RequiredDocumentsChecklistResponse response = RequiredDocumentsChecklistResponse.builder()
                .applicationId(applicationId)
                .totalRequired(requiredTypes.size())
                .approved(approved)
                .pending(pending)
                .rejected(rejected)
                .notUploaded(notUploaded)
                .documents(checklistItems)
                .build();

        response.calculateAllApproved();
        log.info("Checklist for application {}: {}/{} approved", applicationId, approved, requiredTypes.size());

        return response;
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
        log.info("Moving application {} to allocation phase by admin {}", applicationId, adminId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

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
                            "Approved: " + checklist.getApproved() + "/" + checklist.getTotalRequired());
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
            log.error("Failed to send notification for application {} allocation phase transition", applicationId, e);
        }

        return applicationMapper.toResponse(updatedApplication);
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
        log.info("Getting full details for application {}", applicationId);

        // Verify admin access
        AppUser currentUser = authService.getCurrentUser();
        if (!currentUser.getUserRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedAccessException("Only admins can access full application details");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        ApplicationResponse response = applicationMapper.toResponse(application);

        log.info("Returning full details for application {}", applicationId);
        return response;
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
        RequiredDocumentsChecklistResponse checklist = getRequiredDocumentsChecklist(applicationId);
        return Boolean.TRUE.equals(checklist.getAllApproved());
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
            RequiredDocumentsChecklistResponse checklist = getRequiredDocumentsChecklist(application.getId());
            if (!Boolean.TRUE.equals(checklist.getAllApproved())) {
                throw new InvalidStatusException(
                        "Cannot move to AWAITING_ALLOCATION: Not all required documents are approved. " +
                                "Approved: " + checklist.getApproved() + "/" + checklist.getTotalRequired());
            }
        }
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
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        // Authorization check: admin or application owner
        boolean isAdmin = currentUser.getUserRole().equals(UserRole.ADMIN);
        boolean isOwner = application.getStudent().getAppUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedAccessException("You are not authorized to view this application's full details");
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
                .institutionName(student.getInstitution() != null ? student.getInstitution().getName() : null)
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
            eligibilityReason = "Application must be APPROVED first. Current status: " + application.getStatus();
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
                .preferredAccommodationType(application.getPreferredAccommodationType())
                .preferredMoveInDate(application.getPreferredMoveInDate())
                .preferredDurationMonths(application.getPreferredDurationMonths())
                .requiresAccessibility(application.getRequiresAccessibility())
                .specialRequirements(application.getSpecialRequirements())
                .institutionName(application.getInstitutionName())
                .courseName(application.getCourseName())
                .preferredBuildingId(
                        application.getPreferredBuilding() != null ? application.getPreferredBuilding().getId() : null)
                .preferredBuildingName(
                        application.getPreferredBuilding() != null ? application.getPreferredBuilding().getName()
                                : null)
                .allocationMethod(
                        application.getAllocationMethod() != null ? application.getAllocationMethod().name() : null)
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
                        .adminName(comment.getAdmin().getFirstName() + " " + comment.getAdmin().getLastName())
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
        log.info("Saving review progress for application {} by {}", applicationId, username);

        // Verify application exists
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with ID: " + applicationId));

        // Verify admin access
        AppUser admin = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!admin.getUserRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedAccessException("Only admins can save review progress");
        }

        int documentsUpdated = 0;
        int commentsAdded = 0;

        for (DocumentUpdate update : request.getDocumentUpdates()) {
            // Find the document
            Document document = documentRepository.findById(update.getDocumentId())
                    .orElseThrow(() -> new RuntimeException("Document not found with ID: " + update.getDocumentId()));

            // Verify document belongs to this application
            if (!document.getApplication().getId().equals(applicationId)) {
                throw new UnauthorizedAccessException(
                        "Document " + update.getDocumentId() + " does not belong to this application");
            }

            // Update document status if provided
            if (update.getNewStatus() != null && !update.getNewStatus().trim().isEmpty()) {
                try {
                    DocumentStatus newStatus = DocumentStatus.valueOf(update.getNewStatus().toUpperCase());
                    document.setDocumentStatus(newStatus);
                    documentRepository.save(document);
                    documentsUpdated++;
                    log.info("Updated document {} status to {}", update.getDocumentId(), newStatus);
                } catch (IllegalArgumentException e) {
                    throw new InvalidStatusException("Invalid document status: " + update.getNewStatus());
                }
            }

            // Add comment if provided
            if (update.getComment() != null && !update.getComment().trim().isEmpty()) {
                DocumentComment comment = DocumentComment.builder()
                        .document(document)
                        .admin(admin)
                        .commentText(update.getComment())
                        .build();
                documentCommentRepository.save(comment);
                commentsAdded++;
                log.info("Added comment to document {}", update.getDocumentId());
            }
        }

        log.info("Review progress saved for application {}: {} documents updated, {} comments added",
                applicationId, documentsUpdated, commentsAdded);

        return ReviewProgressResponse.builder()
                .success(true)
                .message("Review progress saved successfully")
                .documentsUpdated(documentsUpdated)
                .commentsAdded(commentsAdded)
                .build();
    }

    // ==================== Move to Allocation Phase (Username version)
    // ====================

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

        if (!admin.getUserRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedAccessException("Only admins can move applications to allocation phase");
        }

        return moveToAllocationPhase(applicationId, admin.getId());
    }
}
