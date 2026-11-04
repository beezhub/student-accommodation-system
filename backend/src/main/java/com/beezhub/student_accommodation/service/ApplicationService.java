package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.service.application.ApplicationAdminService;
import com.beezhub.student_accommodation.service.application.ApplicationCoreService;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import com.beezhub.student_accommodation.service.application.ApplicationQueryService;
import com.beezhub.student_accommodation.service.application.ApplicationSearchService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade service for Application management.
 * Delegates to specialized services following the Strangler Pattern.
 * <p>
 * This facade maintains the original public API while internally delegating
 * to decomposed services:
 * - ApplicationCoreService: Core submission and retrieval operations
 * - ApplicationSearchService: Search and filtering operations
 * - ApplicationAdminService: Admin-specific operations
 * - ApplicationDocumentService: Document-related operations
 * - ApplicationQueryService: Complex query operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationCoreService coreService;
    private final ApplicationSearchService searchService;
    private final ApplicationAdminService adminService;
    private final ApplicationDocumentService documentService;
    private final ApplicationQueryService queryService;

    // ==================== Core Operations (Student-facing) ====================

    @Timed(value = "service.application.submit", percentiles = {0.5, 0.95, 0.99})
    public ApplicationResponse submitApplication(ApplicationRequest request) {
        return coreService.submitApplication(request);
    }

    @Timed(value = "service.application.getById", percentiles = {0.5, 0.95, 0.99})
    public ApplicationResponse getApplicationById(Long applicationId) {
        return coreService.getApplicationById(applicationId);
    }

    public List<ApplicationResponse> getApplicationsByStudentId(Long studentId) {
        return coreService.getApplicationsByStudentId(studentId);
    }

    public void cancelApplication(Long applicationId) {
        coreService.cancelApplication(applicationId);
    }

    public ApplicationResponse resubmitApplication(Long applicationId) {
        return coreService.resubmitApplication(applicationId);
    }

    public List<ApplicationResponse> getMyApplications() {
        return coreService.getMyApplications();
    }

    // ==================== Search Operations (Admin) ====================

    public PageResponse<ApplicationResponse> getAllApplications(Pageable pageable) {
        return searchService.getAllApplications(pageable);
    }

    public PageResponse<ApplicationResponse> searchApplications(String applicationCode, String studentName,
            ApplicationStatus status, String allocationStatus, String preferredBuilding, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        return searchService.searchApplications(applicationCode, studentName, status, allocationStatus,
                preferredBuilding, startDate, endDate, pageable);
    }

    public List<ApplicationResponse> getApplicationsByStatus(String status) {
        return searchService.getApplicationsByStatus(status);
    }

    // ==================== Admin Operations ====================

    @Timed(value = "service.application.updateStatus", percentiles = {0.5, 0.95, 0.99})
    public ApplicationResponse updateApplicationStatus(Long applicationId,
            ApplicationStatusUpdateRequest request) {
        return adminService.updateApplicationStatus(applicationId, request);
    }

    public ApplicationResponse markUnderReview(Long applicationId) {
        return adminService.markUnderReview(applicationId);
    }

    public ApplicationResponse requestChanges(Long applicationId, String reason) {
        return adminService.requestChanges(applicationId, reason);
    }

    public ApplicationResponse moveToAllocationPhase(Long applicationId, Long adminId) {
        return adminService.moveToAllocationPhase(applicationId, adminId);
    }

    public ApplicationResponse moveToAllocationPhaseByUsername(Long applicationId, String username) {
        return adminService.moveToAllocationPhaseByUsername(applicationId, username);
    }

    public ApplicationFullDetailsResponse getApplicationFullDetailsEnhanced(Long applicationId, String username) {
        return adminService.getApplicationFullDetailsEnhanced(applicationId, username);
    }

    // ==================== Document Operations ====================

    public RequiredDocumentsChecklistResponse getRequiredDocumentsChecklist(Long applicationId) {
        return documentService.getRequiredDocumentsChecklist(applicationId);
    }

    public ReviewProgressResponse saveReviewProgress(Long applicationId, ReviewProgressRequest request,
            String username) {
        return documentService.saveReviewProgress(applicationId, request, username);
    }

    public boolean areAllRequiredDocumentsApproved(Long applicationId) {
        return documentService.areAllRequiredDocumentsApproved(applicationId);
    }

}

