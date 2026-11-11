package com.beezhub.student_accommodation.service.notification;

/**
 * A reminder to replace an expiring document (GH-498), before it expires or after.
 *
 * @param expired false while the document still has days left, true once it has expired
 */
public record DocumentExpiryReminderContext(
        String studentName,
        String documentName,
        String expiryDate,
        boolean expired,
        String companyName,
        String documentsUrl
) implements EmailContext {}
