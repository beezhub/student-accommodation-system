package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

/** Test helper: a {@link BedLedger} with the given free beds per room id and nothing booked yet. */
final class BedLedgers {

    private BedLedgers() {
    }

    static BedLedger of(Map<Long, Integer> freeBedsByRoomId) {
        List<Room> rooms = freeBedsByRoomId.entrySet().stream().map(e -> {
            Room room = new Room();
            room.setId(e.getKey());
            room.setCapacity(e.getValue());
            return room;
        }).toList();
        return new BedLedger(rooms, Mockito.mock(RoomAllocationRepository.class));
    }
}
