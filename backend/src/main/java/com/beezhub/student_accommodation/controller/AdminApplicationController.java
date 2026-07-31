package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.service.ApplicationService;
import com.beezhub.student_accommodation.service.application.ApplicationAdminService;
import com.beezhub.student_accommodation.service.application.ApplicationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Admin-facing application management endpoints accessible to both PLATFORM_ADMIN
 * and TENANT_ADMIN. Data is automatically scoped to the caller's tenant via the
 * Hibernate {@code tenantFilter} (activated by {@code TenantFilterConfig}).
 *
 * <p>PLATFORM_ADMIN sees all applications across all tenants.
 * TENANT_ADMIN sees only applications whose {@code tenant_id} matches their own.
 */
@Tag(name = "Admin Applications", description = "Admin endpoints for managing applications (tenant-scoped)")
@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class AdminApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationSearchService applicationSearchService;
    private final ApplicationAdminService applicationAdminService;

    @Operation(summary = "List all applications", description = "Returns paginated applications scoped to the caller's tenant. PLATFORM_ADMIN sees all.")
    @GetMapping
    public ResponseEntity<PageResponse<ApplicationResponse>> getAllApplications(
            @PageableDefault(size = 20, sort = "applicationDate") Pageable pageable) {
        return ResponseEntity.ok(applicationSearchService.getAllApplications(pageable));
    }

    @Operation(summary = "Search applications", description = "Filter applications by code, student name, status, preferred building, or date range.")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ApplicationResponse>> searchApplications(
            @RequestParam(required = false) String applicationCode,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String allocationStatus,
            @RequestParam(required = false) String preferredBuilding,
            @RequestParam(required = false) @Parameter(description = "Start date YYYY-MM-DD") String startDate,
            @RequestParam(required = false) @Parameter(description = "End date YYYY-MM-DD") String endDate,
            @PageableDefault(size = 20, sort = "applicationDate") Pageable pageable) {
        LocalDate start;
        LocalDate end;
        ApplicationStatus statusEnum;
        try {
            start = startDate != null ? LocalDate.parse(startDate) : null;
            end = endDate != null ? LocalDate.parse(endDate) : null;
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            statusEnum = status != null ? ApplicationStatus.valueOf(status) : null;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(applicationSearchService.searchApplications(
                applicationCode, studentName, statusEnum, allocationStatus, preferredBuilding, start, end, pageable));
    }

    @Operation(summary = "Get application by ID")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationService.getApplicationById(applicationId));
    }

    @Operation(summary = "Get full application details")
    @GetMapping("/{applicationId}/full-details")
    public ResponseEntity<ApplicationFullDetailsResponse> getApplicationFullDetails(
            @PathVariable Long applicationId,
            Authentication authentication) {
        return ResponseEntity.ok(applicationAdminService.getApplicationFullDetailsEnhanced(
                applicationId, authentication.getName()));
    }

    @Operation(summary = "Update application status")
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResponseEntity.ok(applicationAdminService.updateApplicationStatus(applicationId, request));
    }

    @Operation(summary = "Mark application as under review")
    @PostMapping("/{applicationId}/mark-under-review")
    public ResponseEntity<ApplicationResponse> markUnderReview(@PathVariable Long applicationId) {
        return ResponseEntity.ok(applicationAdminService.markUnderReview(applicationId));
    }

    @Operation(summary = "Request changes for application")
    @PostMapping("/{applicationId}/request-changes")
    public ResponseEntity<ApplicationResponse> requestChanges(
            @PathVariable Long applicationId,
            @RequestBody java.util.Map<String, String> payload) {
        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required");
        }
        return ResponseEntity.ok(applicationAdminService.requestChanges(applicationId, reason));
    }

    @Operation(summary = "Save review progress")
    @PostMapping("/{applicationId}/save-review-progress")
    public ResponseEntity<ReviewProgressResponse> saveReviewProgress(
            @PathVariable Long applicationId,
            @Valid @RequestBody ReviewProgressRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(applicationAdminService.saveReviewProgress(
                applicationId, request, authentication.getName()));
    }

    @Operation(summary = "Move application to allocation phase")
    @PostMapping("/{applicationId}/move-to-allocation")
    public ResponseEntity<ApplicationResponse> moveToAllocationPhase(
            @PathVariable Long applicationId,
            Authentication authentication) {
        return ResponseEntity.ok(applicationAdminService.moveToAllocationPhaseByUsername(
                applicationId, authentication.getName()));
    }
}
