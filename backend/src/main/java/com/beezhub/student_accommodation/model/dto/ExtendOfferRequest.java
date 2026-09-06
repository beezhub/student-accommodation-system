package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * An administrator moving an offer's response deadline (GH-198).
 *
 * @param newExpiryDate the new deadline. Must be in the future, and no later than a fresh offer
 *                      would have given, so a mistyped year cannot leave an offer open for decades.
 * @param reason        why the deadline moved. Required and recorded in the allocation's audit
 *                      trail: an extension changes a decision the student was already told about,
 *                      so it should never be anonymous.
 */
public record ExtendOfferRequest(
        @NotNull(message = "A new deadline is required") LocalDateTime newExpiryDate,
        @NotNull(message = "A reason is required")
        @Size(min = 3, max = 500, message = "The reason must be between 3 and 500 characters")
        String reason
) {}
