package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRuleRequest {

    private Long buildingId;

    @NotBlank(message = "Rule name is required")
    @Size(max = 100, message = "Rule name must not exceed 100 characters")
    private String ruleName;

    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    @DecimalMax(value = "100.0", message = "Weight must not exceed 100")
    private BigDecimal preferenceMatchWeight;

    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    @DecimalMax(value = "100.0", message = "Weight must not exceed 100")
    private BigDecimal desirabilityWeight;

    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    @DecimalMax(value = "100.0", message = "Weight must not exceed 100")
    private BigDecimal accessibilityWeight;

    private Boolean active;

    private Integer priority;
}
