package com.beezhub.student_accommodation.service.notification;

/** A student's stay was extended into the next intake (GH-498). */
public record StayExtendedContext(
        String studentName,
        String roomNumber,
        String buildingName,
        String intakeName,
        String stayEndDate,
        String portalUrl
) implements EmailContext {}
