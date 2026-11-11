package com.beezhub.student_accommodation.service.notification;

import java.util.List;

/**
 * The student accepted a place, and their other applications for overlapping dates were withdrawn
 * (GH-497). One line per withdrawn application, so one email covers every company.
 */
public record ApplicationsWithdrawnContext(
        String studentName,
        String acceptedPlace,
        List<String> withdrawn,
        String portalUrl
) implements EmailContext {}
