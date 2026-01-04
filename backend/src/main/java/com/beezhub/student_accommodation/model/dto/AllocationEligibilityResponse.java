package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AllocationEligibilityResponse {
    private Long studentId;

    @JsonProperty("isEligible")
    private boolean eligible;

    private String reason;
    private boolean hasApprovedApplication;
    private boolean hasActiveAllocation;
}
