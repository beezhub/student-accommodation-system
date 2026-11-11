package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.event.DocumentExpiryReminderEvent;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExpiryNotificationListenerTest {

    @Mock EmailService emailService;
    @Mock UserNotificationService userNotificationService;
    @Mock EntityManager entityManager;

    private DocumentExpiryNotificationListener listener;
    private Document document;

    @BeforeEach
    void setUp() {
        listener = new DocumentExpiryNotificationListener(emailService, new EmailContentBuilder(),
                userNotificationService, entityManager, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(listener, "baseUrl", "http://test-url");
        AppUser user = AppUser.builder().id(1L).email("student@example.com").firstName("Thandi").lastName("M").build();
        Student student = new Student();
        student.setAppUser(user);
        DocumentType type = new DocumentType();
        type.setTypeName("Proof of Registration");
        document = Document.builder().id(50L).student(student).documentType(type).build();
    }

    @Test
    void handleDocumentExpiryReminder_beforeExpiry_emailsAndNotifiesWithOneDateAndOneAction() {
        when(entityManager.find(Document.class, 50L)).thenReturn(document);
        when(entityManager.find(Tenant.class, 7L)).thenReturn(Tenant.builder().id(7L).name("Company A").build());

        listener.handleDocumentExpiryReminder(new DocumentExpiryReminderEvent(50L, 7L, LocalDate.of(2026, 11, 30), false));

        ArgumentCaptor<EmailContext> context = ArgumentCaptor.forClass(EmailContext.class);
        verify(emailService).sendTemplateEmail(eq("student@example.com"), eq(EmailSubjects.DOCUMENT_EXPIRING),
                eq(DocumentExpiryNotificationListener.TEMPLATE), context.capture());
        assertThat(context.getValue()).isEqualTo(new DocumentExpiryReminderContext("Thandi M",
                "Proof of Registration", "30 November 2026", false, "Company A", "http://test-url/documents"));
        verify(userNotificationService).createNotification(1L, DocumentExpiryNotificationListener.NOTIFICATION_TYPE,
                "Document expiring", "Your proof of registration expires on 30 November 2026. Upload the new one.",
                Map.of("documentId", 50L));
    }

    @Test
    void handleDocumentExpiryReminder_afterExpiry_saysItExpired_andAFailedEmailStillNotifies() {
        when(entityManager.find(Document.class, 50L)).thenReturn(document);
        doThrow(new IllegalStateException("smtp down")).when(emailService)
                .sendTemplateEmail(anyString(), anyString(), anyString(), any(EmailContext.class));

        listener.handleDocumentExpiryReminder(new DocumentExpiryReminderEvent(50L, null, LocalDate.of(2026, 11, 30), true));

        verify(userNotificationService).createNotification(eq(1L), eq(DocumentExpiryNotificationListener.NOTIFICATION_TYPE),
                eq("Document expired"), eq("Your proof of registration expired on 30 November 2026. Upload the new one."),
                any());
    }

    @Test
    void handleDocumentExpiryReminder_documentGone_sendsNothing() {
        listener.handleDocumentExpiryReminder(new DocumentExpiryReminderEvent(50L, 7L, LocalDate.of(2026, 11, 30), false));

        verifyNoInteractions(emailService, userNotificationService);
    }
}
