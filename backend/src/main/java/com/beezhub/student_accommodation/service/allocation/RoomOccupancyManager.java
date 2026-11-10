package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * The single writer of {@code room.current_occupancy} and {@code room.status}.
 *
 * <p>GH-136: occupancy used to be a blind delta applied to the in-memory value, while every
 * capacity decision read the live allocation count. The two had no relationship, so they drifted
 * three ways. The delta went negative, because the status machine permits a self-transition and a
 * second check-out on an already-completed allocation decremented again, leaving an empty room at
 * -1 and reading as OCCUPIED. It overwrote MAINTENANCE, so a decline returned a room an admin had
 * parked to AVAILABLE and back into the scoring pool. And nothing ever recomputed, so no path
 * self-healed.
 *
 * <p>Occupancy is now derived from the count rather than adjusted, which makes it convergent: any
 * call fixes whatever the previous state was.
 *
 * <p>It means who is in the room today (GH-496): everyone checked in, and offers and acceptances
 * whose planned stay includes today. Capacity for an offer is counted separately, by overlapping
 * stay dates, so a 2027 offer neither fills a 2026 room here nor is blocked by its status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomOccupancyManager {

    private static final String MAINTENANCE = "MAINTENANCE";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String OCCUPIED = "OCCUPIED";
    private static final String FULL = "FULL";

    private final RoomRepository roomRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final Clock clock;

    /**
     * Recomputes a room's occupancy and status from the allocations in it today.
     *
     * <p>Call this inside the caller's transaction and <strong>after</strong> the allocation's
     * status or room has been mutated. The entity is managed, so Hibernate flushes those changes
     * before the count query runs; an explicit flush is unnecessary and would push unrelated
     * pending writes.
     *
     * <p>MAINTENANCE is preserved. It is an administrative decision about the room, not a statement
     * about how many students are in it, and the old delta silently cleared it.
     */
    public void syncOccupancy(Room room) {
        if (room == null || room.getId() == null) {
            return;
        }

        long active = roomAllocationRepository.countInRoomOn(room.getId(), LocalDate.now(clock));
        int occupancy = (int) active;
        String status = deriveStatus(room, active);

        boolean occupancyChanged = !Integer.valueOf(occupancy).equals(room.getCurrentOccupancy());
        boolean statusChanged = !status.equals(room.getStatus());
        if (!occupancyChanged && !statusChanged) {
            // Room carries a version column, so writing an unchanged row would bump it and create
            // avoidable optimistic-lock contention. This is also what makes a repeated call free.
            return;
        }

        if (room.getCapacity() != null && active > room.getCapacity()) {
            log.warn("action=roomOverCapacity roomId={} active={} capacity={}",
                    room.getId(), active, room.getCapacity());
        }

        room.setCurrentOccupancy(occupancy);
        room.setStatus(status);
        roomRepository.save(room);

        log.info("action=roomOccupancySynced roomId={} occupancy={} status={}",
                room.getId(), occupancy, status);
    }

    private String deriveStatus(Room room, long active) {
        if (MAINTENANCE.equals(room.getStatus())) {
            return MAINTENANCE;
        }
        if (active == 0) {
            return AVAILABLE;
        }
        if (room.getCapacity() != null && active >= room.getCapacity()) {
            return FULL;
        }
        return OCCUPIED;
    }
}
