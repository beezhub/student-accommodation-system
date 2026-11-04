package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ExternalServiceException;
import com.beezhub.student_accommodation.service.notification.*;
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

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /** The SMTP login. The From address only when {@link #mailFrom} is blank. */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * The From address on every message. Blank falls back to the SMTP login, which is an email
     * address with most providers. SES SMTP logs in with an IAM access key ID, which is not an
     * address, and its IAM policy only allows no-reply@, so PROD sets MAIL_FROM (GH-356).
     */
    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${app.support-email:accommodation@university.edu}")
    private String supportEmail;

    @Value("${app.university-name:University}")
    private String universityName;

    @Value("${app.contact-phone:+27 (0) 11 000 0000}")
    private String contactPhone;

    String sender() {
        return mailFrom == null || mailFrom.isBlank() ? fromEmail : mailFrom;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Low-level senders (for internal/utility use only)
    // ──────────────────────────────────────────────────────────────────────────────

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new ExternalServiceException("Failed to send email", e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new ExternalServiceException("Failed to send email", e);
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String text, String attachmentPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sender());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            java.io.File attachmentFile = new java.io.File(attachmentPath);
            helper.addAttachment(attachmentFile.getName(),
                    new org.springframework.core.io.FileSystemResource(attachmentFile));

            mailSender.send(message);
            log.info("Email with attachment sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email with attachment to: {}", to, e);
            throw new ExternalServiceException("Failed to send email with attachment", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Primary template API — all email dispatching goes through here
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Sends a Thymeleaf template email.
     * Automatically injects layout variables (universityName, supportEmail,
     * contactPhone, currentYear) so every template has them available via _layout.html.
     */
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Map<String, Object> allVars = new HashMap<>(variables);
            allVars.put("universityName", universityName);
            allVars.put("supportEmail", supportEmail);
            allVars.put("contactPhone", contactPhone);
            allVars.put("currentYear", java.time.Year.now().getValue());

            Context context = new Context();
            context.setVariables(allVars);

            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Template email '{}' sent successfully to: {}", templateName, to);
        } catch (MessagingException e) {
            log.error("Failed to send template email '{}' to: {}", templateName, to, e);
            throw new ExternalServiceException("Failed to send template email", e);
        } catch (Exception e) {
            log.error("Error processing email template '{}' for: {}", templateName, to, e);
            throw new ExternalServiceException("Failed to process email template", e);
        }
    }

    /**
     * Sends a template email using a typed {@link EmailContext} record.
     * Layout variables are injected automatically — do not include them in the context.
     */
    public void sendTemplateEmail(String to, String subject, String templateName, EmailContext ctx) {
        sendTemplateEmail(to, subject, templateName, toVariableMap(ctx));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Context mapping — sealed switch exhausts all 22 EmailContext subtypes
    // ──────────────────────────────────────────────────────────────────────────────

    private Map<String, Object> toVariableMap(EmailContext ctx) {
        Map<String, Object> vars = new HashMap<>();
        switch (ctx) {
            case WelcomeContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("email", c.email());
                vars.put("studentId", c.studentId());
                vars.put("registrationDate", c.registrationDate());
                vars.put("loginUrl", c.loginUrl());
            }
            case PasswordResetContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("resetUrl", c.resetUrl());
            }
            case ApplicationApprovedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("applicationId", c.applicationId());
                vars.put("applicationDate", c.applicationDate());
                vars.put("academicYear", c.academicYear());
                vars.put("semester", c.semester());
                vars.put("portalUrl", c.portalUrl());
            }
            case ApplicationRejectedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("applicationId", c.applicationId());
                vars.put("rejectionReason", c.rejectionReason());
                vars.put("applicationDate", c.applicationDate());
                vars.put("reviewDate", c.reviewDate());
                vars.put("nextApplicationPeriod", c.nextApplicationPeriod());
                vars.put("portalUrl", c.portalUrl());
            }
            case DocumentVerifiedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("documentType", c.documentType());
                vars.put("documentReference", c.documentReference());
                vars.put("verificationDate", c.verificationDate());
                vars.put("verifiedBy", c.verifiedBy());
                vars.put("portalUrl", c.portalUrl());
            }
            case DocumentRejectedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("documentType", c.documentType());
                vars.put("documentReference", c.documentReference());
                vars.put("submissionDate", c.submissionDate());
                vars.put("reviewDate", c.reviewDate());
                vars.put("rejectionReason", c.rejectionReason());
                vars.put("resubmissionDeadline", c.resubmissionDeadline());
                vars.put("uploadUrl", c.uploadUrl());
                vars.put("feedbackPoints", c.feedbackPoints());
            }
            case DocumentCommentContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("documentType", c.documentType());
                vars.put("documentName", c.documentName());
                vars.put("documentReference", c.documentReference());
                vars.put("uploadDate", c.uploadDate());
                vars.put("documentStatus", c.documentStatus());
                vars.put("comment", c.comment());
                vars.put("reviewerName", c.reviewerName());
                vars.put("commentDate", c.commentDate());
                vars.put("portalUrl", c.portalUrl());
            }
            case RoomAllocatedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("floorNumber", c.floorNumber());
                vars.put("roomType", c.roomType());
                vars.put("bedNumber", c.bedNumber());
                vars.put("allocationId", c.allocationId());
                vars.put("startDate", c.startDate());
                vars.put("endDate", c.endDate());
                vars.put("duration", c.duration());
                vars.put("officeLocation", c.officeLocation());
                vars.put("buildingAddress", c.buildingAddress());
                vars.put("portalUrl", c.portalUrl());
            }
            case AllocationProposalContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("floorNumber", c.floorNumber());
                vars.put("roomCapacity", c.roomCapacity());
                vars.put("proposalId", c.proposalId());
                vars.put("score", c.score());
                vars.put("warnings", c.warnings());
                vars.put("moveInDate", c.moveInDate());
                vars.put("portalUrl", c.portalUrl());
            }
            case AllocationConfirmedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("floorNumber", c.floorNumber());
                vars.put("roomType", c.roomType());
                vars.put("allocationId", c.allocationId());
                vars.put("moveInDate", c.moveInDate());
                vars.put("startDate", c.startDate());
                vars.put("endDate", c.endDate());
                vars.put("portalUrl", c.portalUrl());
            }
            case AllocationRejectedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("proposalId", c.proposalId());
                vars.put("reason", c.reason());
                vars.put("waitingListPosition", c.waitingListPosition());
                vars.put("portalUrl", c.portalUrl());
            }
            case AllocationMovedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("oldRoomNumber", c.oldRoomNumber());
                vars.put("oldBuildingName", c.oldBuildingName());
                vars.put("oldFloorNumber", c.oldFloorNumber());
                vars.put("newRoomNumber", c.newRoomNumber());
                vars.put("newBuildingName", c.newBuildingName());
                vars.put("newFloorNumber", c.newFloorNumber());
                vars.put("newRoomType", c.newRoomType());
                vars.put("effectiveDate", c.effectiveDate());
                vars.put("reason", c.reason());
                vars.put("portalUrl", c.portalUrl());
            }
            case OfferDeclinedRequeueContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("totalDeclineCount", c.totalDeclineCount());
                vars.put("declinesRemaining", c.declinesRemaining());
                vars.put("declineReason", c.declineReason());
                vars.put("portalUrl", c.portalUrl());
            }
            case OfferDeclinedMaxReachedContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("totalDeclineCount", c.totalDeclineCount());
                vars.put("applicationCode", c.applicationCode());
                vars.put("portalUrl", c.portalUrl());
            }
            case OfferExpiredContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("applicationCode", c.applicationCode());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("portalUrl", c.portalUrl());
            }
            case OfferExpiryReminderContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("expiryDate", c.expiryDate());
                vars.put("portalUrl", c.portalUrl());
            }
            case CheckInReminderContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("deadlineDate", c.deadlineDate());
                vars.put("portalUrl", c.portalUrl());
            }
            case CheckInExpiredContext c -> {
                vars.put("studentName", c.studentName());
                vars.put("applicationCode", c.applicationCode());
                vars.put("roomNumber", c.roomNumber());
                vars.put("buildingName", c.buildingName());
                vars.put("portalUrl", c.portalUrl());
            }
        }
        return vars;
    }
}
