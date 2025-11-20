package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

@Data
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Integer capacity;
    private Integer currentOccupancy;
    private String status;
    private BuildingResponse building;
}
