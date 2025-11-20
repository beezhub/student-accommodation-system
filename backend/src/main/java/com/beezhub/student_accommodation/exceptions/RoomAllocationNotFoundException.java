package com.beezhub.student_accommodation.exceptions;

public class RoomAllocationNotFoundException extends RuntimeException {
    public RoomAllocationNotFoundException(String message) {
        super(message);
    }
}