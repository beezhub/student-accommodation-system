package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    AWAITING_ALLOCATION("Awaiting Allocation"),
    ALLOCATED("Allocated"),
    REJECTED("Rejected");

    private final String status;

    ApplicationStatus(String status) {
        this.status = status;
    }

    public static ApplicationStatus fromString(String status) {
        for (ApplicationStatus applicationStatus : ApplicationStatus.values()) {
            if (applicationStatus.getStatus().equalsIgnoreCase(status) ||
                applicationStatus.name().equalsIgnoreCase(status)) {
                return applicationStatus;
            }
        }
        throw new IllegalArgumentException("Invalid status: " + status);
    }

    /**
     * Checks if this status can transition to the target status.
     * Valid transitions:
     * - PENDING → APPROVED, REJECTED
     * - APPROVED → AWAITING_ALLOCATION, REJECTED
     * - AWAITING_ALLOCATION → ALLOCATED, REJECTED
     * - ALLOCATED → (terminal state)
     * - REJECTED → (terminal state)
     */
    public boolean canTransitionTo(ApplicationStatus target) {
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED;
            case APPROVED -> target == AWAITING_ALLOCATION || target == REJECTED;
            case AWAITING_ALLOCATION -> target == ALLOCATED || target == REJECTED;
            case ALLOCATED, REJECTED -> false; // Terminal states
        };
    }
}
