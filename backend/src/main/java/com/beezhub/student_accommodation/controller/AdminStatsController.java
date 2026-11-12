package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.ApplicationStatusCountsResponse;
import com.beezhub.student_accommodation.model.dto.DashboardStatsResponse;
import com.beezhub.student_accommodation.model.dto.OccupancyStatsResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.beezhub.student_accommodation.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Stats", description = "Endpoints for admin dashboard statistics")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final RoomAllocationService roomAllocationService;

    @Operation(summary = "Get dashboard statistics", description = "Retrieves comprehensive statistics for the admin dashboard including applications, documents, users, rooms, and buildings")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(adminStatsService.getDashboardStats());
    }

    @Operation(summary = "Get application counts per status",
            description = "Application count for every status, zeros included, with their total. "
                    + "Not affected by any list filter; tenant admins see their own tenant only.")
    @ApiResponse(responseCode = "200", description = "Application counts retrieved successfully")
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApplicationStatusCountsResponse> getApplicationStatusCounts() {
        return ResponseEntity.ok(adminStatsService.getApplicationStatusCounts());
    }

    @Operation(summary = "Get room occupancy statistics", description = "Retrieves detailed room occupancy statistics with building-level breakdown")
    @ApiResponse(responseCode = "200", description = "Occupancy statistics retrieved successfully")
    @GetMapping("/occupancy")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<OccupancyStatsResponse> getOccupancyStats() {
        return ResponseEntity.ok(adminStatsService.getOccupancyStats());
    }

    @Operation(summary = "Get overdue check-outs",
            description = "Students still checked in after their planned stay ended, longest overdue first. "
                    + "The next student's check-in into that bed is refused until they check out.")
    @ApiResponse(responseCode = "200", description = "Overdue check-outs retrieved successfully")
    @GetMapping("/overdue-check-outs")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<PageResponse<RoomAllocationResponse>> getOverdueCheckOuts(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(roomAllocationService.getOverdueCheckOuts(pageable));
    }
}
