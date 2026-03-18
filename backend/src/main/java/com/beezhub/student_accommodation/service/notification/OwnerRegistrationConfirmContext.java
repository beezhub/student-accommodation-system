package com.beezhub.student_accommodation.service.notification;

public record OwnerRegistrationConfirmContext(
        String ownerName,
        String email,
        String organisationName,
        String verificationUrl,
        String supportEmail
) implements EmailContext {}
