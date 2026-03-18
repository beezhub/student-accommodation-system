package com.beezhub.student_accommodation.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AllocationRuleRequest {

    private Long buildingId;
    private String ruleName;
    private BigDecimal preferenceMatchWeight;
    private BigDecimal desirabilityWeight;
    private BigDecimal accessibilityWeight;
    private Boolean active;
    private Integer priority;
}
