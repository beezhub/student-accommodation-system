package com.beezhub.student_accommodation.service.notification;

public record OfferExpiredContext(
        String studentName,
        String applicationCode,
        String roomNumber,
        String buildingName,
        String portalUrl
) implements EmailContext {}
