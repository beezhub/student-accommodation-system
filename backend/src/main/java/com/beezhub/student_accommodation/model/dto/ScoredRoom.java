package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoredRoom {

    private Long roomId;
    private String roomNumber;
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableSpots;

    private Long buildingId;
    private String buildingName;

    private BigDecimal totalScore;
    private BigDecimal preferenceMatchScore;
    private BigDecimal desirabilityScore;
    private BigDecimal accessibilityScore;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private Boolean meetsHardConstraints;
    private Boolean isPreferredBuilding;
    private Boolean isAccessibilityCompliant;

    // For tie-breaking
    private LocalDateTime applicationCreatedAt;

    // Room attributes
    private String genderRestriction;
    private Integer desirabilityRating;

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}
