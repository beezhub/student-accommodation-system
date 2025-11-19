package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

@Data
public class AllocationEligibilityResponse {
    private Long studentId;
    private boolean eligible;
    private String reason;
}
