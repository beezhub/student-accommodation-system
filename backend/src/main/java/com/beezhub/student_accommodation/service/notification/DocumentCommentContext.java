package com.beezhub.student_accommodation.service.notification;

public record DocumentCommentContext(
        String studentName,
        String documentType,
        String documentName,
        String documentReference,
        String uploadDate,
        String documentStatus,
        String comment,
        String reviewerName,
        String commentDate,
        String portalUrl
) implements EmailContext {}
