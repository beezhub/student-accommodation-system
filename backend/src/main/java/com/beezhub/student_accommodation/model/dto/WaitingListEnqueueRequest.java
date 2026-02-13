package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WaitingListEnqueueRequest {
    @NotNull
    private Long studentId;

    @NotNull
    private Long applicationId;
}
