package com.beezhub.student_accommodation.exceptions;

public class ApplicationNotApprovedException extends RuntimeException {
    public ApplicationNotApprovedException(String message) {
        super(message);
    }
}