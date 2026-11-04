package com.beezhub.student_accommodation.exceptions;

/**
 * Thrown when a delete is refused because students still hold a place in what would be deleted.
 * The message is written for the administrator who asked for the delete.
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String message) {
        super(message);
    }
}
