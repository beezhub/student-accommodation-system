package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleRoomResponse {
    private Long roomId;
    private String roomNumber;
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableSpots;
    private String status;
    
    private Long buildingId;
    private String buildingName;
    private String buildingAddress;
    
    // Room attributes
    private String genderRestriction;
    private Boolean accessibilityCompliant;
    private Integer minYearOfStudy;
    private Integer maxYearOfStudy;
}
