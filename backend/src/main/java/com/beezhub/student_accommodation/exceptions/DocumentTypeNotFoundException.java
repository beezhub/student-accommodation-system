package com.beezhub.student_accommodation.exceptions;

public class DocumentTypeNotFoundException extends RuntimeException {
    public DocumentTypeNotFoundException(String message) {
        super(message);
    }
}
