package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.service.notification.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        try {
            java.lang.reflect.Field fromEmailField = EmailService.class.getDeclaredField("fromEmail");
            fromEmailField.setAccessible(true);
            fromEmailField.set(emailService, "noreply@example.com");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>email</html>");
    }

    // ── GH-356: the From address is not the SMTP login when MAIL_FROM is set ─────────────────

    private void setMailFrom(String value) throws Exception {
        java.lang.reflect.Field field = EmailService.class.getDeclaredField("mailFrom");
        field.setAccessible(true);
        field.set(emailService, value);
    }

    @Test
    void sendSimpleEmail_mailFromSet_sendsFromItNotTheSmtpLogin() throws Exception {
        setMailFrom("no-reply@beezhub.co.za");

        emailService.sendSimpleEmail("test@example.com", "Subject", "Body");

        org.mockito.ArgumentCaptor<SimpleMailMessage> sent = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        org.assertj.core.api.Assertions.assertThat(sent.getValue().getFrom()).isEqualTo("no-reply@beezhub.co.za");
    }

    @Test
    void sendSimpleEmail_mailFromBlank_fallsBackToTheSmtpLogin() throws Exception {
        setMailFrom("");

        emailService.sendSimpleEmail("test@example.com", "Subject", "Body");

        org.mockito.ArgumentCaptor<SimpleMailMessage> sent = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        org.assertj.core.api.Assertions.assertThat(sent.getValue().getFrom()).isEqualTo("noreply@example.com");
    }

    @Test
    void sender_mailFromSet_isUsedForHtmlMailToo() throws Exception {
        setMailFrom("no-reply@beezhub.co.za");

        org.assertj.core.api.Assertions.assertThat(emailService.sender()).isEqualTo("no-reply@beezhub.co.za");
    }

    @Test
    void sendSimpleEmail_shouldSendEmail() {
        emailService.sendSimpleEmail("test@example.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
    
    @Test
    void sendSimpleEmail_whenMailSenderThrowsException_shouldThrowRuntimeException() {
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendSimpleEmail("test@example.com", "Subject", "Body"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send email");
    }

    @Test
    void sendHtmlEmail_shouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("test@example.com", "Subject", "<p>Body</p>");

        verify(mailSender).send(mimeMessage);
    }
    
    @Test
    void sendHtmlEmail_whenMessagingException_shouldThrowRuntimeException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Messaging error"));

        // Service only wraps MessagingException with "Failed to send email"; a raw RuntimeException propagates as-is
        assertThatThrownBy(() -> emailService.sendHtmlEmail("test@example.com", "Subject", "<p>Body</p>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Messaging error");
    }

    @Test
    void sendEmailWithAttachment_shouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendEmailWithAttachment("test@example.com", "Subject", "Body", "path/to/attachment");

        verify(mailSender).send(mimeMessage);
    }
    
    @Test
    void sendEmailWithAttachment_whenException_shouldThrowRuntimeException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Messaging error"));

        assertThatThrownBy(() -> emailService.sendEmailWithAttachment("test@example.com", "Subject", "Body", "path/to/attachment"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void sendTemplateEmail_shouldProcessTemplateAndSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("Processed HTML");

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test");

        emailService.sendTemplateEmail("test@example.com", "Subject", "test-template", variables);

        verify(templateEngine).process(eq("email/test-template"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendTemplateEmail_shouldThrowException_whenTemplateError() {
        when(templateEngine.process(anyString(), any(Context.class))).thenThrow(new RuntimeException("Template error"));

        Map<String, Object> variables = new HashMap<>();

        assertThatThrownBy(() -> emailService.sendTemplateEmail("test@example.com", "Subject", "test-template", variables))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void sendTemplateEmail_withApplicationApprovedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new ApplicationApprovedContext("Student Name", 1L, "APP-1", "2023-01-01", "2023", "Semester 1", "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.APPLICATION_APPROVED, "application-approved", ctx);

        verify(templateEngine).process(eq("email/application-approved"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withApplicationRejectedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new ApplicationRejectedContext("Student Name", 1L, "APP-1", "Reason", "2023-01-01", "2023-01-02", "Fall", "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.APPLICATION_REJECTED, "application-rejected", ctx);

        verify(templateEngine).process(eq("email/application-rejected"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withChangesRequestedContext_passesTheReasonAndLinkToTheTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new ApplicationChangesRequestedContext("Student Name", 1L, "APP-1", "Reason", "http://portal/applications/1");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.APPLICATION_CHANGES_REQUESTED,
                "application-changes-requested", ctx);

        org.mockito.ArgumentCaptor<Context> context = org.mockito.ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/application-changes-requested"), context.capture());
        org.assertj.core.api.Assertions.assertThat(context.getValue().getVariable("changesReason")).isEqualTo("Reason");
        org.assertj.core.api.Assertions.assertThat(context.getValue().getVariable("portalUrl"))
                .isEqualTo("http://portal/applications/1");
    }

    @Test
    void sendTemplateEmail_withDocumentVerifiedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new DocumentVerifiedContext("Student Name", "ID", "REF", "2023-01-01", "Admin", "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.DOCUMENT_VERIFIED, "document-verified", ctx);

        verify(templateEngine).process(eq("email/document-verified"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withDocumentRejectedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new DocumentRejectedContext("Student Name", "ID", "REF", "2023-01-01",
                "2023-01-02", "Blurry", "2023-01-10", "url", List.of());

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.DOCUMENT_REJECTED, "document-rejected", ctx);

        verify(templateEngine).process(eq("email/document-rejected"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withRoomAllocatedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new RoomAllocatedContext("Student Name", "101", "Building", "Single",
                "1", "ALLOC1", "2023-01-01", "2023-12-31", "1 Year", "Office", "Address", "url",
                "01 January 2024 at 12:00 UTC");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.ROOM_ALLOCATED, "room-allocated", ctx);

        verify(templateEngine).process(eq("email/room-allocated"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withWelcomeContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new WelcomeContext("Student Name", "test@example.com", "ST123", "2023-01-01", "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.WELCOME, "welcome", ctx);

        verify(templateEngine).process(eq("email/welcome"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withAllocationProposalContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new AllocationProposalContext("Student Name", "101", "Building", 2, "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.ALLOCATION_PROPOSAL, "allocation-proposal", ctx);

        verify(templateEngine).process(eq("email/allocation-proposal"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withAllocationConfirmedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new AllocationConfirmedContext("Student", "101", "Building", "Single",
                "ALLOC1", "2023-01-01", "2023-01-01", "2023-12-31", "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.ALLOCATION_CONFIRMED, "allocation-confirmed", ctx);

        verify(templateEngine).process(eq("email/allocation-confirmed"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withAllocationRejectedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new AllocationRejectedContext("Student", "101", "Building", "Full", 5, "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.ALLOCATION_REJECTED, "allocation-rejected", ctx);

        verify(templateEngine).process(eq("email/allocation-rejected"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withAllocationMovedContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new AllocationMovedContext("Student", "101", "Bld1", "201", "Bld2",
                "Double", "2023-01-01", "Maintenance", "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.ALLOCATION_MOVED, "allocation-moved", ctx);

        verify(templateEngine).process(eq("email/allocation-moved"), any(Context.class));
    }

    @Test
    void sendTemplateEmail_withDocumentCommentContext_shouldProcessCorrectTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var ctx = new DocumentCommentContext("Student", "ID", "id.jpg", "DOC-1",
                "2023-01-01", "REJECTED", "Blurry", "Admin", "2023-01-02", "url");

        emailService.sendTemplateEmail("test@example.com", EmailSubjects.DOCUMENT_COMMENT, "document-comment", ctx);

        verify(templateEngine).process(eq("email/document-comment"), any(Context.class));
    }
}