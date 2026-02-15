package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Room;
import java.time.LocalDateTime;

public record AllocationMovedEvent(RoomAllocation newAllocation, Room oldRoom, LocalDateTime effectiveDate,
        String reason) {
}
