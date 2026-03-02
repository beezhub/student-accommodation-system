package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
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

    @Mock private NotificationRepository notificationRepository;
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

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
    }

    // ─────────────────────────────────────────────────────────────
    // createNotification
    // ─────────────────────────────────────────────────────────────

    @Test
    void createNotification_buildsAndSavesNotification() {
        Notification result = notificationService.createNotification(
                "student@test.com",
                "Welcome",
                NotificationType.WELCOME_EMAIL,
                user,
                user.getId(),
                "AppUser"
        );

        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    // ─────────────────────────────────────────────────────────────
    // markNotificationAsSent
    // ─────────────────────────────────────────────────────────────

    @Test
    void markNotificationAsSent_setsStatusAndSaves() {
        notificationService.markNotificationAsSent(savedNotification);

        assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(notificationRepository).save(savedNotification);
    }

    // ─────────────────────────────────────────────────────────────
    // handleNotificationFailure
    // ─────────────────────────────────────────────────────────────

    @Test
    void handleNotificationFailure_setsFailedStatusAndSaves() {
        RuntimeException ex = new RuntimeException("SMTP error");

        notificationService.handleNotificationFailure(savedNotification, ex);

        assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(savedNotification.getErrorMessage()).isEqualTo("SMTP error");
        verify(notificationRepository).save(savedNotification);
    }

    @Test
    void handleNotificationFailure_nullExceptionMessage_usesSimpleClassName() {
        // NullPointerException() default constructor leaves getMessage() == null
        NullPointerException ex = new NullPointerException();

        notificationService.handleNotificationFailure(savedNotification, ex);

        assertThat(savedNotification.getErrorMessage()).isEqualTo("NullPointerException");
        verify(notificationRepository).save(savedNotification);
    }

    // ─────────────────────────────────────────────────────────────
    // sendWelcomeEmail
    // ─────────────────────────────────────────────────────────────

    @Test
    void sendWelcomeEmail_success_marksNotificationAsSent() {
        doNothing().when(emailDispatchService).sendWelcomeEmail(any(), any(), any());

        notificationService.sendWelcomeEmail(user);

        verify(emailDispatchService).sendWelcomeEmail(eq(user), eq("Alice Smith"), eq("student@test.com"));
        // notification saved twice: once on create, once on mark-as-sent
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void sendWelcomeEmail_dispatchThrows_marksNotificationAsFailed() {
        doThrow(new RuntimeException("SMTP down")).when(emailDispatchService)
                .sendWelcomeEmail(any(), any(), any());

        notificationService.sendWelcomeEmail(user);

        // Still saved on failure (handleNotificationFailure saves)
        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
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
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void sendPasswordResetEmail_dispatchThrows_marksNotificationAsFailed() {
        doThrow(new RuntimeException("Mail failure")).when(emailDispatchService)
                .sendPasswordResetEmail(any(), any(), any(), any());

        notificationService.sendPasswordResetEmail(user, "token");

        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
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
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void sendApplicationStatusNotification_rejected_sendsRejectedEmail() {
        Application app = buildApplication(user);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.REJECTED);

        verify(emailDispatchService).sendApplicationRejectedEmail(eq(app), eq("Alice Smith"), eq("student@test.com"));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void sendApplicationStatusNotification_otherStatus_logsWarnAndDoesNothing() {
        Application app = buildApplication(user);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.PENDING);

        verifyNoInteractions(emailDispatchService);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void sendApplicationStatusNotification_nullStudent_logsWarnAndDoesNothing() {
        Application app = new Application();
        app.setId(200L);
        app.setStudent(null);

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.APPROVED);

        verifyNoInteractions(emailDispatchService);
        verifyNoInteractions(notificationRepository);
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
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void sendApplicationStatusNotification_approved_dispatchThrows_marksAsFailed() {
        Application app = buildApplication(user);
        doThrow(new RuntimeException("Email down")).when(emailDispatchService)
                .sendApplicationApprovedEmail(any(), any(), any());

        notificationService.sendApplicationStatusNotification(app, ApplicationStatus.APPROVED);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(savedNotification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}
