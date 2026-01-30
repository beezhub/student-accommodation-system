package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BuildingResponse {
    private Long id;
    private String name;
    private String address;
    private String description;
    private Integer totalFloors;
    private List<InstitutionResponse> institutions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}