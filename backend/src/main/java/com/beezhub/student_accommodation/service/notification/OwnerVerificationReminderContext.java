package com.beezhub.student_accommodation.service.notification;

public record OwnerVerificationReminderContext(
        String ownerName,
        String verificationUrl,
        String supportEmail
) implements EmailContext {}
