package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BuildingImageResponse {
    private Long id;
    private Long buildingId;
    private String fileName;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
