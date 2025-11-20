package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomAllocationRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    private Long applicationId;

    private LocalDateTime allocationDate;

    private LocalDateTime endDate;
}