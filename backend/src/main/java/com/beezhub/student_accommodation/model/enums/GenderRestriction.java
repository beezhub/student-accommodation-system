package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum GenderRestriction {
    ANY("Any"),
    MALE_ONLY("Male Only"),
    FEMALE_ONLY("Female Only");

    private final String displayName;

    GenderRestriction(String displayName) {
        this.displayName = displayName;
    }

    public static GenderRestriction fromString(String value) {
        for (GenderRestriction restriction : GenderRestriction.values()) {
            if (restriction.name().equalsIgnoreCase(value) ||
                restriction.getDisplayName().equalsIgnoreCase(value)) {
                return restriction;
            }
        }
        throw new IllegalArgumentException("Invalid gender restriction: " + value);
    }
}
