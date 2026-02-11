package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.DeclineReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeclineOfferRequest {
    @NotNull(message = "Decline reason is required")
    private DeclineReason declineReason;

    @Size(max = 500, message = "Decline reason text cannot exceed 500 characters")
    private String declineReasonText;
}
