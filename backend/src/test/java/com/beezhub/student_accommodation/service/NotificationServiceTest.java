package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
import com.beezhub.student_accommodation.service.notification.NotificationPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private NotificationPersistenceService notificationPersistence;
    @Mock private EmailDispatchService emailDispatchService;

    @InjectMocks
    private NotificationService notificationService;

    private AppUser user;
    private Notification savedNotification;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(1L)
                .email("student@test.com")
                .firstName("Alice")
                .lastName("Smith")
                .userRole(UserRole.STUDENT)
                .build();

        savedNotification = Notification.builder()
                .id(10L)
                .recipient("student@test.com")
                .subject("Welcome")
                .type(NotificationType.WELCOME_EMAIL)
                .status(NotificationStatus.PENDING)
                .user(user)
                .referenceId(user.getId())
                .referenceType("AppUser")
                .retryCount(0)
                .maxRetries(3)
                .build();

        when(notificationPersistence.createNotification(
                anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString()))
                .thenReturn(savedNotification);
    }

    // ─────────────────────────────────────────────────────────────
    // sendWelcomeEmail
    // ─────────────────────────────────────────────────────────────

    @Test
    void sendWelcomeEmail_success_marksNotificationAsSent() {
        doNothing().when(emailDispatchService).sendWelcomeEmail(any(), any(), any());

        notificationService.sendWelcomeEmail(user);

        verify(emailDispatchService).sendWelcomeEmail(eq(user), eq("Alice Smith"), eq("student@test.com"));
        verify(notificationPersistence).createNotification(
                anyString(), anyString(), any(NotificationType.class), any(), anyLong(), anyString());
        verify(notificationPersistence).markNotificationAsSent(savedNotification);
    }

    @Test
    void sendWelcomeEmail_dispatchThrows_marksNotificationAsFailed() {
        doThrow(new RuntimeException("SMTP down")).when(emailDispatchService)
                .sendWelcomeEmail(any(), any(), any());

        notificationService.sendWelcomeEmail(user);

        verify(notificationPersistence).handleNotificationFailure(eq(savedNotification), any(Exception.class));
    }

    // ─────────────────────────────────────────────────────────────
    // sendPasswordResetEmail
    // ─────────────────────────────────────────────────────────────

    @Test
    void sendPasswordResetEmail_success_marksNotificationAsSent() {
        doNothing().when(emailDispatchService).sendPasswordResetEmail(any(), any(), any(), any());

        notificationService.sendPasswordResetEmail(user, "reset-token-123");

        verify(emailDispatchService).sendPasswordResetEmail(
                eq("Alice Smith"), eq("student@test.com"), eq("reset-token-123"), any());
        verify(notificationPersistence).markNotificationAsSent(savedNotification);
    }

    @Test
    void sendPasswordResetEmail_dispatchThrows_marksNotificationAsFailed() {
        doThrow(new RuntimeException("Mail failure")).when(emailDispatchService)
                .sendPasswordResetEmail(any(), any(), any(), any());

        notificationService.sendPasswordResetEmail(user, "token");

        verify(notificationPersistence).handleNotificationFailure(eq(savedNotification), any(Exception.class));
    }

    // ─────────────────────────────────────────────────────────────
    // sendApplicationStatusNotification
    // ─────────────────────────────────────────────────────────────

    private Application buildApplication(AppUser appUser) {
        Student student = new Student();
        student.setId(1L);
        student.setAppUser(appUser);

        Application app = new Application();
        app.setId(100L);
        app.setStudent(student);
        return app;
    }

    @Test
    void sendApplicationStatusNotification_approved_sendsApprovedEmail() {
        Application app = buildApplication(user);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.APPROVED);

        verify(emailDispatchService).sendApplicationApprovedEmail(eq(app), eq("Alice Smith"), eq("student@test.com"));
        verify(notificationPersistence).markNotificationAsSent(savedNotification);
    }

    @Test
    void sendApplicationStatusNotification_rejected_sendsRejectedEmail() {
        Application app = buildApplication(user);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.REJECTED);

        verify(emailDispatchService).sendApplicationRejectedEmail(eq(app), eq("Alice Smith"), eq("student@test.com"));
        verify(notificationPersistence).markNotificationAsSent(savedNotification);
    }

    @Test
    void sendApplicationStatusNotification_otherStatus_logsWarnAndDoesNothing() {
        Application app = buildApplication(user);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.PENDING);

        verifyNoInteractions(emailDispatchService);
        verifyNoInteractions(notificationPersistence);
    }

    @Test
    void sendApplicationStatusNotification_nullStudent_logsWarnAndDoesNothing() {
        Application app = new Application();
        app.setId(200L);
        app.setStudent(null);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.APPROVED);

        verifyNoInteractions(emailDispatchService);
        verifyNoInteractions(notificationPersistence);
    }

    @Test
    void sendApplicationStatusNotification_nullAppUser_logsWarnAndDoesNothing() {
        Student student = new Student();
        student.setId(2L);
        student.setAppUser(null);

        Application app = new Application();
        app.setId(201L);
        app.setStudent(student);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.APPROVED);

        verifyNoInteractions(emailDispatchService);
        verifyNoInteractions(notificationPersistence);
    }

    @Test
    void sendApplicationStatusNotification_approved_dispatchThrows_marksAsFailed() {
        Application app = buildApplication(user);
        doThrow(new RuntimeException("Email down")).when(emailDispatchService)
                .sendApplicationApprovedEmail(any(), any(), any());

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.APPROVED);

        verify(notificationPersistence).handleNotificationFailure(eq(savedNotification), any(Exception.class));
    }
}
