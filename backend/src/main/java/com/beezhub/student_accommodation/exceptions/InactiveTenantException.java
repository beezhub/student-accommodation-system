package com.beezhub.student_accommodation.exceptions;

/**
 * Thrown when a request would attach a new record to a deactivated property company.
 *
 * <p>Deactivating a company is not a soft delete: its buildings, staff and applications stay
 * exactly where they are, and its administration screen keeps listing it so it can be switched
 * back on. What deactivation withdraws is the right to make <em>new</em> associations, so this is
 * thrown by the creation paths only, never by a read or by an edit of a record that already
 * belongs to the company.
 *
 * <p>Mapped to 400, not 409. The single guard that existed before (GH-267) threw
 * {@link DuplicateResourceException}, which the handler answers with 409 Conflict, and nothing is
 * duplicated here: the request names a target that is not accepting new records. Every path that
 * refuses for this reason throws this one exception, so the three of them cannot drift apart in
 * status or wording.
 */
public class InactiveTenantException extends RuntimeException {

    public InactiveTenantException(String message) {
        super(message);
    }
}
