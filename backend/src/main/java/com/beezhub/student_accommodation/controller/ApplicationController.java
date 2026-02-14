package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ApplicationFullDetailsResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import org.springframework.security.core.Authentication;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.service.ApplicationService;
import com.beezhub.student_accommodation.service.application.ApplicationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.valueOf;

@Tag(name = "Application", description = "Endpoints for student accommodation applications")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
        private final ApplicationService applicationService;
        private final ApplicationSearchService applicationSearchService;

        @Operation(summary = "Submit a student accommodation application", description = "Submits an application for accommodation. Students can only submit for themselves.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Application submitted successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized to submit for this student"),
                        @ApiResponse(responseCode = "404", description = "Student not found"),
                        @ApiResponse(responseCode = "409", description = "Student already has a pending application")
        })
        @PostMapping("/submit")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApplicationResponse> submitApplication(
                        @Valid @RequestBody ApplicationRequest request) {
                ApplicationResponse response = applicationService.submitApplication(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @Operation(summary = "Get my applications", description = "Retrieves all applications for the currently authenticated student.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Student profile not found")
        })
        @GetMapping("/my-applications")
        @PreAuthorize("hasRole('STUDENT')")
        public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
                return ResponseEntity.ok(applicationService.getMyApplications());
        }

        @Operation(summary = "Get application by ID", description = "Retrieves a specific application by its ID. Students can only view their own applications.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Application retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized to view this application"),
                        @ApiResponse(responseCode = "404", description = "Application not found")
        })
        @GetMapping("/{applicationId}")
        @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
        public ResponseEntity<ApplicationResponse> getApplicationById(
                        @Parameter(description = "ID of the application to retrieve", required = true) @PathVariable("applicationId") Long applicationId) {
                ApplicationResponse response = applicationService.getApplicationById(applicationId);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get all applications for a student", description = "Retrieves all applications for a specific student. Students can only view their own applications.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized to view these applications"),
                        @ApiResponse(responseCode = "404", description = "Student not found")
        })
        @GetMapping("/student/{studentId}")
        @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
        public ResponseEntity<List<ApplicationResponse>> getApplicationsByStudentId(
                        @Parameter(description = "ID of the student to retrieve applications for", required = true) @PathVariable("studentId") Long studentId) {
                return ResponseEntity.ok(applicationService.getApplicationsByStudentId(studentId));
        }

        @Operation(summary = "Get all applications", description = "Retrieves all applications in the system. Admin only. Supports pagination with query params: page, size, sort")
        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully")
        @GetMapping
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<?> getAllApplications(
                        @PageableDefault(size = 20, sort = "applicationDate") Pageable pageable) {
                PageResponse<ApplicationResponse> page = applicationSearchService.getAllApplications(pageable);
                return ResponseEntity.ok(page);
        }

        @Operation(summary = "Search applications", description = "Search and filter applications by code, student name, status, allocation status, preferred building, or date range. Admin only.")
        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully")
        @GetMapping("/search")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<PageResponse<ApplicationResponse>> searchApplications(
                        @RequestParam(required = false) String applicationCode,
                        @RequestParam(required = false) String studentName,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String allocationStatus,
                        @RequestParam(required = false) String preferredBuilding,
                        @RequestParam(required = false) @Parameter(description = "Start date in format YYYY-MM-DD") String startDate,
                        @RequestParam(required = false) @Parameter(description = "End date in format YYYY-MM-DD") String endDate,
                        @PageableDefault(size = 20, sort = "applicationDate") Pageable pageable) {
                LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
                LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
                ApplicationStatus statusEnum = status != null ? valueOf(status) : null;
                PageResponse<ApplicationResponse> page = applicationSearchService.searchApplications(
                                applicationCode, studentName, statusEnum, allocationStatus, preferredBuilding, start,
                                end, pageable);
                return ResponseEntity.ok(page);
        }

        @Operation(summary = "Get all applications by status", description = "Retrieves all applications with the specified status. Admin only.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid status")
        })
        @GetMapping("/status/{status}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<List<ApplicationResponse>> getApplicationsByStatus(
                        @Parameter(description = "Status of the applications to retrieve (PENDING, APPROVED, REJECTED)", required = true) @PathVariable("status") String status) {
                return ResponseEntity.ok(applicationSearchService.getApplicationsByStatus(status));
        }

        @Operation(summary = "Update application status", description = "Updates the status of an application. Admin only.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Application status updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid status"),
                        @ApiResponse(responseCode = "404", description = "Application not found")
        })
        @PatchMapping("/{applicationId}/status")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApplicationResponse> updateApplicationStatus(
                        @Parameter(description = "ID of the application to update", required = true) @PathVariable("applicationId") Long applicationId,
                        @Valid @RequestBody ApplicationStatusUpdateRequest request) {
                ApplicationResponse response = applicationService.updateApplicationStatus(
                                applicationId, request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/{applicationId}/mark-under-review")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Mark application as under review", description = "Moves application to UNDER_REVIEW status. Admin only.")
        public ResponseEntity<ApplicationResponse> markUnderReview(
                        @Parameter(description = "ID of the application", required = true) @PathVariable("applicationId") Long applicationId) {
                return ResponseEntity.ok(applicationService.markUnderReview(applicationId));
        }

        @PostMapping("/{applicationId}/request-changes")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Request changes for application", description = "Moves application to CHANGES_REQUESTED status with a reason. Admin only.")
        public ResponseEntity<ApplicationResponse> requestChanges(
                        @Parameter(description = "ID of the application", required = true) @PathVariable("applicationId") Long applicationId,
                        @RequestBody java.util.Map<String, String> payload) {
                String reason = payload.get("reason");
                if (reason == null || reason.trim().isEmpty()) {
                        throw new IllegalArgumentException("Reason is required");
                }
                return ResponseEntity.ok(applicationService.requestChanges(applicationId, reason));
        }

        @Operation(summary = "Cancel application", description = "Cancels a pending application. Students can only cancel their own applications.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Application canceled successfully"),
                        @ApiResponse(responseCode = "400", description = "Application cannot be canceled (not pending)"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized to cancel this application"),
                        @ApiResponse(responseCode = "404", description = "Application not found")
        })
        @DeleteMapping("/{applicationId}")
        @PreAuthorize("hasRole('STUDENT')")
        public ResponseEntity<Void> cancelApplication(
                        @Parameter(description = "ID of the application to cancel", required = true) @PathVariable("applicationId") Long applicationId) {
                applicationService.cancelApplication(applicationId);
                return ResponseEntity.noContent().build();
        }

        // ==================== Full Details, Review Progress, and Allocation Endpoints
        // ====================

        @Operation(summary = "Get application full details", description = "Retrieves comprehensive application details including student profile, "
                        +
                        "documents with nested comments, required documents checklist, and allocation eligibility. " +
                        "Accessible by admins or the application owner.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Full details retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized to view this application"),
                        @ApiResponse(responseCode = "404", description = "Application not found")
        })
        @GetMapping("/{applicationId}/full-details")
        @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
        public ResponseEntity<ApplicationFullDetailsResponse> getApplicationFullDetails(
                        @Parameter(description = "ID of the application", required = true) @PathVariable("applicationId") Long applicationId,
                        Authentication authentication) {
                ApplicationFullDetailsResponse response = applicationService.getApplicationFullDetailsEnhanced(
                                applicationId, authentication.getName());
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Save review progress", description = "Saves document review progress atomically. Updates document statuses and adds comments "
                        +
                        "without completing the review or moving to the next phase. Admin only.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Review progress saved successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid request or document status"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized - admin only"),
                        @ApiResponse(responseCode = "404", description = "Application or document not found")
        })
        @PostMapping("/{applicationId}/save-review-progress")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ReviewProgressResponse> saveReviewProgress(
                        @Parameter(description = "ID of the application", required = true) @PathVariable("applicationId") Long applicationId,
                        @Valid @RequestBody ReviewProgressRequest request,
                        Authentication authentication) {
                ReviewProgressResponse response = applicationService.saveReviewProgress(
                                applicationId, request, authentication.getName());
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Move application to allocation phase", description = "Moves an application to the allocation phase (AWAITING_ALLOCATION status). "
                        +
                        "Validates that the application is APPROVED and all required documents are approved. Admin only.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Application moved to allocation phase"),
                        @ApiResponse(responseCode = "400", description = "Cannot move to allocation - requirements not met"),
                        @ApiResponse(responseCode = "403", description = "Unauthorized - admin only"),
                        @ApiResponse(responseCode = "404", description = "Application not found")
        })
        @PostMapping("/{applicationId}/move-to-allocation")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApplicationResponse> moveToAllocationPhase(
                        @Parameter(description = "ID of the application", required = true) @PathVariable("applicationId") Long applicationId,
                        Authentication authentication) {
                ApplicationResponse response = applicationService.moveToAllocationPhaseByUsername(
                                applicationId, authentication.getName());
                return ResponseEntity.ok(response);
        }
}
