package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BuildingRequest {
    @NotBlank(message = "Building name is required")
    private String name;

    private String address;

    private String description;

    @Min(value = 1, message = "Total floors must be at least 1")
    private Integer totalFloors;
}