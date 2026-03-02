package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.DashboardStatsResponse;
import com.beezhub.student_accommodation.model.dto.OccupancyStatsResponse;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private BuildingRepository buildingRepository;

    @InjectMocks
    private AdminStatsService service;

    @Test
    void getDashboardStats_returnsAggregatedStats() {
        when(roomRepository.count()).thenReturn(50L);
        when(roomRepository.countByStatus("AVAILABLE")).thenReturn(20L);
        when(roomRepository.countByStatus("OCCUPIED")).thenReturn(25L);
        when(roomRepository.countByStatus("FULL")).thenReturn(3L);
        when(roomRepository.countByStatus("MAINTENANCE")).thenReturn(2L);
        when(roomRepository.sumTotalCapacity()).thenReturn(100L);
        when(roomRepository.sumTotalOccupancy()).thenReturn(75L);
        when(applicationRepository.count()).thenReturn(200L);
        when(applicationRepository.countByStatus(ApplicationStatus.PENDING)).thenReturn(10L);
        when(applicationRepository.countByStatus(ApplicationStatus.APPROVED)).thenReturn(50L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(15L);
        when(applicationRepository.countByStatus(ApplicationStatus.AWAITING_ALLOCATION)).thenReturn(30L);
        when(applicationRepository.countByStatus(ApplicationStatus.ALLOCATED)).thenReturn(95L);
        when(documentRepository.count()).thenReturn(500L);
        when(documentRepository.countByDocumentStatus(DocumentStatus.PENDING)).thenReturn(20L);
        when(documentRepository.countByDocumentStatus(DocumentStatus.APPROVED)).thenReturn(450L);
        when(documentRepository.countByDocumentStatus(DocumentStatus.REJECTED)).thenReturn(30L);
        when(studentRepository.count()).thenReturn(180L);
        when(appUserRepository.countByUserRole(UserRole.PLATFORM_ADMIN)).thenReturn(5L);
        when(buildingRepository.count()).thenReturn(10L);

        DashboardStatsResponse stats = service.getDashboardStats();

        assertThat(stats.getTotalRooms()).isEqualTo(50L);
        assertThat(stats.getAvailableRooms()).isEqualTo(20L);
        assertThat(stats.getOccupiedRooms()).isEqualTo(25L);
        assertThat(stats.getFullRooms()).isEqualTo(3L);
        assertThat(stats.getMaintenanceRooms()).isEqualTo(2L);
        assertThat(stats.getTotalCapacity()).isEqualTo(100L);
        assertThat(stats.getTotalOccupancy()).isEqualTo(75L);
        assertThat(stats.getOverallOccupancyRate()).isEqualTo(75.0);
        assertThat(stats.getTotalApplications()).isEqualTo(200L);
        assertThat(stats.getTotalDocuments()).isEqualTo(500L);
        assertThat(stats.getTotalStudents()).isEqualTo(180L);
        assertThat(stats.getTotalAdmins()).isEqualTo(5L);
        assertThat(stats.getTotalBuildings()).isEqualTo(10L);
    }

    @Test
    void getDashboardStats_zeroCapacity_occupancyRateIsZero() {
        when(roomRepository.sumTotalCapacity()).thenReturn(0L);
        when(roomRepository.sumTotalOccupancy()).thenReturn(0L);
        when(roomRepository.count()).thenReturn(0L);
        when(roomRepository.countByStatus(any())).thenReturn(0L);
        when(applicationRepository.count()).thenReturn(0L);
        when(applicationRepository.countByStatus(any())).thenReturn(0L);
        when(documentRepository.count()).thenReturn(0L);
        when(documentRepository.countByDocumentStatus(any())).thenReturn(0L);
        when(studentRepository.count()).thenReturn(0L);
        when(appUserRepository.countByUserRole(any())).thenReturn(0L);
        when(buildingRepository.count()).thenReturn(0L);

        DashboardStatsResponse stats = service.getDashboardStats();

        assertThat(stats.getOverallOccupancyRate()).isEqualTo(0.0);
    }

    @Test
    void getOccupancyStats_returnsPerBuildingBreakdown() {
        Object[] buildingRow = {1L, "Block A", 20L, 40L, 30L};
        List<Object[]> buildingStats = new ArrayList<>();
        buildingStats.add(buildingRow);
        when(roomRepository.getBuildingOccupancyStats()).thenReturn(buildingStats);
        when(roomRepository.sumTotalCapacity()).thenReturn(40L);
        when(roomRepository.sumTotalOccupancy()).thenReturn(30L);
        when(roomRepository.count()).thenReturn(20L);
        when(buildingRepository.count()).thenReturn(1L);

        OccupancyStatsResponse stats = service.getOccupancyStats();

        assertThat(stats.getTotalBuildings()).isEqualTo(1);
        assertThat(stats.getTotalRooms()).isEqualTo(20);
        assertThat(stats.getTotalCapacity()).isEqualTo(40L);
        assertThat(stats.getTotalOccupancy()).isEqualTo(30L);
        assertThat(stats.getOverallOccupancyRate()).isEqualTo(75.0);
        assertThat(stats.getBuildingOccupancies()).hasSize(1);

        OccupancyStatsResponse.BuildingOccupancy occ = stats.getBuildingOccupancies().get(0);
        assertThat(occ.getBuildingId()).isEqualTo(1L);
        assertThat(occ.getBuildingName()).isEqualTo("Block A");
        assertThat(occ.getRoomCount()).isEqualTo(20L);
        assertThat(occ.getTotalCapacity()).isEqualTo(40L);
        assertThat(occ.getCurrentOccupancy()).isEqualTo(30L);
        assertThat(occ.getOccupancyRate()).isEqualTo(75.0);
    }

    @Test
    void getOccupancyStats_zeroCapacityBuilding_occupancyRateIsZero() {
        Object[] buildingRow = {2L, "Empty Block", 5L, 0L, 0L};
        List<Object[]> buildingStats = new ArrayList<>();
        buildingStats.add(buildingRow);
        when(roomRepository.getBuildingOccupancyStats()).thenReturn(buildingStats);
        when(roomRepository.sumTotalCapacity()).thenReturn(0L);
        when(roomRepository.sumTotalOccupancy()).thenReturn(0L);
        when(roomRepository.count()).thenReturn(5L);
        when(buildingRepository.count()).thenReturn(1L);

        OccupancyStatsResponse stats = service.getOccupancyStats();

        assertThat(stats.getOverallOccupancyRate()).isEqualTo(0.0);
        assertThat(stats.getBuildingOccupancies().get(0).getOccupancyRate()).isEqualTo(0.0);
    }

    @Test
    void getOccupancyStats_emptyBuildingList_returnsEmptyOccupancies() {
        when(roomRepository.getBuildingOccupancyStats()).thenReturn(List.of());
        when(roomRepository.sumTotalCapacity()).thenReturn(0L);
        when(roomRepository.sumTotalOccupancy()).thenReturn(0L);
        when(roomRepository.count()).thenReturn(0L);
        when(buildingRepository.count()).thenReturn(0L);

        OccupancyStatsResponse stats = service.getOccupancyStats();

        assertThat(stats.getBuildingOccupancies()).isEmpty();
    }
}
