package com.beezhub.student_accommodation.service.notification;

public record CheckInReminderContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String deadlineDate,
        String portalUrl
) implements EmailContext {}
