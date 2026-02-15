package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import java.time.LocalDateTime;

public record AllocationAcceptedEvent(RoomAllocation allocation) {
}
