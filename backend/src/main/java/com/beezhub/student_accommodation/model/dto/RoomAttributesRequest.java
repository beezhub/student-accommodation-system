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
public class RoomAttributesRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    private Boolean accessibilityCompliant;

    private GenderRestriction genderRestriction;

    @Min(value = 1, message = "Desirability override must be at least 1")
    @Max(value = 10, message = "Desirability override must not exceed 10")
    private Integer desirabilityOverride;
}
