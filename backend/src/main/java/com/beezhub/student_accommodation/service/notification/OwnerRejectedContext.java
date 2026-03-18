package com.beezhub.student_accommodation.service.notification;

public record OwnerRejectedContext(
        String ownerName,
        String reason,
        String reapplyUrl,
        String supportEmail
) implements EmailContext {}
