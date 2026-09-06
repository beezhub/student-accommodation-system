package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ActivityLogPageResponse;
import com.beezhub.student_accommodation.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/activity-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Activity Logs", description = "Admin APIs for viewing system activity logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @Operation(
            summary = "Get activity logs with filters",
            description = "Retrieves paginated activity logs with optional filters. Admin only."
    )
    @ApiResponse(responseCode = "200", description = "Activity logs retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping
    public ResponseEntity<ActivityLogPageResponse> getActivityLogs(
            @Parameter(description = "User ID filter")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "User role filter (ADMIN, STUDENT, LANDLORD)")
            @RequestParam(required = false) String userRole,

            @Parameter(description = "Action filter (CREATE, UPDATE, DELETE, LOGIN, LOGOUT, APPROVE, REJECT, etc.)")
            @RequestParam(required = false) String action,

            @Parameter(description = "Entity type filter (APPLICATION, DOCUMENT, USER, ROOM, PAYMENT, etc.)")
            @RequestParam(required = false) String entityType,

            @Parameter(description = "Start date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("GET /api/admin/activity-logs - page: {}, size: {}, filters: userRole={}, action={}, entityType={}",
                page, size, userRole, action, entityType);

        ActivityLogPageResponse response = activityLogService.getActivityLogs(
                userId, userRole, action, entityType, startDate, endDate, page, size
        );

        log.info("Retrieved {} activity logs (page {}/{})",
                response.getContent().size(), page, response.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get activity log by ID",
            description = "Retrieves a specific activity log by its ID. Admin only."
    )
    @ApiResponse(responseCode = "200", description = "Activity log retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Activity log not found")
    @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<com.beezhub.student_accommodation.model.dto.ActivityLogResponse> getActivityLogById(
            @Parameter(description = "Activity log ID")
            @PathVariable Long id
    ) {
        log.debug("GET /api/admin/activity-logs/{}", id);
        return ResponseEntity.ok(activityLogService.getActivityLogById(id));
    }

    @Operation(
            summary = "Export activity logs to CSV",
            description = "Exports filtered activity logs to CSV format. Admin only."
    )
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    @ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportActivityLogs(
            @Parameter(description = "User ID filter")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "User role filter")
            @RequestParam(required = false) String userRole,

            @Parameter(description = "Action filter")
            @RequestParam(required = false) String action,

            @Parameter(description = "Entity type filter")
            @RequestParam(required = false) String entityType,

            @Parameter(description = "Start date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date filter (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Exporting activity logs to CSV - filters: userRole={}, action={}, entityType={}",
                userRole, action, entityType);

        byte[] csvData = activityLogService.exportActivityLogsToCsv(
                userId, userRole, action, entityType, startDate, endDate
        );

        String filename = "activity-logs-" + LocalDate.now() + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        log.info("Generated CSV export: {} bytes, filename: {}", csvData.length, filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }
}
