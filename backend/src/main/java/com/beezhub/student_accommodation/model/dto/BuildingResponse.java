package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

@Data
public class BuildingResponse {
    private String id;
    private String name;
    private String address;
    private String description;
    private Integer totalFloors;
}