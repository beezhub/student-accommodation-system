package com.beezhub.student_accommodation.exceptions;

/**
 * Thrown when a requested tenant does not exist or cannot be resolved.
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String message) {
        super(message);
    }
}
