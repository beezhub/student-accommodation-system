package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRetryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationRetryService notificationRetryService;

    @Test
    void retryFailedNotifications_failedNotificationExists_retriesNotification() {
        ReflectionTestUtils.setField(notificationRetryService, "retryDelayMinutes", 5);

        Notification failedNotification = new Notification();
        failedNotification.setId(1L);
        failedNotification.setStatus(NotificationStatus.FAILED);
        failedNotification.setRetryCount(0);
        failedNotification.setMaxRetries(3);
        failedNotification.setType(NotificationType.APPLICATION_APPROVED);

        when(notificationRepository.findFailedNotificationsForRetry(eq(NotificationStatus.FAILED), any(LocalDateTime.class)))
                .thenReturn(List.of(failedNotification));

        notificationRetryService.retryFailedNotifications();

        verify(notificationRepository, times(1)).save(failedNotification);
        assertEquals(1, failedNotification.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, failedNotification.getStatus());
    }

    @Test
    void retryFailedNotifications_maxRetriesExceeded_logsWarning() {
        ReflectionTestUtils.setField(notificationRetryService, "retryDelayMinutes", 5);

        Notification maxedNotification = new Notification();
        maxedNotification.setId(1L);
        maxedNotification.setStatus(NotificationStatus.FAILED);
        maxedNotification.setRetryCount(3);
        maxedNotification.setMaxRetries(3);

        when(notificationRepository.findFailedNotificationsForRetry(eq(NotificationStatus.FAILED), any(LocalDateTime.class)))
                .thenReturn(List.of(maxedNotification));

        notificationRetryService.retryFailedNotifications();

        verify(notificationRepository, never()).save(maxedNotification);
    }

    @Test
    void retryNotification_validNotification_incrementsRetryCountAndSaves() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setRetryCount(1);
        notification.setMaxRetries(3);
        notification.setType(NotificationType.APPLICATION_APPROVED);

        notificationRetryService.retryNotification(notification);

        assertEquals(2, notification.getRetryCount());
        assertEquals(NotificationStatus.RETRYING, notification.getStatus());
        verify(notificationRepository).save(notification);
    }
}
