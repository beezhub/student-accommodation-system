package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
import com.beezhub.student_accommodation.service.notification.EmailSubjects;
import com.beezhub.student_accommodation.service.notification.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationPersistenceService notificationPersistence;
    private final EmailDispatchService emailDispatchService;

    @Async("notificationExecutor")
    public void sendWelcomeEmail(AppUser user) {
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending welcome email to {}", email);

        // Persisting the template data is what makes a failed welcome email recoverable: the
        // retry job rebuilds the email from this map, and without it the row was unsendable and
        // stranded (GH-141).
        Notification notification = notificationPersistence.createNotification(
                email,
                EmailSubjects.WELCOME,
                NotificationType.WELCOME_EMAIL,
                user,
                user.getId(),
                "AppUser",
                emailDispatchService.welcomeTemplateData(user, studentName, email));

        try {
            emailDispatchService.sendWelcomeEmail(user, studentName, email);
            notificationPersistence.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationPersistence.handleNotificationFailure(notification, e);
        }
    }

    @Async("notificationExecutor")
    public void sendPasswordResetEmail(AppUser user, String resetToken) {
        String studentName = user.getFirstName() + " " + user.getLastName();
        String email = user.getEmail();

        log.info("Sending password reset email to {}", email);

        // Deliberately stores no template data, unlike the welcome path above. The only variable
        // this template needs is the reset URL, which embeds a live single-use token. Persisting
        // it would write that token in cleartext to the notification table, and a retry minutes
        // later could mail a token the user has already consumed. A secret-bearing notification
        // is better left unsendable, which the retry job now terminates cleanly (GH-141).
        Notification notification = notificationPersistence.createNotification(
                email,
                EmailSubjects.PASSWORD_RESET,
                NotificationType.PASSWORD_RESET,
                user,
                user.getId(),
                "AppUser");

        try {
            emailDispatchService.sendPasswordResetEmail(studentName, email, resetToken, notification.getSubject());
            notificationPersistence.markNotificationAsSent(notification);
        } catch (Exception e) {
            notificationPersistence.handleNotificationFailure(notification, e);
        }
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
            Notification notification = notificationPersistence.createNotification(
                    email,
                    EmailSubjects.APPLICATION_APPROVED,
                    NotificationType.APPLICATION_APPROVED,
                    user,
                    application.getId(),
                    "Application");
            try {
                emailDispatchService.sendApplicationApprovedEmail(application, studentName, email);
                notificationPersistence.markNotificationAsSent(notification);
            } catch (Exception e) {
                notificationPersistence.handleNotificationFailure(notification, e);
            }
        } else if (status == ApplicationStatus.REJECTED) {
            Notification notification = notificationPersistence.createNotification(
                    email,
                    EmailSubjects.APPLICATION_REJECTED,
                    NotificationType.APPLICATION_REJECTED,
                    user,
                    application.getId(),
                    "Application");
            try {
                emailDispatchService.sendApplicationRejectedEmail(application, studentName, email);
                notificationPersistence.markNotificationAsSent(notification);
            } catch (Exception e) {
                notificationPersistence.handleNotificationFailure(notification, e);
            }
        } else {
            log.warn("Notification not implemented for application status: {}", status);
        }
    }
}
