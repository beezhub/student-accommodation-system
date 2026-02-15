package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.RoomRepository;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
class RoomOccupancyManager {

    /**
     * Updates room occupancy and status.
     * Calculated based on current occupancy + change.
     */
    public static void updateOccupancy(Room room, int change, RoomRepository roomRepository) {
        int newOccupancy = (room.getCurrentOccupancy() != null ? room.getCurrentOccupancy() : 0) + change;
        room.setCurrentOccupancy(newOccupancy);

        if (newOccupancy == 0) {
            room.setStatus("AVAILABLE");
        } else if (newOccupancy >= room.getCapacity()) {
            room.setStatus("FULL");
        } else {
            room.setStatus("OCCUPIED");
        }

        roomRepository.save(room);
        log.info("Updated room {} occupancy to {} (status: {})",
                room.getRoomNumber(), newOccupancy, room.getStatus());
    }
}
