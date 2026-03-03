package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
import com.beezhub.student_accommodation.service.notification.EmailSubjects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import com.beezhub.student_accommodation.model.event.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailDispatchService emailDispatchService;

    @Async("notificationExecutor")
    public void sendWelcomeEmail(AppUser user) {
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending welcome email to {}", email);

        Notification notification = createNotification(
                email,
                EmailSubjects.WELCOME,
                NotificationType.WELCOME_EMAIL,
                user,
                user.getId(),
                "AppUser");

        try {
            emailDispatchService.sendWelcomeEmail(user, studentName, email);
            markNotificationAsSent(notification);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    public void sendPasswordResetEmail(AppUser user, String resetToken) {
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending password reset email to {}", email);

        Notification notification = createNotification(
                email,
                EmailSubjects.PASSWORD_RESET,
                NotificationType.PASSWORD_RESET,
                user,
                user.getId(),
                "AppUser");

        try {
            emailDispatchService.sendPasswordResetEmail(studentName, email, resetToken, notification.getSubject());
            markNotificationAsSent(notification);
        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    @Transactional
    public Notification createNotification(String recipient, String subject,
            NotificationType type, AppUser user,
            Long referenceId, String referenceType) {
        return createNotification(recipient, subject, type, user, referenceId, referenceType, null);
    }

    @Transactional
    public Notification createNotification(String recipient, String subject,
            NotificationType type, AppUser user,
            Long referenceId, String referenceType,
            Map<String, Object> templateData) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .subject(subject)
                .type(type)
                .status(NotificationStatus.PENDING)
                .user(user)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .retryCount(0)
                .maxRetries(3)
                .templateData(templateData)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public void markNotificationAsSent(Notification notification) {
        notification.markAsSent();
        notificationRepository.save(notification);
        log.debug("Notification {} marked as sent", notification.getId());
    }

    @Transactional
    public void handleNotificationFailure(Notification notification, Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        notification.markAsFailed(errorMessage);
        notificationRepository.save(notification);

        log.error("Failed to send notification {} (type: {}) to {}. Error: {}. Will retry if under max attempts.",
                notification.getId(), notification.getType(), notification.getRecipient(), errorMessage, e);
    }

    @Async("notificationExecutor")
    public void sendApplicationStatusNotification(Application application, ApplicationStatus status) {
        if (application.getStudent() == null || application.getStudent().getAppUser() == null) {
            log.warn("Cannot send application status notification: Student or AppUser is null for application {}",
                    application.getId());
            return;
        }

        AppUser user = application.getStudent().getAppUser();
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        if (status == ApplicationStatus.APPROVED) {
            Notification notification = createNotification(
                    email,
                    EmailSubjects.APPLICATION_APPROVED,
                    NotificationType.APPLICATION_APPROVED,
                    user,
                    application.getId(),
                    "Application");
            try {
                emailDispatchService.sendApplicationApprovedEmail(application, studentName, email);
                markNotificationAsSent(notification);
            } catch (Exception e) {
                handleNotificationFailure(notification, e);
            }
        } else if (status == ApplicationStatus.REJECTED) {
            Notification notification = createNotification(
                    email,
                    EmailSubjects.APPLICATION_REJECTED,
                    NotificationType.APPLICATION_REJECTED,
                    user,
                    application.getId(),
                    "Application");
            try {
                emailDispatchService.sendApplicationRejectedEmail(application, studentName, email);
                markNotificationAsSent(notification);
            } catch (Exception e) {
                handleNotificationFailure(notification, e);
            }
        } else {
            log.warn("Notification not implemented for application status: {}", status);
        }
    }
}
