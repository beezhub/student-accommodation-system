package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingPreferenceResponse {
    private Long buildingId;
    private String buildingName;
    private Integer preferenceOrder;
}
