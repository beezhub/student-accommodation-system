package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class BuildingResponse {
    private Long id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeId;
    private String formattedAddress;
    private Map<String, String> addressComponents;
    private String description;
    private Integer totalFloors;
    private List<InstitutionResponse> institutions;
    private String primaryImageUrl;
    private List<BuildingImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}