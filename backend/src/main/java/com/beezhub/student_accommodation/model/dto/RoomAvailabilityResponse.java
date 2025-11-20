package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

@Data
public class RoomAvailabilityResponse {
    private Long roomId;
    private String roomNumber;
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableSpots;
    private boolean available;
    private String status;
}
