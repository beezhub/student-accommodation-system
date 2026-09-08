package com.beezhub.student_accommodation.service.notification;

public record DocumentVerifiedContext(
        String studentName,
        String documentType,
        String documentReference,
        String verificationDate,
        String verifiedBy,
        String portalUrl
) implements EmailContext {}
