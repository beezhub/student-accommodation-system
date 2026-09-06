package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.service.allocation.AllocationBatchService;
import com.beezhub.student_accommodation.service.allocation.AllocationExpiryService;
import com.beezhub.student_accommodation.service.allocation.OfferReminderService;
import com.beezhub.student_accommodation.service.allocation.CheckInReminderService;
import com.beezhub.student_accommodation.service.document.DocumentRetentionJob;
import com.beezhub.student_accommodation.service.document.OrphanFileCleanupJob;
import com.beezhub.student_accommodation.service.notification.NotificationRetryService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Centralised scheduler that owns all {@code @Scheduled} annotations and wraps
 * each job body in the standard observability pattern:
 * <ul>
 *   <li>Structured log on start / complete / failure</li>
 *   <li>Prometheus counter {@code sas.jobs.{name}.processed} incremented by records processed</li>
 *   <li>Prometheus counter {@code sas.jobs.{name}.failures} incremented on uncaught exception</li>
 * </ul>
 *
 * One job failure does NOT affect other jobs — each wrapper has its own try/catch boundary.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobRunner {

    private final AllocationExpiryService allocationExpiryService;
    private final AllocationBatchService allocationBatchService;
    private final OrphanFileCleanupJob orphanFileCleanupJob;
    private final DocumentRetentionJob documentRetentionJob;
    private final OfferReminderService offerReminderService;
    private final CheckInReminderService checkInReminderService;
    private final NotificationRetryService notificationRetryService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void runOfferExpiry() {
        String jobName = "offerExpiry";
        log.info("job={} action=start", jobName);
        try {
            int processed = allocationExpiryService.expireOffers();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void runCheckInExpiry() {
        String jobName = "checkInExpiry";
        log.info("job={} action=start", jobName);
        try {
            int processed = allocationExpiryService.expireCheckIns();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.allocation.batch-interval-ms:21600000}")
    public void runBatchAllocation() {
        String jobName = "batchAllocation";
        log.info("job={} action=start", jobName);
        try {
            int processed = allocationBatchService.scheduledBatchAllocation();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(cron = "${app.storage.cleanup.cron:0 0 3 * * *}")
    public void runOrphanFileCleanup() {
        String jobName = "orphanCleanup";
        log.info("job={} action=start", jobName);
        try {
            int processed = orphanFileCleanupJob.cleanupOrphanedFiles();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    /**
     * Deletes documents past their retention period (GH-235). Runs after the orphan sweep rather
     * than beside it: this job leaves objects for the after-commit deletion listener to remove, and
     * anything that listener misses is an orphan the next sweep collects.
     */
    @Scheduled(cron = "${app.storage.retention.cron:0 30 3 * * *}")
    public void runDocumentRetention() {
        String jobName = "documentRetention";
        log.info("job={} action=start", jobName);
        try {
            int processed = documentRetentionJob.expireDocumentsPastRetention();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void runOfferReminder() {
        String jobName = "offerReminder";
        log.info("job={} action=start", jobName);
        try {
            int processed = offerReminderService.sendOfferExpiryReminders();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void runCheckInReminder() {
        String jobName = "checkInReminder";
        log.info("job={} action=start", jobName);
        try {
            int processed = checkInReminderService.sendCheckInReminders();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.notification.retry-check-interval-ms:300000}")
    public void runNotificationRetry() {
        String jobName = "notificationRetry";
        log.info("job={} action=start", jobName);
        try {
            notificationRetryService.retryFailedNotifications();
            log.info("job={} action=complete", jobName);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }
}
