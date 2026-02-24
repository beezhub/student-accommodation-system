package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.service.allocation.AllocationBatchService;
import com.beezhub.student_accommodation.service.allocation.AllocationExpiryService;
import com.beezhub.student_accommodation.service.document.OrphanFileCleanupJob;
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
 *   <li>Prometheus counter {@code jobs.{name}.processed} incremented by records processed</li>
 *   <li>Prometheus counter {@code jobs.{name}.failures} incremented on uncaught exception</li>
 * </ul>
 *
 * One job failure does NOT affect other jobs — each wrapper has its own try/catch boundary.
 *
 * NOTE: {@code NotificationRetryService.retryFailedNotifications()} is intentionally omitted.
 * Its {@code @Scheduled} annotation is deferred to Phase 3 pending full retry-logic implementation.
 * See the TODO comment in {@link com.beezhub.student_accommodation.service.notification.NotificationRetryService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobRunner {

    private final AllocationExpiryService allocationExpiryService;
    private final AllocationBatchService allocationBatchService;
    private final OrphanFileCleanupJob orphanFileCleanupJob;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void runOfferExpiry() {
        String jobName = "offerExpiry";
        log.info("job={} action=start", jobName);
        try {
            int processed = allocationExpiryService.expireOffers();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void runCheckInExpiry() {
        String jobName = "checkInExpiry";
        log.info("job={} action=start", jobName);
        try {
            int processed = allocationExpiryService.expireCheckIns();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(fixedDelayString = "${app.allocation.batch-interval-ms:21600000}")
    public void runBatchAllocation() {
        String jobName = "batchAllocation";
        log.info("job={} action=start", jobName);
        try {
            int processed = allocationBatchService.scheduledBatchAllocation();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("jobs." + jobName + ".failures").increment();
        }
    }

    @Scheduled(cron = "${app.storage.cleanup.cron:0 0 3 * * *}")
    public void runOrphanFileCleanup() {
        String jobName = "orphanCleanup";
        log.info("job={} action=start", jobName);
        try {
            int processed = orphanFileCleanupJob.cleanupOrphanedFiles();
            log.info("job={} action=complete processed={}", jobName, processed);
            meterRegistry.counter("jobs." + jobName + ".processed").increment(processed);
        } catch (Exception e) {
            log.error("job={} action=failed", jobName, e);
            meterRegistry.counter("jobs." + jobName + ".failures").increment();
        }
    }
}
