package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.service.allocation.AllocationBatchService;
import com.beezhub.student_accommodation.service.allocation.AllocationExpiryService;
import com.beezhub.student_accommodation.service.allocation.CheckInReminderService;
import com.beezhub.student_accommodation.service.document.OrphanFileCleanupJob;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledJobRunnerTest {

    @Mock
    private AllocationExpiryService allocationExpiryService;

    @Mock
    private AllocationBatchService allocationBatchService;

    @Mock
    private OrphanFileCleanupJob orphanFileCleanupJob;

    @Mock
    private CheckInReminderService checkInReminderService;

    private MeterRegistry meterRegistry;
    private ScheduledJobRunner scheduledJobRunner;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduledJobRunner = new ScheduledJobRunner(
                allocationExpiryService,
                allocationBatchService,
                orphanFileCleanupJob,
                checkInReminderService,
                meterRegistry);
    }

    // --- offerExpiry ---

    @Test
    void runOfferExpiry_whenServiceThrows_failureCounterIncremented_noExceptionPropagated() {
        when(allocationExpiryService.expireOffers()).thenThrow(new RuntimeException("DB connection lost"));

        assertThatCode(() -> scheduledJobRunner.runOfferExpiry()).doesNotThrowAnyException();

        Counter failures = meterRegistry.find("sas.jobs.offerExpiry.failures").counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
    }

    @Test
    void runOfferExpiry_whenServiceSucceeds_processedCounterIncremented() {
        when(allocationExpiryService.expireOffers()).thenReturn(3);

        scheduledJobRunner.runOfferExpiry();

        Counter processed = meterRegistry.find("sas.jobs.offerExpiry.processed").counter();
        assertThat(processed).isNotNull();
        assertThat(processed.count()).isEqualTo(3.0);
    }

    // --- checkInExpiry ---

    @Test
    void runCheckInExpiry_whenServiceThrows_failureCounterIncremented_noExceptionPropagated() {
        when(allocationExpiryService.expireCheckIns()).thenThrow(new RuntimeException("timeout"));

        assertThatCode(() -> scheduledJobRunner.runCheckInExpiry()).doesNotThrowAnyException();

        Counter failures = meterRegistry.find("sas.jobs.checkInExpiry.failures").counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
    }

    @Test
    void runCheckInExpiry_whenServiceSucceeds_processedCounterIncremented() {
        when(allocationExpiryService.expireCheckIns()).thenReturn(5);

        scheduledJobRunner.runCheckInExpiry();

        Counter processed = meterRegistry.find("sas.jobs.checkInExpiry.processed").counter();
        assertThat(processed).isNotNull();
        assertThat(processed.count()).isEqualTo(5.0);
    }

    // --- batchAllocation ---

    @Test
    void runBatchAllocation_whenServiceThrows_failureCounterIncremented_noExceptionPropagated() {
        when(allocationBatchService.scheduledBatchAllocation()).thenThrow(new RuntimeException("allocation error"));

        assertThatCode(() -> scheduledJobRunner.runBatchAllocation()).doesNotThrowAnyException();

        Counter failures = meterRegistry.find("sas.jobs.batchAllocation.failures").counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
    }

    @Test
    void runBatchAllocation_whenServiceSucceeds_processedCounterIncremented() {
        when(allocationBatchService.scheduledBatchAllocation()).thenReturn(10);

        scheduledJobRunner.runBatchAllocation();

        Counter processed = meterRegistry.find("sas.jobs.batchAllocation.processed").counter();
        assertThat(processed).isNotNull();
        assertThat(processed.count()).isEqualTo(10.0);
    }

    // --- orphanCleanup ---

    @Test
    void runOrphanFileCleanup_whenServiceThrows_failureCounterIncremented_noExceptionPropagated() {
        when(orphanFileCleanupJob.cleanupOrphanedFiles()).thenThrow(new RuntimeException("MinIO unavailable"));

        assertThatCode(() -> scheduledJobRunner.runOrphanFileCleanup()).doesNotThrowAnyException();

        Counter failures = meterRegistry.find("sas.jobs.orphanCleanup.failures").counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
    }

    @Test
    void runOrphanFileCleanup_whenServiceSucceeds_processedCounterIncremented() {
        when(orphanFileCleanupJob.cleanupOrphanedFiles()).thenReturn(2);

        scheduledJobRunner.runOrphanFileCleanup();

        Counter processed = meterRegistry.find("sas.jobs.orphanCleanup.processed").counter();
        assertThat(processed).isNotNull();
        assertThat(processed.count()).isEqualTo(2.0);
    }

    // --- isolation: one job failure does not affect others ---

    @Test
    void jobFailures_areIsolated_otherJobsStillRun() {
        when(allocationExpiryService.expireOffers()).thenThrow(new RuntimeException("fail"));
        when(allocationExpiryService.expireCheckIns()).thenReturn(2);

        assertThatCode(() -> scheduledJobRunner.runOfferExpiry()).doesNotThrowAnyException();
        assertThatCode(() -> scheduledJobRunner.runCheckInExpiry()).doesNotThrowAnyException();

        assertThat(meterRegistry.find("sas.jobs.offerExpiry.failures").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("sas.jobs.checkInExpiry.processed").counter().count()).isEqualTo(2.0);
    }

    // --- checkInReminder ---

    @Test
    void runCheckInReminder_whenServiceThrows_failureCounterIncremented_noExceptionPropagated() {
        when(checkInReminderService.sendCheckInReminders()).thenThrow(new RuntimeException("SMTP down"));

        assertThatCode(() -> scheduledJobRunner.runCheckInReminder()).doesNotThrowAnyException();

        Counter failures = meterRegistry.find("sas.jobs.checkInReminder.failures").counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
    }

    @Test
    void runCheckInReminder_whenServiceSucceeds_processedCounterIncremented() {
        when(checkInReminderService.sendCheckInReminders()).thenReturn(3);

        scheduledJobRunner.runCheckInReminder();

        Counter processed = meterRegistry.find("sas.jobs.checkInReminder.processed").counter();
        assertThat(processed).isNotNull();
        assertThat(processed.count()).isEqualTo(3.0);
    }
}
