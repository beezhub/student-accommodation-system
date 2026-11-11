package com.beezhub.student_accommodation.service.notification;

public record DocumentRejectedContext(
        String studentName,
        String documentType,
        String documentReference,
        String submissionDate,
        String reviewDate,
        String rejectionReason,
        String uploadUrl,
        String maxFileSize
) implements EmailContext {}
