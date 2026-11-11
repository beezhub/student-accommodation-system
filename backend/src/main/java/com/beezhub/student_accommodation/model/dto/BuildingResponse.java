package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class BuildingResponse {
    private Long id;
    private Long tenantId;

    /** The company's name. Filled in only where a student picks buildings across companies (GH-495). */
    private String companyName;
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

    // Pricing and availability summary (computed from rooms)
    private BigDecimal minMonthlyRate;
    private BigDecimal maxMonthlyRate;
    private Integer availableRoomCount;

    // Room summaries for detail endpoint
    private List<RoomResponse> rooms;

    // Building attributes for detail endpoint
    private String accessibilityFeatures;
}