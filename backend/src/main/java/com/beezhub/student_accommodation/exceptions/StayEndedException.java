package com.beezhub.student_accommodation.exceptions;

/** A room was offered for a stay whose planned dates have already ended (GH-496). Maps to 409. */
public class StayEndedException extends RuntimeException {
    public StayEndedException(String message) {
        super(message);
    }
}
