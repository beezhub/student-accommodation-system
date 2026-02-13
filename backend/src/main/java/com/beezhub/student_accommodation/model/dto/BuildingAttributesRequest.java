package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingAttributesRequest {

    @NotNull(message = "Building ID is required")
    private Long buildingId;

    private GenderRestriction genderRestriction;

    private String accessibilityFeatures;

    @Min(value = 1, message = "Desirability score must be at least 1")
    @Max(value = 10, message = "Desirability score must not exceed 10")
    private Integer desirabilityScore;

    private Boolean allowMixedGender;
}
