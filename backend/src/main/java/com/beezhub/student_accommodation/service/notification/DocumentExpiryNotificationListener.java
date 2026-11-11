package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.event.DocumentExpiryReminderEvent;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Sends the expiring-document reminder (GH-498) after the daily job's transaction commits: an email
 * and an in-app notification, each with one date and one action. Like the other reminder jobs it
 * stores no retry row: a reminder that fails is followed by the next one in the cadence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExpiryNotificationListener {

    static final String TEMPLATE = "document-expiry-reminder";
    static final String NOTIFICATION_TYPE = "DOCUMENT_EXPIRING";

    private final EmailService emailService;
    private final EmailContentBuilder contentBuilder;
    private final UserNotificationService userNotificationService;
    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDocumentExpiryReminder(DocumentExpiryReminderEvent event) {
        Document document = entityManager.find(Document.class, event.documentId());
        if (document == null || document.getStudent() == null || document.getStudent().getAppUser() == null) {
            log.warn("action=documentExpiryReminderSkipped reason=documentMissing documentId={}", event.documentId());
            return;
        }
        AppUser user = document.getStudent().getAppUser();
        String documentName = document.getDocumentType() != null
                ? document.getDocumentType().getTypeName() : "document";
        String expiryDate = contentBuilder.formatDate(event.expiryDate());
        Tenant company = event.tenantId() == null ? null : entityManager.find(Tenant.class, event.tenantId());

        TemplatedEmail email = new TemplatedEmail(TEMPLATE,
                event.expired() ? EmailSubjects.DOCUMENT_EXPIRED : EmailSubjects.DOCUMENT_EXPIRING,
                new DocumentExpiryReminderContext(user.getFirstName() + " " + user.getLastName(), documentName,
                        expiryDate, event.expired(), company != null ? company.getName() : "",
                        baseUrl + "/documents"));
        try {
            emailService.sendTemplateEmail(user.getEmail(), email.subject(), email.templateName(), email.context());
            meterRegistry.counter("sas.emails.sent").increment();
        } catch (Exception e) {
            meterRegistry.counter("sas.emails.failed").increment();
            log.error("action=documentExpiryEmailFailed documentId={}", document.getId(), e);
        }
        try {
            userNotificationService.createNotification(user.getId(), NOTIFICATION_TYPE,
                    event.expired() ? "Document expired" : "Document expiring",
                    message(documentName, expiryDate, event.expired()), Map.of("documentId", document.getId()));
        } catch (Exception e) {
            log.error("action=documentExpiryNotificationFailed documentId={}", document.getId(), e);
        }
        log.info("action=documentExpiryReminderSent documentId={} expired={}", document.getId(), event.expired());
    }

    /** One date and one action: "Your proof of registration expires on 30 November 2026. Upload the new one." */
    static String message(String documentName, String expiryDate, boolean expired) {
        return "Your " + documentName.toLowerCase() + (expired ? " expired on " : " expires on ") + expiryDate
                + ". Upload the new one.";
    }
}
