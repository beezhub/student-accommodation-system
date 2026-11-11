package com.beezhub.student_accommodation.service.notification;

public record ApplicationRejectedContext(
        String studentName,
        Long applicationId,
        String applicationCode,
        String rejectionReason,
        String applicationDate,
        String reviewDate,
        String nextApplicationPeriod,
        String portalUrl
) implements EmailContext {}
