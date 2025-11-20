package com.beezhub.student_accommodation.exceptions;

public class ApplicationAlreadyAllocatedException extends RuntimeException {
    public ApplicationAlreadyAllocatedException(String message) {
        super(message);
    }
}