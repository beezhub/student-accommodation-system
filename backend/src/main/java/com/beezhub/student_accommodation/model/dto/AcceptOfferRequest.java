package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AcceptOfferRequest {
    /**
     * When the student expects to arrive. Optional (GH-496): empty means move-in day, or now when
     * the stay has already started.
     */
    private LocalDateTime expectedCheckInDate;
}
