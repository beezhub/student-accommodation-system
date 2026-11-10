package com.beezhub.student_accommodation.service.notification;

public record ApplicationApprovedContext(
        String studentName,
        Long applicationId,
        String applicationDate,
        String academicYear,
        String intake,
        String portalUrl
) implements EmailContext {}
