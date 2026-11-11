package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ApplicationStatusCountsResponse;
import com.beezhub.student_accommodation.model.dto.DashboardStatsResponse;
import com.beezhub.student_accommodation.model.dto.OccupancyStatsResponse;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final AppUserRepository appUserRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;

    @Transactional(readOnly = true)
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

        ApplicationStatusCountsResponse applications = getApplicationStatusCounts();

        return DashboardStatsResponse.builder()
                // Application stats
                .totalApplications(applications.getTotal())
                .applicationsByStatus(applications.getByStatus())
                // Document stats
                .totalDocuments(documentRepository.count())
                .pendingDocuments(documentRepository.countByDocumentStatus(DocumentStatus.PENDING))
                .approvedDocuments(documentRepository.countByDocumentStatus(DocumentStatus.APPROVED))
                .rejectedDocuments(documentRepository.countByDocumentStatus(DocumentStatus.REJECTED))
                // User stats — students are counted via tenant-scoped applications;
                // admins are TENANT_ADMIN users visible through the tenant filter.
                .totalStudents(applicationRepository.countDistinctStudents())
                .totalAdmins(appUserRepository.countByUserRole(UserRole.TENANT_ADMIN))
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

    /**
     * Per-status application counts with their total. The total is the sum of the per-status
     * counts, so a breakdown built from them always adds up to it.
     */
    @Transactional(readOnly = true)
    public ApplicationStatusCountsResponse getApplicationStatusCounts() {
        Map<ApplicationStatus, Long> byStatus = countApplicationsByStatus();
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return ApplicationStatusCountsResponse.builder().total(total).byStatus(byStatus).build();
    }

    /**
     * Application count for every status, zeros included, in one grouped query. Runs in a read-only
     * transaction so the tenant filter applies: a tenant admin counts only their own tenant's
     * applications, a platform admin counts all of them.
     */
    @Transactional(readOnly = true)
    public Map<ApplicationStatus, Long> countApplicationsByStatus() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : applicationRepository.countGroupedByStatus()) {
            counts.put((ApplicationStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional(readOnly = true)
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
