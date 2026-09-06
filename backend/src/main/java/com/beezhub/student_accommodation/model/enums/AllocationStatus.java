package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum AllocationStatus {
    OFFERED("Offered"), // Initial state when admin allocates
    ACCEPTED("Accepted"), // Student has accepted the offer
    DECLINED("Declined"), // Student refused the offer
    EXPIRED("Expired"), // Offer expired without response
    CHECK_IN_EXPIRED("Check-In Expired"), // Student failed to check in after acceptance
    ACTIVE("Active"), // Student has checked in (Lease active)
    COMPLETED("Completed"), // Student moved out naturally
    TERMINATED("Terminated"), // Lease cancelled early
    CANCELLED("Cancelled"); // Offer cancelled by admin before acceptance

    private final String status;

    AllocationStatus(String status) {
        this.status = status;
    }

    public static AllocationStatus fromString(String status) {
        for (AllocationStatus allocationStatus : AllocationStatus.values()) {
            if (allocationStatus.getStatus().equalsIgnoreCase(status) ||
                    allocationStatus.name().equalsIgnoreCase(status)) {
                return allocationStatus;
            }
        }
        throw new IllegalArgumentException("Invalid status: " + status);
    }

    /**
     * Checks if this status can transition to the target status.
     * Valid transitions:
     * - OFFERED -> ACCEPTED, DECLINED, EXPIRED, CANCELLED
     * - ACCEPTED -> ACTIVE, CHECK_IN_EXPIRED, CANCELLED
     * - ACTIVE -> COMPLETED, TERMINATED, CANCELLED
     * - DECLINED, EXPIRED, CHECK_IN_EXPIRED, COMPLETED, TERMINATED, CANCELLED -> (terminal states)
     */
    public boolean canTransitionTo(AllocationStatus target) {
        // Allow self-transition
        if (this == target)
            return true;

        return switch (this) {
            case OFFERED -> target == ACCEPTED || target == DECLINED || target == EXPIRED || target == CANCELLED;
            case ACCEPTED -> target == ACTIVE || target == CHECK_IN_EXPIRED || target == CANCELLED;
            case ACTIVE -> target == COMPLETED || target == TERMINATED || target == CANCELLED;
            case DECLINED, EXPIRED, CHECK_IN_EXPIRED, COMPLETED, TERMINATED, CANCELLED -> false; // Terminal states
        };
    }

    /**
     * Whether this is an end state. Note {@link #canTransitionTo} permits a self-transition, so a
     * terminal status alone does not stop an operation being applied twice; callers that must not
     * repeat themselves check this explicitly (GH-138).
     */
    public boolean isTerminal() {
        return switch (this) {
            case DECLINED, EXPIRED, CHECK_IN_EXPIRED, COMPLETED, TERMINATED, CANCELLED -> true;
            default -> false;
        };
    }
}
