package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Free beds during one batch run, by room and stay dates (GH-496).
 *
 * <p>The batch matches every student before it creates any allocation, so the database count alone
 * would let two students take the last bed for the same dates. The ledger adds the stays already
 * matched in this run to the database count, and a Semester 1 and a Semester 2 student can still
 * be matched to the same bed because their stays do not overlap.
 */
public class BedLedger {

    private final Map<Long, Room> rooms = new HashMap<>();
    private final Map<Long, List<StayPeriod>> matched = new HashMap<>();
    private final RoomAllocationRepository roomAllocationRepository;

    public BedLedger(List<Room> rooms, RoomAllocationRepository roomAllocationRepository) {
        rooms.forEach(room -> this.rooms.put(room.getId(), room));
        this.roomAllocationRepository = roomAllocationRepository;
    }

    /**
     * Takes a bed in the room for the stay when one is free, counting allocations whose planned stay
     * overlaps it and the stays matched earlier in this run.
     *
     * @return whether a bed was taken
     */
    public boolean tryTake(Long roomId, StayPeriod stay) {
        Room room = rooms.get(roomId);
        if (room == null || room.getCapacity() == null) {
            return false;
        }
        List<StayPeriod> alreadyMatched = matched.getOrDefault(roomId, List.of());
        long taken = roomAllocationRepository.countHoldingBeds(roomId, stay.from(), stay.until())
                + alreadyMatched.stream().filter(stay::overlaps).count();
        if (taken >= room.getCapacity()) {
            return false;
        }
        matched.computeIfAbsent(roomId, k -> new ArrayList<>()).add(stay);
        return true;
    }
}
