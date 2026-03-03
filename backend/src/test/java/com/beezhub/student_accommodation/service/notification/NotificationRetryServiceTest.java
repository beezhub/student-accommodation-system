package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRetryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationRetryService notificationRetryService;

    // -------------------------------------------------------------------------
    // retryFailedNotifications — batch scheduling tests
    // -------------------------------------------------------------------------

    @Test
    void retryFailedNotifications_failedNotificationExists_retriesNotification() {
        ReflectionTestUtils.setField(notificationRetryService, "retryDelayMinutes", 5);

        Notification failedNotification = new Notification();
        failedNotification.setId(1L);
        failedNotification.setStatus(NotificationStatus.FAILED);
        failedNotification.setRetryCount(0);
        failedNotification.setMaxRetries(3);
        failedNotification.setType(NotificationType.APPLICATION_APPROVED);
        // No templateData — dispatch is skipped but retryCount is still incremented

        when(notificationRepository.findFailedNotificationsForRetry(eq(NotificationStatus.FAILED), any(LocalDateTime.class)))
                .thenReturn(List.of(failedNotification));

        notificationRetryService.retryFailedNotifications();

        verify(notificationRepository, times(1)).save(failedNotification);
        assertEquals(1, failedNotification.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, failedNotification.getStatus());
    }

    @Test
    void retryFailedNotifications_maxRetriesExceeded_marksPermanentlyFailed() {
        ReflectionTestUtils.setField(notificationRetryService, "retryDelayMinutes", 5);

        Notification maxedNotification = new Notification();
        maxedNotification.setId(1L);
        maxedNotification.setStatus(NotificationStatus.FAILED);
        maxedNotification.setRetryCount(3);
        maxedNotification.setMaxRetries(3);
        maxedNotification.setType(NotificationType.APPLICATION_APPROVED);

        when(notificationRepository.findFailedNotificationsForRetry(eq(NotificationStatus.FAILED), any(LocalDateTime.class)))
                .thenReturn(List.of(maxedNotification));
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        notificationRetryService.retryFailedNotifications();

        assertEquals(NotificationStatus.PERMANENTLY_FAILED, maxedNotification.getStatus());
        verify(notificationRepository).save(maxedNotification);
        verify(counter).increment();
    }

    // -------------------------------------------------------------------------
    // retryNotification — unit-level tests
    // -------------------------------------------------------------------------

    @Test
    void retryNotification_validNotification_incrementsRetryCountAndSaves() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(1);
        notification.setMaxRetries(3);
        notification.setType(NotificationType.APPLICATION_APPROVED);
        // No templateData — dispatch is skipped, but retryCount is incremented and saved

        notificationRetryService.retryNotification(notification);

        assertEquals(2, notification.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        verify(notificationRepository).save(notification);
    }

    @Test
    void retryNotification_withValidTemplateData_sendsEmailAndMarksSent() throws Exception {
        Notification notification = new Notification();
        notification.setId(2L);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(0);
        notification.setMaxRetries(3);
        notification.setType(NotificationType.APPLICATION_APPROVED);
        notification.setRecipient("student@example.com");
        notification.setSubject("Application Approved");

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("templateName", "application-approved");
        templateData.put("studentName", "Alice");
        notification.setTemplateData(templateData);

        doNothing().when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), any());

        notificationRetryService.retryNotification(notification);

        verify(emailService).sendTemplateEmail(
                eq("student@example.com"),
                eq("Application Approved"),
                eq("application-approved"),
                eq(templateData));
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNotNull(notification.getSentAt());
        // save is called twice: once after markAsRetrying, once after markAsSent
        verify(notificationRepository, times(2)).save(notification);
    }

    @Test
    void retryNotification_emailThrows_marksFailedAndSetsErrorMessage() throws Exception {
        Notification notification = new Notification();
        notification.setId(3L);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(1);
        notification.setMaxRetries(3);
        notification.setType(NotificationType.ROOM_ALLOCATED);
        notification.setRecipient("student@example.com");
        notification.setSubject("Room Confirmed");

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("templateName", "allocation-confirmed");
        notification.setTemplateData(templateData);

        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), any());

        notificationRetryService.retryNotification(notification);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals("SMTP connection refused", notification.getErrorMessage());
        // save called twice: once after markAsRetrying, once after markAsFailed
        verify(notificationRepository, times(2)).save(notification);
    }

    @Test
    void retryNotification_nullTemplateData_skipsDispatchAndRemainsRetrying() {
        Notification notification = new Notification();
        notification.setId(4L);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(0);
        notification.setMaxRetries(3);
        notification.setType(NotificationType.APPLICATION_APPROVED);
        notification.setTemplateData(null);

        notificationRetryService.retryNotification(notification);

        // Email must NOT be dispatched
        verifyNoInteractions(emailService);
        // Status remains RETRYING (early return after markAsRetrying + incrementRetryCount)
        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        assertEquals(1, notification.getRetryCount());
        verify(notificationRepository, times(1)).save(notification);
    }
}
