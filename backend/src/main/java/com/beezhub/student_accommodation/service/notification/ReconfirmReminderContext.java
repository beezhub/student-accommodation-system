package com.beezhub.student_accommodation.service.notification;

/** A reminder to reconfirm a place before move-in (GH-496). */
public record ReconfirmReminderContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String moveInDate,
        String releaseDate,
        String portalUrl
) implements EmailContext {}
