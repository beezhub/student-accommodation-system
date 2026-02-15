package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryService {

    private final NotificationRepository notificationRepository;

    @Value("${app.notification.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelayString = "${app.notification.retry-check-interval-ms:300000}")
    @Transactional
    public void retryFailedNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(retryDelayMinutes);
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry(NotificationStatus.FAILED, threshold);

        if (!failedNotifications.isEmpty()) {
            log.info("Found {} failed notifications to retry", failedNotifications.size());

            for (Notification notification : failedNotifications) {
                if (notification.canRetry()) {
                    retryNotification(notification);
                } else {
                    log.warn("Notification {} exceeded max retries ({}), marking as permanently failed",
                            notification.getId(), MAX_RETRIES);
                }
            }
        }
    }

    @Transactional
    public void retryNotification(Notification notification) {
        log.info("Retrying notification {} (attempt {}/{})",
                notification.getId(), notification.getRetryCount() + 1, MAX_RETRIES);

        notification.markAsRetrying();
        notification.incrementRetryCount();
        notificationRepository.save(notification);

        try {
            Thread.sleep(getExponentialBackoff(notification.getRetryCount()));

            switch (notification.getType()) {
                case PASSWORD_RESET -> log.warn("Password reset tokens may have expired, skipping retry");
                default -> log.info("Retry logic for {} not yet implemented", notification.getType());
            }

        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private long getExponentialBackoff(int retryCount) {
        return (long) Math.pow(2, retryCount) * 1000;
    }

    private void handleNotificationFailure(Notification notification, Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        notification.markAsFailed(errorMessage);
        notificationRepository.save(notification);

        log.error("Failed to retry notification {} (type: {}) to {}. Error: {}",
                notification.getId(), notification.getType(), notification.getRecipient(), errorMessage, e);
    }
}
