package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAllocationRequest {

    @NotEmpty(message = "Student IDs list cannot be empty")
    @Size(max = 50, message = "Cannot process more than 50 students at once")
    private List<Long> studentIds;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    private Long preferredBuildingId;
}
