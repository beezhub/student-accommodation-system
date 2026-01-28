package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // Application stats
    private Long totalApplications;
    private Long pendingApplications;
    private Long approvedApplications;
    private Long rejectedApplications;
    private Long inReviewApplications;
    private Long allocatedApplications;

    // Document stats
    private Long totalDocuments;
    private Long pendingDocuments;
    private Long approvedDocuments;
    private Long rejectedDocuments;

    // User stats
    private Long totalStudents;
    private Long totalAdmins;

    // Room stats
    private Long totalRooms;
    private Long availableRooms;
    private Long occupiedRooms;
    private Long fullRooms;
    private Long maintenanceRooms;

    // Building stats
    private Long totalBuildings;

    // Occupancy summary
    private Long totalCapacity;
    private Long totalOccupancy;
    private Double overallOccupancyRate;
}
