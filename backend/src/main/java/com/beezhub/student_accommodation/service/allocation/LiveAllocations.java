package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Picks the one allocation to show a student when they hold more than one (GH-496), for example a
 * 2026 room they live in and an offer for 2027.
 *
 * <p>The one that needs them first: an open offer, which lapses within days; then an accepted place
 * waiting for its "Still coming?" answer; then the room they live in; then any other accepted place.
 * Ties go to the earlier stay.
 */
public final class LiveAllocations {

    private LiveAllocations() {
    }

    public static Optional<RoomAllocation> primary(List<RoomAllocation> live) {
        if (live == null || live.isEmpty()) {
            return Optional.empty();
        }
        return live.stream().min(Comparator
                .comparingInt(LiveAllocations::rank)
                .thenComparing(a -> StayPeriod.of(a).from())
                .thenComparing(RoomAllocation::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static int rank(RoomAllocation allocation) {
        AllocationStatus status = allocation.getStatus();
        if (status == AllocationStatus.OFFERED) {
            return 0;
        }
        if (status == AllocationStatus.ACCEPTED && allocation.getReconfirmRequestedAt() != null
                && allocation.getReconfirmedAt() == null) {
            return 1;
        }
        if (status == AllocationStatus.ACTIVE) {
            return 2;
        }
        return 3;
    }
}
