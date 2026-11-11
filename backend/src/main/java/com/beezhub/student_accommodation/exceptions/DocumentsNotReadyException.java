package com.beezhub.student_accommodation.exceptions;

/**
 * A room offer, check-in or stay extension waits for the student's documents (GH-498): a required
 * expiring document is expired or not yet approved. Maps to 409.
 */
public class DocumentsNotReadyException extends RuntimeException {
    public DocumentsNotReadyException(String message) {
        super(message);
    }
}
