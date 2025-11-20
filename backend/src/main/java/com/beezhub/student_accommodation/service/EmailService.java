package com.beezhub.student_accommodation.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String text, String attachmentPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            log.info("Email with attachment sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email with attachment to: {}", to, e);
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }

    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Template email '{}' sent successfully to: {}", templateName, to);
        } catch (MessagingException e) {
            log.error("Failed to send template email '{}' to: {}", templateName, to, e);
            throw new RuntimeException("Failed to send template email", e);
        } catch (Exception e) {
            log.error("Error processing email template '{}' for: {}", templateName, to, e);
            throw new RuntimeException("Failed to process email template", e);
        }
    }

    public void sendApplicationApprovedEmail(String to, String studentName, Long applicationId,
                                            String applicationDate, String academicYear,
                                            String semester, String portalUrl) {
        Map<String, Object> variables = Map.of(
            "studentName", studentName,
            "applicationId", applicationId,
            "applicationDate", applicationDate,
            "academicYear", academicYear,
            "semester", semester,
            "portalUrl", portalUrl
        );
        sendTemplateEmail(to, "Application Approved - Student Accommodation",
                         "application-approved", variables);
    }

    public void sendApplicationRejectedEmail(String to, String studentName, Long applicationId,
                                            String rejectionReason, String applicationDate,
                                            String reviewDate, String nextApplicationPeriod,
                                            String portalUrl) {
        Map<String, Object> variables = Map.of(
            "studentName", studentName,
            "applicationId", applicationId,
            "rejectionReason", rejectionReason,
            "applicationDate", applicationDate,
            "reviewDate", reviewDate,
            "nextApplicationPeriod", nextApplicationPeriod,
            "portalUrl", portalUrl
        );
        sendTemplateEmail(to, "Application Status Update - Student Accommodation",
                         "application-rejected", variables);
    }

    public void sendDocumentVerifiedEmail(String to, String studentName, String documentType,
                                         String documentReference, String verificationDate,
                                         String verifiedBy, String portalUrl) {
        Map<String, Object> variables = Map.of(
            "studentName", studentName,
            "documentType", documentType,
            "documentReference", documentReference,
            "verificationDate", verificationDate,
            "verifiedBy", verifiedBy,
            "portalUrl", portalUrl
        );
        sendTemplateEmail(to, "Document Verified - Student Accommodation",
                         "document-verified", variables);
    }

    public void sendDocumentRejectedEmail(String to, String studentName, String documentType,
                                         String documentReference, String submissionDate,
                                         String reviewDate, String rejectionReason,
                                         String resubmissionDeadline, String uploadUrl,
                                         Map<String, Object> additionalVariables) {
        Map<String, Object> variables = new java.util.HashMap<>(Map.of(
            "studentName", studentName,
            "documentType", documentType,
            "documentReference", documentReference,
            "submissionDate", submissionDate,
            "reviewDate", reviewDate,
            "rejectionReason", rejectionReason,
            "resubmissionDeadline", resubmissionDeadline,
            "uploadUrl", uploadUrl
        ));
        if (additionalVariables != null) {
            variables.putAll(additionalVariables);
        }
        sendTemplateEmail(to, "Document Review Required - Student Accommodation",
                         "document-rejected", variables);
    }

    public void sendRoomAllocatedEmail(String to, String studentName, String roomNumber,
                                      String buildingName, Integer floorNumber, String roomType,
                                      String bedNumber, String allocationId, String startDate,
                                      String endDate, String duration, String officeLocation,
                                      String buildingAddress, String portalUrl) {
        Map<String, Object> variables = Map.ofEntries(
            Map.entry("studentName", studentName),
            Map.entry("roomNumber", roomNumber),
            Map.entry("buildingName", buildingName),
            Map.entry("floorNumber", floorNumber),
            Map.entry("roomType", roomType),
            Map.entry("bedNumber", bedNumber),
            Map.entry("allocationId", allocationId),
            Map.entry("startDate", startDate),
            Map.entry("endDate", endDate),
            Map.entry("duration", duration),
            Map.entry("officeLocation", officeLocation),
            Map.entry("buildingAddress", buildingAddress),
            Map.entry("portalUrl", portalUrl)
        );
        sendTemplateEmail(to, "Room Allocation Confirmation - Student Accommodation",
                         "room-allocated", variables);
    }

    public void sendWelcomeEmail(String to, String studentName, String email, String studentId,
                                String registrationDate, String loginUrl, String guideUrl) {
        Map<String, Object> variables = Map.of(
            "studentName", studentName,
            "email", email,
            "studentId", studentId,
            "registrationDate", registrationDate,
            "loginUrl", loginUrl,
            "guideUrl", guideUrl
        );
        sendTemplateEmail(to, "Welcome to Student Accommodation System",
                         "welcome", variables);
    }
}
