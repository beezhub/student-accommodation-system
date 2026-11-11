package com.beezhub.student_accommodation.service.notification;

import java.util.List;

public record DocumentRejectedContext(
        String studentName,
        String documentType,
        String documentReference,
        String submissionDate,
        String reviewDate,
        String rejectionReason,
        String uploadUrl,
        List<String> feedbackPoints
) implements EmailContext {}
