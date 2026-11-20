package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds and sends the welcome email for an admin account added to a company (GH-419): which
 * company, where to sign in, and whether the first sign-in asks for a new password. It never
 * carries the password. Kept apart from {@link EmailDispatchService}, which is over the service
 * size limit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminWelcomeEmailService {

    static final String TEMPLATE = "admin-welcome";

    private final EmailService emailService;
    private final MeterRegistry meterRegistry;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * The welcome email. An account created in user management must change the password it was
     * given at first sign-in; an older account repaired here may already have its own, so the email
     * says which applies.
     */
    public TemplatedEmail welcome(AppUser admin, Tenant company) {
        return new TemplatedEmail(TEMPLATE, EmailSubjects.ADMIN_WELCOME, new AdminWelcomeContext(
                admin.getFirstName() + " " + admin.getLastName(),
                company.getName(),
                admin.getEmail(),
                Boolean.TRUE.equals(admin.getMustChangePassword()),
                baseUrl + "/login"));
    }

    public void send(String to, TemplatedEmail email) {
        try {
            emailService.sendTemplateEmail(to, email.subject(), email.templateName(), email.context());
            meterRegistry.counter("sas.emails.sent").increment();
        } catch (RuntimeException e) {
            meterRegistry.counter("sas.emails.failed").increment();
            throw e;
        }
        log.info("action=adminWelcomeEmailSent template={}", email.templateName());
    }
}
