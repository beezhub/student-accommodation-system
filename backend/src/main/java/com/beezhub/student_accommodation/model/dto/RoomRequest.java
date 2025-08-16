package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class RoomRequest {

    @NotBlank(message = "Room name must not be blank")
    @Size(max = 100, message = "Room name must be at most 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Capacity must be provided")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotNull(message = "Availability must be provided")
    private Boolean available;

    private Long buildingId;

    private Set<Long> featureIds;
}
