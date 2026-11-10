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
 * <p>The one that needs them first: an open offer, which lapses within days; then the room they live
 * in; then an accepted place. Ties go to the earlier stay.
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
        if (status == AllocationStatus.ACTIVE) {
            return 1;
        }
        return 2;
    }
}
