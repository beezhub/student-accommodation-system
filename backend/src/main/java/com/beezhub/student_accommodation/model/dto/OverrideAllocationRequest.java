package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverrideAllocationRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Reason for override is required")
    private String reason;

    private List<String> bypassConstraints;
}
