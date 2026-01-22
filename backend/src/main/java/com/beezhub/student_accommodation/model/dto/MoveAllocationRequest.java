package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveAllocationRequest {

    @NotNull(message = "New room ID is required")
    private Long newRoomId;

    @NotBlank(message = "Reason for move is required")
    private String reason;
}
