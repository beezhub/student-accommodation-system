package com.beezhub.student_accommodation.exceptions;

/**
 * A check-in was refused because every bed in the room is still physically taken, usually by a
 * resident whose stay has ended but who has not checked out (GH-496). Maps to 409, as a full room
 * does.
 */
public class RoomStillOccupiedException extends RoomCapacityExceededException {
    public RoomStillOccupiedException(String message) {
        super(message);
    }
}
