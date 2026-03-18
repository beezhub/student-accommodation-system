package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRuleResponse {

    private Long id;
    private Long buildingId;
    private String buildingName;
    private String ruleName;
    private BigDecimal preferenceMatchWeight;
    private BigDecimal desirabilityWeight;
    private BigDecimal accessibilityWeight;
    private BigDecimal fcfsWeight;
    private BigDecimal nsfasPriorityWeight;
    private BigDecimal universityPrefWeight;
    private String priorityInstitutionIds;
    private Boolean active;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
