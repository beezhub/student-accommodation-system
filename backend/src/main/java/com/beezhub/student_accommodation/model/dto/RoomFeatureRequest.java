package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoomFeatureRequest {
    @NotBlank
    private String name;

    private String description;
}
