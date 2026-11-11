package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;

import java.time.LocalDate;

/**
 * A student with an accepted place was asked "Still coming?" before move-in, or reminded (GH-496).
 *
 * @param allocation  the accepted allocation
 * @param reminder    false for the first request, true for a reminder
 * @param releaseDate the day the place is released if it is still not reconfirmed
 */
public record ReconfirmationRequestedEvent(RoomAllocation allocation, boolean reminder, LocalDate releaseDate) {
}
