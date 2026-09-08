package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomRequest {
    @NotBlank(message = "Room number is required")
    private String roomNumber;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private String status; // AVAILABLE, OCCUPIED, FULL, MAINTENANCE

    @Min(value = 0, message = "Monthly rate must be zero or positive")
    private BigDecimal monthlyRate;

    private Long buildingId;
}
