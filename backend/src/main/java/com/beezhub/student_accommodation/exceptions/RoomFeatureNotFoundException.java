package com.beezhub.student_accommodation.exceptions;

public class RoomFeatureNotFoundException extends RuntimeException {
    public RoomFeatureNotFoundException(String message) {
        super(message);
    }
}
