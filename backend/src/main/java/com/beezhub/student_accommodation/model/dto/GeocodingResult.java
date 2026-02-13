package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingResult {
    private boolean success;
    private String errorMessage;
    private Double latitude;
    private Double longitude;
    private String placeId;
    private String formattedAddress;
    private Map<String, String> addressComponents;
}
