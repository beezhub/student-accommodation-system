package com.beezhub.student_accommodation.exceptions;

/**
 * Thrown when a building's accessibility features are not a JSON list of text. The column is JSONB
 * and the public building page reads a list, so anything else would fail the save with a 500 or
 * never be shown.
 */
public class InvalidAccessibilityFeaturesException extends RuntimeException {
    public InvalidAccessibilityFeaturesException(String message) {
        super(message);
    }
}
