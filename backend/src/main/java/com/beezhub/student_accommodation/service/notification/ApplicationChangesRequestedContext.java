package com.beezhub.student_accommodation.service.notification;

public record ApplicationChangesRequestedContext(
        String studentName,
        Long applicationId,
        String applicationCode,
        String changesReason,
        String portalUrl
) implements EmailContext {}
