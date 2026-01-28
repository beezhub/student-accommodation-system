package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.DashboardStatsResponse;
import com.beezhub.student_accommodation.model.dto.OccupancyStatsResponse;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;

    public DashboardStatsResponse getDashboardStats() {
        // Room stats
        Long totalRooms = roomRepository.count();
        Long availableRooms = roomRepository.countByStatus("AVAILABLE");
        Long occupiedRooms = roomRepository.countByStatus("OCCUPIED");
        Long fullRooms = roomRepository.countByStatus("FULL");
        Long maintenanceRooms = roomRepository.countByStatus("MAINTENANCE");

        // Capacity stats
        Long totalCapacity = roomRepository.sumTotalCapacity();
        Long totalOccupancy = roomRepository.sumTotalOccupancy();
        Double occupancyRate = totalCapacity > 0 ? (double) totalOccupancy / totalCapacity * 100.0 : 0.0;

        return DashboardStatsResponse.builder()
                // Application stats
                .totalApplications(applicationRepository.count())
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.PENDING))
                .approvedApplications(applicationRepository.countByStatus(ApplicationStatus.APPROVED))
                .rejectedApplications(applicationRepository.countByStatus(ApplicationStatus.REJECTED))
                .inReviewApplications(applicationRepository.countByStatus(ApplicationStatus.AWAITING_ALLOCATION))
                .allocatedApplications(applicationRepository.countByStatus(ApplicationStatus.ALLOCATED))
                // Document stats
                .totalDocuments(documentRepository.count())
                .pendingDocuments(documentRepository.countByDocumentStatus(DocumentStatus.PENDING))
                .approvedDocuments(documentRepository.countByDocumentStatus(DocumentStatus.APPROVED))
                .rejectedDocuments(documentRepository.countByDocumentStatus(DocumentStatus.REJECTED))
                // User stats
                .totalStudents(studentRepository.count())
                .totalAdmins(appUserRepository.countByUserRole(UserRole.ADMIN))
                // Room stats
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .occupiedRooms(occupiedRooms)
                .fullRooms(fullRooms)
                .maintenanceRooms(maintenanceRooms)
                // Building stats
                .totalBuildings(buildingRepository.count())
                // Occupancy summary
                .totalCapacity(totalCapacity)
                .totalOccupancy(totalOccupancy)
                .overallOccupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .build();
    }

    public OccupancyStatsResponse getOccupancyStats() {
        List<Object[]> buildingStats = roomRepository.getBuildingOccupancyStats();

        List<OccupancyStatsResponse.BuildingOccupancy> buildingOccupancies = buildingStats.stream()
                .map(row -> {
                    Long buildingId = (Long) row[0];
                    String buildingName = (String) row[1];
                    Long roomCount = (Long) row[2];
                    Long capacity = (Long) row[3];
                    Long occupancy = (Long) row[4];
                    double rate = capacity > 0 ? (double) occupancy / capacity * 100.0 : 0.0;

                    return OccupancyStatsResponse.BuildingOccupancy.builder()
                            .buildingId(buildingId)
                            .buildingName(buildingName)
                            .roomCount(roomCount)
                            .totalCapacity(capacity)
                            .currentOccupancy(occupancy)
                            .occupancyRate(Math.round(rate * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());

        Long totalCapacity = roomRepository.sumTotalCapacity();
        Long totalOccupancy = roomRepository.sumTotalOccupancy();
        double overallRate = totalCapacity > 0 ? (double) totalOccupancy / totalCapacity * 100.0 : 0.0;

        return OccupancyStatsResponse.builder()
                .totalBuildings((int) buildingRepository.count())
                .totalRooms((int) roomRepository.count())
                .totalCapacity(totalCapacity)
                .totalOccupancy(totalOccupancy)
                .overallOccupancyRate(Math.round(overallRate * 10.0) / 10.0)
                .buildingOccupancies(buildingOccupancies)
                .build();
    }
}
