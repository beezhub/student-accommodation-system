package com.beezhub.student_accommodation.service.notification;

public record AllocationConfirmedContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String roomType,
        String startDate,
        String endDate,
        String checkInDeadline,
        String portalUrl
) implements EmailContext {}
