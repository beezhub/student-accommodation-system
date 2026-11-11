package com.beezhub.student_accommodation.service.notification;

/** "Still coming?" before move-in, the first request (GH-496). */
public record ReconfirmRequestContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String moveInDate,
        String releaseDate,
        String confirmByDate,
        String portalUrl
) implements EmailContext {}
