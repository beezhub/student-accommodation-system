package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum StagedAllocationStatus {
    PROPOSED("Proposed"),
    CONFIRMED("Confirmed"),
    REJECTED("Rejected");

    private final String displayName;

    StagedAllocationStatus(String displayName) {
        this.displayName = displayName;
    }

    public static StagedAllocationStatus fromString(String value) {
        for (StagedAllocationStatus status : StagedAllocationStatus.values()) {
            if (status.name().equalsIgnoreCase(value) ||
                status.getDisplayName().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid staged allocation status: " + value);
    }
}
