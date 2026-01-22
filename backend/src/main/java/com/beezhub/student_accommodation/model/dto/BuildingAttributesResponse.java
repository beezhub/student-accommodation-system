package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildingAttributesResponse {

    private Long id;
    private Long buildingId;
    private String buildingName;
    private GenderRestriction genderRestriction;
    private String accessibilityFeatures;
    private Integer desirabilityScore;
    private Boolean allowMixedGender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
