package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    CHANGES_REQUESTED("Changes Requested"),
    APPROVED("Approved"),
    AWAITING_ALLOCATION("Awaiting Allocation"),
    ALLOCATED("Allocated"),
    CHECKED_IN("Checked In"),
    REJECTED("Rejected"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled");

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
     * - PENDING -> UNDER_REVIEW, APPROVED, REJECTED, CANCELLED
     * - UNDER_REVIEW -> CHANGES_REQUESTED, APPROVED, REJECTED
     * - CHANGES_REQUESTED -> PENDING, CANCELLED
     * - APPROVED -> AWAITING_ALLOCATION, REJECTED
     * - AWAITING_ALLOCATION -> ALLOCATED, REJECTED, EXPIRED
     * - ALLOCATED -> CHECKED_IN, EXPIRED, CANCELLED
     * - CHECKED_IN -> (terminal state for application flow)
     * - REJECTED, EXPIRED, CANCELLED -> (terminal states)
     */
    public boolean canTransitionTo(ApplicationStatus target) {
        // Allow self-transition
        if (this == target)
            return true;

        return switch (this) {
            case PENDING -> target == UNDER_REVIEW || target == APPROVED || target == REJECTED || target == CANCELLED;
            case UNDER_REVIEW ->
                target == PENDING || target == CHANGES_REQUESTED || target == APPROVED || target == REJECTED;
            case CHANGES_REQUESTED -> target == PENDING || target == CANCELLED;
            case APPROVED -> target == AWAITING_ALLOCATION || target == REJECTED || target == CANCELLED;
            case AWAITING_ALLOCATION ->
                target == ALLOCATED || target == REJECTED || target == EXPIRED || target == CANCELLED;
            case ALLOCATED -> target == CHECKED_IN || target == EXPIRED || target == CANCELLED;
            case CHECKED_IN, REJECTED, EXPIRED, CANCELLED -> false; // Terminal states
        };
    }
}
