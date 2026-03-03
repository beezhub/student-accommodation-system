package com.beezhub.student_accommodation.service.notification;

public record WelcomeContext(
        String studentName,
        String email,
        String studentId,
        String registrationDate,
        String loginUrl,
        String guideUrl
) implements EmailContext {}
