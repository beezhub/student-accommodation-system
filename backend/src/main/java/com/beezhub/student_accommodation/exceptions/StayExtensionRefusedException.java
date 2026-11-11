package com.beezhub.student_accommodation.exceptions;

/**
 * A stay cannot be extended into the next intake (GH-498): the bed was taken since the page loaded,
 * a document is not in order, or the stay has no next intake. Maps to 409.
 */
public class StayExtensionRefusedException extends RuntimeException {
    public StayExtensionRefusedException(String message) {
        super(message);
    }
}
