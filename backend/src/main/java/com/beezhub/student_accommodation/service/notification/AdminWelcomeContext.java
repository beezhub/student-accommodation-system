package com.beezhub.student_accommodation.service.notification;

/**
 * An admin account was added to a company and can now sign in (GH-419). There is deliberately no
 * password here: the platform admin chose it and passes it on separately.
 */
public record AdminWelcomeContext(
        String adminName,
        String companyName,
        String email,
        boolean mustChangePassword,
        String loginUrl
) implements EmailContext {}
