package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AcceptOfferRequest {
    @NotNull(message = "Expected check-in date is required")
    private LocalDateTime expectedCheckInDate;
}
