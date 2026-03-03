package com.beezhub.student_accommodation.service.notification;

public record PasswordResetContext(
        String studentName,
        String resetUrl
) implements EmailContext {}
