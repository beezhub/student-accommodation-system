package com.beezhub.student_accommodation.service.notification;

/**
 * An unfinished application expired because its intake's stay ended (GH-497). The next-intake
 * fields are empty when no window is open at that company.
 */
public record ApplicationExpiredContext(
        String studentName,
        String applicationCode,
        String companyName,
        String intakeName,
        String stayEndDate,
        String nextIntakeName,
        String nextMoveInDate,
        String nextApplyByDate,
        String applyUrl,
        String portalUrl
) implements EmailContext {}
