package com.beezhub.student_accommodation.model.enums;

import java.util.EnumSet;
import java.util.Set;

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

    /**
     * The statuses under which an application is still doing work, and so under which its
     * supporting documents still matter.
     *
     * <p>Lives here because two rules have to agree on it and previously did not.
     * {@code DocumentLifecycleService} locks an approved document while an application it supports
     * is live, while document review resolved the application through a lookup that required status
     * exactly {@code APPROVED}. Approval moves an application to {@code AWAITING_ALLOCATION}
     * immediately, so the one remedy for a wrongly approved document — an admin rejecting it so the
     * student can re-upload — was unavailable in precisely the states the lock protects, leaving
     * deletion as the only unlock and losing the record of what was approved (GH-217).
     *
     * <p>Terminal statuses are excluded on purpose: once an application is rejected, expired or
     * cancelled, nothing depends on the evidence any more.
     */
    public static final Set<ApplicationStatus> LIVE_STATUSES = EnumSet.of(
            PENDING,
            UNDER_REVIEW,
            CHANGES_REQUESTED,
            APPROVED,
            AWAITING_ALLOCATION,
            ALLOCATED,
            CHECKED_IN);

    /** Whether an application in this status is still doing work. See {@link #LIVE_STATUSES}. */
    public boolean isLive() {
        return LIVE_STATUSES.contains(this);
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
     * Whether this status may transition to the target status.
     *
     * <p>Valid transitions, matching the switch below rather than an older draft of it:
     * <ul>
     *   <li>PENDING -> UNDER_REVIEW, APPROVED, REJECTED, CANCELLED</li>
     *   <li>UNDER_REVIEW -> PENDING, CHANGES_REQUESTED, APPROVED, REJECTED</li>
     *   <li>CHANGES_REQUESTED -> PENDING, CANCELLED</li>
     *   <li>APPROVED -> AWAITING_ALLOCATION, ALLOCATED, REJECTED, CANCELLED</li>
     *   <li>AWAITING_ALLOCATION -> ALLOCATED, REJECTED, EXPIRED, CANCELLED</li>
     *   <li>ALLOCATED -> CHECKED_IN, AWAITING_ALLOCATION, EXPIRED, CANCELLED</li>
     *   <li>CHECKED_IN, REJECTED, EXPIRED, CANCELLED -> terminal</li>
     * </ul>
     *
     * <p>APPROVED -> ALLOCATED is legal, and the enum was wrong to forbid it. Three of the four
     * offer-creation paths make an OFFERED allocation without moving the application, so it is
     * still APPROVED when the student accepts. Requiring AWAITING_ALLOCATION first would have made
     * the ordinary accept path illegal rather than describing it (GH-137).
     *
     * <p>The javadoc above also previously omitted three transitions the switch has always
     * allowed, which is how the issue came to describe a legal expiry transition as illegal.
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
            case APPROVED ->
                target == AWAITING_ALLOCATION || target == ALLOCATED || target == REJECTED
                        || target == CANCELLED;
            case AWAITING_ALLOCATION ->
                target == ALLOCATED || target == REJECTED || target == EXPIRED || target == CANCELLED;
            // AWAITING_ALLOCATION is a re-route, not a regression: cancelling an offer has to put
            // the student back on the queue, and without it the application stayed ALLOCATED and
            // blocked them from re-applying (GH-138). Reachable from the admin PATCH endpoint too.
            case ALLOCATED ->
                target == CHECKED_IN || target == AWAITING_ALLOCATION || target == EXPIRED
                        || target == CANCELLED;
            case CHECKED_IN, REJECTED, EXPIRED, CANCELLED -> false; // Terminal states
        };
    }
}
