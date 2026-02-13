package com.beezhub.student_accommodation.exceptions;

public class InstitutionNotFoundException extends RuntimeException {
    public InstitutionNotFoundException(String message) {
        super(message);
    }
}
