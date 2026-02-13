package com.beezhub.student_accommodation.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        // Reflection to set @Value field
        try {
            java.lang.reflect.Field fromEmailField = EmailService.class.getDeclaredField("fromEmail");
            fromEmailField.setAccessible(true);
            fromEmailField.set(emailService, "noreply@example.com");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendSimpleEmail_shouldSendEmail() {
        emailService.sendSimpleEmail("test@example.com", "Subject", "Body");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendHtmlEmail_shouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("test@example.com", "Subject", "<p>Body</p>");

        verify(mailSender).send(mimeMessage);
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
}
