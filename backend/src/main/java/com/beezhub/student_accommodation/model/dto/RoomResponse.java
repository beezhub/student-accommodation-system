package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Integer capacity;
    private Integer currentOccupancy;
    private String status;
    private Long buildingId;
    private String buildingName;
    private BuildingResponse building;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
