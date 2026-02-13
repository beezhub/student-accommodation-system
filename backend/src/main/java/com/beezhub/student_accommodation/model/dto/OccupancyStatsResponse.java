package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyStatsResponse {
    private int totalBuildings;
    private int totalRooms;
    private long totalCapacity;
    private long totalOccupancy;
    private double overallOccupancyRate;
    private List<BuildingOccupancy> buildingOccupancies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuildingOccupancy {
        private Long buildingId;
        private String buildingName;
        private long roomCount;
        private long totalCapacity;
        private long currentOccupancy;
        private double occupancyRate;
    }
}
