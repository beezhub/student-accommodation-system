package com.beezhub.student_accommodation.service.notification;

public record OfferExpiryReminderContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String expiryDate,
        String portalUrl
) implements EmailContext {}
