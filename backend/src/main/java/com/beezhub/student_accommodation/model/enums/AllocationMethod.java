package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum AllocationMethod {
    AUTO("Automatic"),
    MANUAL("Manual");

    private final String displayName;

    AllocationMethod(String displayName) {
        this.displayName = displayName;
    }

    public static AllocationMethod fromString(String value) {
        for (AllocationMethod method : AllocationMethod.values()) {
            if (method.name().equalsIgnoreCase(value) ||
                method.getDisplayName().equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid allocation method: " + value);
    }
}
