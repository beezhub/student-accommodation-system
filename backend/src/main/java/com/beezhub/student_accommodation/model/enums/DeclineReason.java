package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum DeclineReason {
    WRONG_LOCATION("Wrong Location"),
    TOO_EXPENSIVE("Too Expensive"),
    FOUND_ALTERNATIVE("Found Alternative Accommodation"),
    ACCESSIBILITY_ISSUES("Accessibility Issues"),
    PERSONAL_REASONS("Personal Reasons"),
    OTHER("Other");

    private final String displayName;

    DeclineReason(String displayName) {
        this.displayName = displayName;
    }

    public static DeclineReason fromString(String reason) {
        for (DeclineReason declineReason : DeclineReason.values()) {
            if (declineReason.getDisplayName().equalsIgnoreCase(reason) ||
                    declineReason.name().equalsIgnoreCase(reason)) {
                return declineReason;
            }
        }
        throw new IllegalArgumentException("Invalid decline reason: " + reason);
    }
}
