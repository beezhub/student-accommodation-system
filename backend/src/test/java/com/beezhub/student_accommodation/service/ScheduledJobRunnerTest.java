package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.service.allocation.AllocationBatchService;
import com.beezhub.student_accommodation.service.allocation.AllocationExpiryService;
import com.beezhub.student_accommodation.service.allocation.OfferReminderService;
import com.beezhub.student_accommodation.service.allocation.CheckInReminderService;
import com.beezhub.student_accommodation.service.document.DocumentRetentionJob;
import com.beezhub.student_accommodation.service.document.OrphanFileCleanupJob;
import com.beezhub.student_accommodation.service.notification.NotificationRetryService;
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
    private DocumentRetentionJob documentRetentionJob;

    @Mock
    private OfferReminderService offerReminderService;
    @Mock
    private CheckInReminderService checkInReminderService;
    @Mock
    private NotificationRetryService notificationRetryService;
    @Mock
    private com.beezhub.student_accommodation.service.allocation.ReconfirmationService reconfirmationService;
    @Mock
    private com.beezhub.student_accommodation.service.application.IntakeExpiryService intakeExpiryService;
    @Mock
    private com.beezhub.student_accommodation.service.document.DocumentExpiryReminderService documentExpiryReminderService;
    @Mock
    private com.beezhub.student_accommodation.service.allocation.RoomOccupancyRecountService roomOccupancyRecountService;

    private MeterRegistry meterRegistry;
    private ScheduledJobRunner scheduledJobRunner;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduledJobRunner = new ScheduledJobRunner(
                allocationExpiryService,
                allocationBatchService,
                orphanFileCleanupJob,
                documentRetentionJob,
                offerReminderService,
                checkInReminderService,
                notificationRetryService,
                reconfirmationService,
                intakeExpiryService,
                documentExpiryReminderService,
                roomOccupancyRecountService,
                meterRegistry);
    }

    // --- documentRetention (GH-235) ---

    @Test
    void runDocumentRetention_countsWhatTheJobProcessed() {
        when(documentRetentionJob.expireDocumentsPastRetention()).thenReturn(7);

        scheduledJobRunner.runDocumentRetention();

        Counter processed = meterRegistry.find("sas.jobs.documentRetention.processed").counter();
        assertThat(processed).isNotNull();
        assertThat(processed.count()).isEqualTo(7.0);
    }

    /** The job deletes student documents; a failure must not take the scheduler thread down. */
    @Test
    void runDocumentRetention_whenJobThrows_failureCounterIncremented_noExceptionPropagated() {
        when(documentRetentionJob.expireDocumentsPastRetention())
                .thenThrow(new RuntimeException("storage unavailable"));

        assertThatCode(() -> scheduledJobRunner.runDocumentRetention()).doesNotThrowAnyException();

        Counter failures = meterRegistry.find("sas.jobs.documentRetention.failures").counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
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
        when(orphanFileCleanupJob.cleanupOrphanedFiles()).thenThrow(new RuntimeException("Storage unavailable"));

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

    // --- offerReminder ---

    @Test
    void runOfferReminder_whenServiceThrows_failureCounterIncremented_noExceptionPropagated() {
        when(offerReminderService.sendOfferExpiryReminders()).thenThrow(new RuntimeException("SMTP down"));

        assertThatCode(() -> scheduledJobRunner.runOfferReminder()).doesNotThrowAnyException();

        Counter failures = meterRegistry.find("sas.jobs.offerReminder.failures").counter();
        assertThat(failures).isNotNull();
        assertThat(failures.count()).isEqualTo(1.0);
    }

    @Test
    void runOfferReminder_whenServiceSucceeds_processedCounterIncremented() {
        when(offerReminderService.sendOfferExpiryReminders()).thenReturn(4);

        scheduledJobRunner.runOfferReminder();

        Counter processed = meterRegistry.find("sas.jobs.offerReminder.processed").counter();
        assertThat(processed).isNotNull();
        assertThat(processed.count()).isEqualTo(4.0);
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

    // --- reconfirmation (GH-496) ---

    @Test
    void runReconfirmation_countsWhatTheJobProcessed() {
        when(reconfirmationService.runDaily()).thenReturn(4);

        scheduledJobRunner.runReconfirmation();

        assertThat(meterRegistry.find("sas.jobs.reconfirmation.processed").counter().count()).isEqualTo(4.0);
    }

    @Test
    void runReconfirmation_whenServiceThrows_failureCounted_noExceptionPropagated() {
        when(reconfirmationService.runDaily()).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> scheduledJobRunner.runReconfirmation()).doesNotThrowAnyException();

        assertThat(meterRegistry.find("sas.jobs.reconfirmation.failures").counter().count()).isEqualTo(1.0);
    }

    // --- intake expiry (GH-497) ---

    @Test
    void runIntakeExpiry_countsWhatTheJobProcessed() {
        when(intakeExpiryService.runDaily()).thenReturn(3);

        scheduledJobRunner.runIntakeExpiry();

        assertThat(meterRegistry.find("sas.jobs.intakeExpiry.processed").counter().count()).isEqualTo(3.0);
    }

    @Test
    void runIntakeExpiry_whenServiceThrows_failureCounted_noExceptionPropagated() {
        when(intakeExpiryService.runDaily()).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> scheduledJobRunner.runIntakeExpiry()).doesNotThrowAnyException();

        assertThat(meterRegistry.find("sas.jobs.intakeExpiry.failures").counter().count()).isEqualTo(1.0);
    }

    // --- documentExpiryReminder (GH-498) ---

    @Test
    void runDocumentExpiryReminders_countsWhatTheJobProcessed() {
        when(documentExpiryReminderService.runDaily()).thenReturn(4);

        scheduledJobRunner.runDocumentExpiryReminders();

        assertThat(meterRegistry.find("sas.jobs.documentExpiryReminder.processed").counter().count()).isEqualTo(4.0);
    }

    @Test
    void runDocumentExpiryReminders_whenServiceThrows_failureCounted_noExceptionPropagated() {
        when(documentExpiryReminderService.runDaily()).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> scheduledJobRunner.runDocumentExpiryReminders()).doesNotThrowAnyException();

        assertThat(meterRegistry.find("sas.jobs.documentExpiryReminder.failures").counter().count()).isEqualTo(1.0);
    }

    // --- occupancyRecount (GH-541) ---

    @Test
    void runOccupancyRecount_whenServiceSucceeds_processedCounterIncremented() {
        when(roomOccupancyRecountService.recountAll()).thenReturn(4);

        scheduledJobRunner.runOccupancyRecount();

        verify(roomOccupancyRecountService).recountAll();
        assertThat(meterRegistry.find("sas.jobs.occupancyRecount.processed").counter().count()).isEqualTo(4.0);
    }

    @Test
    void runOccupancyRecount_whenServiceThrows_failureCounterIncremented_noExceptionPropagated() {
        when(roomOccupancyRecountService.recountAll()).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> scheduledJobRunner.runOccupancyRecount()).doesNotThrowAnyException();

        assertThat(meterRegistry.find("sas.jobs.occupancyRecount.failures").counter().count()).isEqualTo(1.0);
    }
}
