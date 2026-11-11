package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.allocation.StayPeriod;
import com.beezhub.student_accommodation.service.application.CompanyIntakePolicy;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Builds and sends the email for an application that expired with its intake (GH-497), offering
 * the next intake open at that company for the student's institution, if one is open. Kept apart
 * from {@link EmailDispatchService}, which is over the service size limit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationExpiryEmailService {

    static final String TEMPLATE = "application-expired";

    private final EmailService emailService;
    private final EmailContentBuilder contentBuilder;
    private final CompanyIntakePolicy companyIntakePolicy;
    private final MeterRegistry meterRegistry;

    @Value("${app.base-url}")
    private String baseUrl;

    /** The expiry notice, with the earliest open intake at the same company when there is one. */
    public TemplatedEmail expired(Application application, String studentName) {
        Intake intake = application.getIntake();
        Optional<Intake> next = nextOpenIntake(application);
        return new TemplatedEmail(TEMPLATE, EmailSubjects.APPLICATION_EXPIRED, new ApplicationExpiredContext(
                studentName,
                application.getApplicationCode(),
                application.getTenant() != null ? application.getTenant().getName() : "the residence",
                intake != null && intake.getAcademicYear() != null ? CompanyIntakePolicy.label(intake)
                        : contentBuilder.intakeOf(application),
                contentBuilder.formatDate(StayPeriod.of(application).end()),
                next.map(CompanyIntakePolicy::label).orElse(null),
                next.map(i -> contentBuilder.formatDate(i.getStayStartDate())).orElse(null),
                next.map(i -> contentBuilder.formatDate(i.getApplicationCloseDate())).orElse(null),
                baseUrl + "/applications/new",
                baseUrl + "/applications"));
    }

    public void send(String to, TemplatedEmail email) {
        try {
            emailService.sendTemplateEmail(to, email.subject(), email.templateName(), email.context());
            meterRegistry.counter("sas.emails.sent").increment();
        } catch (RuntimeException e) {
            meterRegistry.counter("sas.emails.failed").increment();
            throw e;
        }
        log.info("action=applicationExpiredEmailSent template={}", email.templateName());
    }

    private Optional<Intake> nextOpenIntake(Application application) {
        if (application.getTenantId() == null) {
            return Optional.empty();
        }
        Long institutionId = application.getInstitution() != null ? application.getInstitution().getId() : null;
        return companyIntakePolicy.openIntakes(application.getTenantId(), institutionId).stream().findFirst();
    }
}
