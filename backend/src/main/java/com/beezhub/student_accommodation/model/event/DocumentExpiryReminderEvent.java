package com.beezhub.student_accommodation.model.event;

import java.time.LocalDate;

/**
 * A student is due a reminder to replace an expiring document (GH-498).
 *
 * @param documentId the document that expires
 * @param tenantId   the company whose academic year it expires with
 * @param expiryDate the last day it is valid at that company
 * @param expired    false before the expiry date, true after it
 */
public record DocumentExpiryReminderEvent(Long documentId, Long tenantId, LocalDate expiryDate, boolean expired) {
}
