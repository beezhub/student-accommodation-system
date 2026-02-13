package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RoomAvailabilityResponse {
    private Long roomId;
    private String roomNumber;
    private String buildingName;
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableSpots;

    @JsonProperty("isAvailable")
    private boolean available;

    private String status;
}
