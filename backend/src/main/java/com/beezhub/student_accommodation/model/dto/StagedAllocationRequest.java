package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StagedAllocationRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    private Long applicationId;

    private StagedAllocationStatus status;

    private BigDecimal score;

    private List<String> warnings;

    private String batchId;
}
