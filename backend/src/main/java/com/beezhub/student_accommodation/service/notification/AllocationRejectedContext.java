package com.beezhub.student_accommodation.service.notification;

public record AllocationRejectedContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String portalUrl
) implements EmailContext {}
