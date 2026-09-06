package com.beezhub.student_accommodation.exceptions;

/**
 * Thrown when a student tries to replace or delete a document an administrator has already
 * approved, while an application that approval supports is still live.
 *
 * <p>GH-145: neither path looked at the document status, so the evidence behind an approved or
 * allocated application could be swapped or wiped after review. The checklist and the allocation
 * gate both read the live document pack by student id, so the pack silently changed underneath.
 */
public class DocumentLockedException extends RuntimeException {

    public DocumentLockedException(String message) {
        super(message);
    }
}
