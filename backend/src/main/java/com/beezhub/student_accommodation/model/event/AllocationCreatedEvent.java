package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;

public record AllocationCreatedEvent(RoomAllocation allocation) {
}
