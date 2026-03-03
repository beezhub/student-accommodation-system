package com.beezhub.student_accommodation.service.notification;

public record OfferDeclinedMaxReachedContext(
        String studentName,
        int totalDeclineCount,
        String applicationCode,
        String portalUrl
) implements EmailContext {}
