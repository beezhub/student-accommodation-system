package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // Application stats
    private Long totalApplications;
    /** Every {@link ApplicationStatus}, zeros included; the values add up to totalApplications. */
    private Map<ApplicationStatus, Long> applicationsByStatus;

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
