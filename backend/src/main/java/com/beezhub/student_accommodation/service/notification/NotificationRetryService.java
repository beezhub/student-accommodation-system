package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryService {

    private final NotificationRepository notificationRepository;
    private final MeterRegistry meterRegistry;
    private final EmailService emailService;

    @Value("${app.notification.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    private static final int MAX_RETRIES = 3;

    /**
     * Scans for FAILED notifications that are ready to retry.
     *
     * <p>A notification is eligible when its {@code lastAttemptAt + exponentialBackoff(retryCount)}
     * is in the past — i.e. enough time has elapsed since the last attempt.
     * If {@code lastAttemptAt} is null the simple {@code retryDelayMinutes} threshold is used.
     *
     * <p>Wired into {@link com.beezhub.student_accommodation.service.ScheduledJobRunner}
     * via {@code runNotificationRetry()}.
     */
    @Transactional
    public void retryFailedNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(retryDelayMinutes);
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsForRetry(NotificationStatus.FAILED, threshold);

        if (!failedNotifications.isEmpty()) {
            log.info("Found {} failed notifications to retry", failedNotifications.size());

            for (Notification notification : failedNotifications) {
                // Enforce exponential backoff: skip if not enough time has elapsed
                // since the last attempt relative to the retry count.
                if (!isReadyForRetry(notification)) {
                    log.debug("Notification {} not yet ready for retry (backoff active)", notification.getId());
                    continue;
                }

                if (notification.canRetry()) {
                    retryNotification(notification);
                } else {
                    markAsPermanentlyFailed(notification);
                }
            }
        }
    }

    /**
     * Checks whether enough time has elapsed since the last attempt,
     * using exponential backoff based on the current retry count.
     */
    private boolean isReadyForRetry(Notification notification) {
        if (notification.getLastAttemptAt() == null) {
            return true;
        }
        long backoffMs = getExponentialBackoff(notification.getRetryCount());
        LocalDateTime nextAttemptAt = notification.getLastAttemptAt()
                .plusNanos(backoffMs * 1_000_000L);
        return LocalDateTime.now().isAfter(nextAttemptAt);
    }

    /**
     * Attempts to re-dispatch a single failed notification.
     *
     * <p>Reads {@code templateData} stored at creation time. If null (pre-migration row or
     * unsupported type), logs a warning and skips actual dispatch but still increments retry
     * count to avoid infinite polling.
     */
    @Transactional
    public void retryNotification(Notification notification) {
        log.info("Retrying notification {} (attempt {}/{})",
                notification.getId(), notification.getRetryCount() + 1, MAX_RETRIES);

        notification.markAsRetrying();
        notification.incrementRetryCount();
        notificationRepository.save(notification);

        Map<String, Object> templateData = notification.getTemplateData();

        if (templateData == null) {
            log.warn("Notification {} has no templateData — cannot reconstruct email. Skipping dispatch.",
                    notification.getId());
            // Leave as RETRYING; next cycle will see retryCount incremented.
            // Once canRetry() returns false it will be permanently failed.
            return;
        }

        String templateName = (String) templateData.get("templateName");
        if (templateName == null) {
            log.warn("Notification {} templateData missing 'templateName' key — skipping dispatch.",
                    notification.getId());
            return;
        }

        try {
            emailService.sendTemplateEmail(
                    notification.getRecipient(),
                    notification.getSubject(),
                    templateName,
                    templateData);

            notification.markAsSent();
            notificationRepository.save(notification);
            log.info("Notification {} retried successfully via template '{}'",
                    notification.getId(), templateName);

        } catch (Exception e) {
            handleNotificationFailure(notification, e);
        }
    }

    private void markAsPermanentlyFailed(Notification notification) {
        log.warn("Notification {} exceeded max retries ({}), marking as permanently failed",
                notification.getId(), MAX_RETRIES);
        notification.setStatus(NotificationStatus.PERMANENTLY_FAILED);
        notificationRepository.save(notification);
        meterRegistry.counter("sas.notifications.permanently_failed",
                "type", notification.getType() != null ? notification.getType().name() : "UNKNOWN")
                .increment();
    }

    /**
     * Returns the minimum milliseconds to wait before the next retry attempt,
     * calculated as {@code 2^retryCount * 1000 ms}.
     */
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
