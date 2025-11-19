package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

@Data
public class RoomResponse {
    private String id;
    private String roomNumber;
    private Integer capacity;
    private BuildingResponse building;
}
