package com.beezhub.student_accommodation.service.notification;

public record OwnerApprovedContext(
        String ownerName,
        String organisationName,
        String passwordSetupUrl,
        String portalUrl,
        String supportEmail
) implements EmailContext {}
