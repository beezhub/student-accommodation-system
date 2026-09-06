package com.beezhub.student_accommodation.service.document;

/**
 * Outcome of validating an uploaded file.
 *
 * @param clean               whether the file may be stored
 * @param reason              why it was rejected, or null when it was accepted
 * @param resolvedContentType the content type decided from the file's own bytes, never the value
 *                            the client claimed (GH-144); null when the file was rejected
 */
public record ValidationResult(boolean clean, String reason, String resolvedContentType) {

    public static ValidationResult valid(String resolvedContentType) {
        return new ValidationResult(true, null, resolvedContentType);
    }

    public static ValidationResult invalid(String reason) {
        return new ValidationResult(false, reason, null);
    }
}
