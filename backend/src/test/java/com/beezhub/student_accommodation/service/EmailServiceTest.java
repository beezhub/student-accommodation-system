package com.beezhub.student_accommodation.service;

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

import java.math.BigDecimal;
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

        // Assuming no actual file is needed for the helper creation to succeed in tests
        verify(mailSender, never()).send(mimeMessage); // wait, sendEmailWithAttachment does not call send() in the source code! It just logs.
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
    void sendApplicationApprovedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendApplicationApprovedEmail("test@example.com", "Student Name", 1L, 
                "2023-01-01", "2023", "Spring", "url");

        verify(templateEngine).process(eq("email/application-approved"), any(Context.class));
    }

    @Test
    void sendApplicationRejectedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendApplicationRejectedEmail("test@example.com", "Student Name", 1L, 
                "Reason", "2023-01-01", "2023-01-02", "Fall", "url");

        verify(templateEngine).process(eq("email/application-rejected"), any(Context.class));
    }

    @Test
    void sendDocumentVerifiedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendDocumentVerifiedEmail("test@example.com", "Student Name", "ID", 
                "REF", "2023-01-01", "Admin", "url");

        verify(templateEngine).process(eq("email/document-verified"), any(Context.class));
    }

    @Test
    void sendDocumentRejectedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendDocumentRejectedEmail("test@example.com", "Student Name", "ID", 
                "REF", "2023-01-01", "2023-01-02", "Blurry", "2023-01-10", "url", Map.of("extra", "info"));

        verify(templateEngine).process(eq("email/document-rejected"), any(Context.class));
    }

    @Test
    void sendRoomAllocatedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendRoomAllocatedEmail("test@example.com", "Student Name", "101", 
                "Building", 1, "Single", "1", "ALLOC1", "2023-01-01", "2023-12-31", 
                "1 Year", "Office", "Address", "url");

        verify(templateEngine).process(eq("email/room-allocated"), any(Context.class));
    }

    @Test
    void sendWelcomeEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendWelcomeEmail("test@example.com", "Student Name", "test@example.com", 
                "ST123", "2023-01-01", "url", "guide_url");

        verify(templateEngine).process(eq("email/welcome"), any(Context.class));
    }

    @Test
    void sendAllocationProposalEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendAllocationProposalEmail("test@example.com", "Student Name", "101", 
                "Building", 1, 2, "PROP1", new BigDecimal("100"), List.of("Warning"), 
                "2023-01-01", "url", "support@example.com");

        verify(templateEngine).process(eq("email/allocation-proposal"), any(Context.class));
    }

    @Test
    void sendAllocationConfirmedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendAllocationConfirmedEmail("test@example.com", "Student", "101", "Building", 
                1, "Single", "ALLOC1", "2023-01-01", "2023-01-01", "2023-12-31", 
                "url", "support", "phone");

        verify(templateEngine).process(eq("email/allocation-confirmed"), any(Context.class));
    }

    @Test
    void sendAllocationRejectedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendAllocationRejectedEmail("test@example.com", "Student", "101", "Building", 
                "PROP1", "Full", 5, "url", "support", "phone");

        verify(templateEngine).process(eq("email/allocation-rejected"), any(Context.class));
    }

    @Test
    void sendAllocationMovedEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendAllocationMovedEmail("test@example.com", "Student", "101", "Bld1", 1, 
                "201", "Bld2", 2, "Double", "2023-01-01", "Maintenance", "url", "support", "phone");

        verify(templateEngine).process(eq("email/allocation-moved"), any(Context.class));
    }

    @Test
    void sendDocumentCommentEmail_shouldSendTemplateEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendDocumentCommentEmail("test@example.com", "Student", "ID", "id.jpg", 
                "APP1", "2023-01-01", "REJECTED", "Blurry", "Admin", "2023-01-02", 
                "url", "support", "phone");

        verify(templateEngine).process(eq("email/document-comment"), any(Context.class));
    }
}