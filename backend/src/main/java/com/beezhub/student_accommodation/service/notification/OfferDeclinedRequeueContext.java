package com.beezhub.student_accommodation.service.notification;

public record OfferDeclinedRequeueContext(
        String studentName,
        String roomNumber,
        String buildingName,
        int totalDeclineCount,
        int declinesRemaining,
        int maxDeclines,
        String declineReason,
        String portalUrl
) implements EmailContext {}
