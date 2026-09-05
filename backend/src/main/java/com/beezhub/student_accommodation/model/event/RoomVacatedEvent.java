package com.beezhub.student_accommodation.model.event;

import com.beezhub.student_accommodation.model.enums.AllocationStatus;

/**
 * Published when an allocation releases its room (declined or cancelled). Handled after commit
 * so the waiting-list refill sees the freed capacity and cannot roll back the caller's change.
 *
 * @param allocationId The allocation that was declined or cancelled
 * @param roomId       The room whose capacity was released; may be {@code null} if the allocation had no room
 * @param newStatus    The status the allocation moved to
 */
public record RoomVacatedEvent(Long allocationId, Long roomId, AllocationStatus newStatus) {
}
