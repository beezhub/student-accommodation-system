package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;

/** An accepted place was released because the student did not reconfirm before move-in (GH-496). */
public record PlaceReleasedEvent(RoomAllocation allocation) {
}
