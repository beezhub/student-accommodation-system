package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Integer capacity;
    private Integer currentOccupancy;
    private String status;
    private BigDecimal monthlyRate;
    private Long buildingId;
    private String buildingName;
    private BuildingResponse building;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Room attributes
    private Boolean accessibilityCompliant;
    private GenderRestriction genderRestriction;
    private Integer desirabilityOverride;
    private Integer minYearOfStudy;
    private Integer maxYearOfStudy;
}
