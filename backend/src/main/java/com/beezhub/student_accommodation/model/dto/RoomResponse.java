package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class RoomResponse {
    private Long id;
    private String name;
    private String description;
    private Integer capacity;
    private Boolean available;
    private Long buildingId;
    private Set<Long> featureIds;
    private Integer currentOccupancy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
