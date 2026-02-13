package com.beezhub.student_accommodation.exceptions;

public class PasswordChangeRequiredException extends RuntimeException {
    public PasswordChangeRequiredException(String message) {
        super(message);
    }
}
