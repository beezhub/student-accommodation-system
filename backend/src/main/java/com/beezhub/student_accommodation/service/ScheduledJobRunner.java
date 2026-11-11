package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.service.allocation.AllocationBatchService;
import com.beezhub.student_accommodation.service.allocation.AllocationExpiryService;
import com.beezhub.student_accommodation.service.allocation.OfferReminderService;
import com.beezhub.student_accommodation.service.allocation.CheckInReminderService;
import com.beezhub.student_accommodation.service.allocation.ReconfirmationService;
import com.beezhub.student_accommodation.service.allocation.RoomOccupancyRecountService;
import com.beezhub.student_accommodation.service.application.IntakeExpiryService;
import com.beezhub.student_accommodation.service.document.DocumentExpiryReminderService;
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
    private final ReconfirmationService reconfirmationService;
    private final IntakeExpiryService intakeExpiryService;
    private final DocumentExpiryReminderService documentExpiryReminderService;
    private final RoomOccupancyRecountService roomOccupancyRecountService;
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

    /**
     * In UTC whatever the JVM's zone is, here and on the retention job below, so these two stay
     * clear of the 01:30 UTC database backup and RDS's Sunday 01:00 UTC maintenance window (GH-461).
     */
    @Scheduled(cron = "${app.storage.cleanup.cron:0 0 3 * * *}", zone = "UTC")
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
    @Scheduled(cron = "${app.storage.retention.cron:0 30 3 * * *}", zone = "UTC")
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

    /**
     * "Still coming?" for places accepted long before move-in, with reminders, and release of those
     * not reconfirmed in time (GH-496). Daily at 06:00 UTC, 08:00 in South Africa, so the emails
     * arrive in the morning and the run stays clear of the 01:30 UTC backup. Dates inside the run
     * are read in South African time; the service sets the tenant context per company.
     */
    @Scheduled(cron = "${app.allocation.reconfirm-cron:0 0 6 * * *}", zone = "UTC")
    public void runReconfirmation() {
        String jobName = "reconfirmation";
        log.info("job={} action=start", jobName);
        try {
            int processed = reconfirmationService.runDaily();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    /**
     * Expires unfinished applications the day after their intake's stay ends (GH-497). Daily at
     * 05:00 UTC, 07:00 in South Africa: after midnight there, so an intake that ended yesterday is
     * caught the same morning, before the reconfirmation run, and clear of the 01:30 UTC backup. Dates
     * inside the run are read in South African time; the service sets the tenant context per company.
     */
    @Scheduled(cron = "${app.application.intake-expiry-cron:0 0 5 * * *}", zone = "UTC")
    public void runIntakeExpiry() {
        String jobName = "intakeExpiry";
        log.info("job={} action=start", jobName);
        try {
            int processed = intakeExpiryService.runDaily();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    /**
     * Reminds students to replace expiring documents, before and after expiry (GH-498). Daily at
     * 06:30 UTC, 08:30 in South Africa: after the intake expiry and reconfirmation runs, so the
     * emails arrive in the morning, and clear of the 01:30 UTC backup. Dates inside the run are read
     * in South African time; the service sets the tenant context per company.
     */
    @Scheduled(cron = "${app.document.expiry-reminder-cron:0 30 6 * * *}", zone = "UTC")
    public void runDocumentExpiryReminders() {
        String jobName = "documentExpiryReminder";
        log.info("job={} action=start", jobName);
        try {
            int processed = documentExpiryReminderService.runDaily();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }

    /**
     * Recounts every room's occupancy (GH-541). An accepted place starts counting on its stay start
     * date and stops on its end date, but nothing writes to the room on those dates. Daily at 22:05
     * UTC, 00:05 in South Africa, just after the date the count is taken on has changed, and clear
     * of the 01:30 UTC backup. Processed is the number of rooms that were wrong and got corrected.
     */
    @Scheduled(cron = "${app.allocation.occupancy-recount-cron:0 5 22 * * *}", zone = "UTC")
    public void runOccupancyRecount() {
        String jobName = "occupancyRecount";
        log.info("job={} action=start", jobName);
        try {
            int processed = roomOccupancyRecountService.recountAll();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("sas.jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("sas.jobs." + jobName + ".failures").increment();
        }
    }
}
