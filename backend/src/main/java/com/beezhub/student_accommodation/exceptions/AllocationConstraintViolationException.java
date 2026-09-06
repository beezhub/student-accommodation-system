package com.beezhub.student_accommodation.exceptions;

/**
 * An allocation was refused by a rule about rooms and students rather than by a status transition.
 *
 * <p>Covers a room that fails a student's hard constraints, and the case where no room can be found
 * at all. Separate from {@link RoomCapacityExceededException}, which is specifically about a room
 * being full, and from {@link InvalidAllocationStatusTransitionException}, which is about the
 * allocation's own lifecycle.
 *
 * <p>Carries a message meant for the administrator who triggered the allocation (GH-205).
 */
public class AllocationConstraintViolationException extends RuntimeException {

    public AllocationConstraintViolationException(String message) {
        super(message);
    }
}
