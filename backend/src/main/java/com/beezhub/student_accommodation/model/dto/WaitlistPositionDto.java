package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for GET /api/waitlist/position — the authenticated student's
 * current position on the waiting list, the total queue depth, and an
 * estimated wait time (null when there isn't enough historical data to
 * estimate from).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistPositionDto {
    private int position;
    private int totalInQueue;
    private Integer estimatedWaitDays;
}
