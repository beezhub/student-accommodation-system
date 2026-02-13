package com.beezhub.student_accommodation.exceptions;

public class ApplicationWindowClosedException extends RuntimeException {
    public ApplicationWindowClosedException(String message) {
        super(message);
    }
}
