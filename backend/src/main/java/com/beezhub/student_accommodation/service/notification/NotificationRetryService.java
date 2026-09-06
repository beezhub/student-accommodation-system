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

import org.springframework.data.domain.PageRequest;

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

    /**
     * Caps one pass. The whole pass runs in a single transaction, and dropping the
     * retry-count predicate from the scan means the first run after deploy would otherwise see
     * the entire historical backlog of failed rows at once (GH-141).
     */
    @Value("${app.notification.retry-batch-size:200}")
    private int retryBatchSize;

    private static final int MAX_RETRIES = 3;

    /**
     * RETRYING is in scope because rows were stranded in it by an early return that committed
     * the status before discovering it could not rebuild the email.
     */
    private static final List<NotificationStatus> RETRYABLE_STATUSES =
            List.of(NotificationStatus.FAILED, NotificationStatus.RETRYING);

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
        List<Notification> candidates = notificationRepository.findRetryableNotifications(
                RETRYABLE_STATUSES, threshold, PageRequest.of(0, retryBatchSize));

        if (candidates.isEmpty()) {
            return;
        }
        log.info("action=notificationRetryScan candidates={}", candidates.size());

        for (Notification notification : candidates) {
            // Exhaustion is checked before backoff, and the order is load-bearing. With the
            // backoff test first, a row that has used up its attempts could be skipped for not
            // being due yet and would never reach a terminal state (GH-141).
            if (!notification.canRetry()) {
                markAsPermanentlyFailed(notification, "max retries exhausted");
                continue;
            }
            if (!isReadyForRetry(notification)) {
                log.debug("action=notificationRetrySkipped reason=backoff notificationId={}",
                        notification.getId());
                continue;
            }
            retryNotification(notification);
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
     * Attempts to re-dispatch a single notification.
     *
     * <p>The template name is resolved before anything is mutated. The old order marked the row
     * RETRYING and incremented the count first, then returned when the template data turned out
     * to be missing. That left the row committed as RETRYING, which the scan did not look at, so
     * it sat there forever (GH-141).
     *
     * <p>A row with no usable template data is terminated rather than retried. Nothing about it
     * becomes reconstructable by waiting, so counting attempts against it only churns it three
     * times with no dispatch before it dies anyway.
     */
    @Transactional
    public void retryNotification(Notification notification) {
        String templateName = templateNameOf(notification);
        if (templateName == null) {
            markAsPermanentlyFailed(notification, "templateData missing or has no templateName");
            return;
        }

        log.info("action=notificationRetry notificationId={} attempt={}/{}",
                notification.getId(), notification.getRetryCount() + 1, MAX_RETRIES);

        notification.markAsRetrying();
        notification.incrementRetryCount();
        notificationRepository.save(notification);

        Map<String, Object> templateData = notification.getTemplateData();

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

    /** Null when the stored data cannot name a template, which is what makes a row unsendable. */
    private String templateNameOf(Notification notification) {
        Map<String, Object> templateData = notification.getTemplateData();
        if (templateData == null) {
            return null;
        }
        Object templateName = templateData.get("templateName");
        if (templateName instanceof String name && !name.isBlank()) {
            return name;
        }
        return null;
    }

    /**
     * The one terminal state. Until GH-141 this was unreachable, because the scan already
     * filtered out every row that had exhausted its attempts.
     *
     * <p>The error message is only set when there is not one already, so the last SMTP
     * diagnostic survives for a row that died of exhaustion rather than of missing data.
     */
    private void markAsPermanentlyFailed(Notification notification, String reason) {
        log.warn("action=notificationPermanentlyFailed notificationId={} type={} reason={}",
                notification.getId(), notification.getType(), reason);
        notification.setStatus(NotificationStatus.PERMANENTLY_FAILED);
        notification.setLastAttemptAt(LocalDateTime.now());
        if (notification.getErrorMessage() == null) {
            notification.setErrorMessage(reason);
        }
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

        // The recipient is deliberately absent: the house rule forbids PII at ERROR, and the
        // notification id is enough to find the row.
        log.error("action=notificationRetryFailed notificationId={} type={} error={}",
                notification.getId(), notification.getType(), errorMessage, e);
    }
}
