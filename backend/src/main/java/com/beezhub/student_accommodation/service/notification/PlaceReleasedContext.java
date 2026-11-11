package com.beezhub.student_accommodation.service.notification;

/** A place released because it was not reconfirmed before move-in (GH-496). */
public record PlaceReleasedContext(
        String studentName,
        String applicationCode,
        String roomNumber,
        String buildingName,
        String moveInDate,
        String portalUrl
) implements EmailContext {}
