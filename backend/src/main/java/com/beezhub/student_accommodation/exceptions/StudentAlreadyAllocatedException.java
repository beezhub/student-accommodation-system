package com.beezhub.student_accommodation.exceptions;

public class StudentAlreadyAllocatedException extends RuntimeException {
    public StudentAlreadyAllocatedException(String message) {
        super(message);
    }
}