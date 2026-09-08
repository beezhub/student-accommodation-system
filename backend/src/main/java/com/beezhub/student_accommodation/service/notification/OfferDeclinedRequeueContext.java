package com.beezhub.student_accommodation.service.notification;

public record OfferDeclinedRequeueContext(
        String studentName,
        String roomNumber,
        String buildingName,
        int totalDeclineCount,
        int declinesRemaining,
        String declineReason,
        String portalUrl
) implements EmailContext {}
