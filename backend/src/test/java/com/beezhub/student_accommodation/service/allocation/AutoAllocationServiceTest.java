package com.beezhub.student_accommodation.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GH-207: auto-allocation was gated by two independent switches with no link between them. The
 * batch path read the FF4j {@code auto-allocation} flag; this per-application path read the
 * separate {@code auto_allocation_enabled} system setting. An admin who enabled the flag still saw
 * this path skipped, and neither screen mentioned the other.
 */
@ExtendWith(MockitoExtension.class)
class AutoAllocationServiceTest {

    @Mock private FeatureFlagService featureFlagService;
    @Mock private ApplicationDocumentService applicationDocumentService;
    @Mock private ScoringStrategy allocationScoringService;
    @Mock private AllocationLifecycleService allocationLifecycleService;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserNotificationService userNotificationService;

    @InjectMocks private AutoAllocationService autoAllocationService;

    private Application application;

    @BeforeEach
    void setUp() {
        AppUser appUser = new AppUser();
        appUser.setId(1L);

        Student student = new Student();
        student.setId(1L);
        student.setAppUser(appUser);

        application = new Application();
        application.setId(7L);
        application.setStatus(ApplicationStatus.APPROVED);
        application.setStudent(student);
    }

    @Test
    void flagDisabled_doesNothingAtAll() {
        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        autoAllocationService.attemptAutoAllocation(application);

        // Not even a status change: a disabled feature should leave the application exactly as the
        // approval left it.
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        verifyNoInteractions(applicationDocumentService, allocationScoringService,
                allocationLifecycleService);
        verify(applicationRepository, never()).save(any());
    }

    /**
     * The point of the fix: this path reads the same flag the batch path does, so turning the
     * feature on in one place turns it on everywhere.
     */
    @Test
    void flagEnabled_proceedsPastTheGate() {
        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(applicationDocumentService.areAllRequiredDocumentsApproved(7L)).thenReturn(false);

        autoAllocationService.attemptAutoAllocation(application);

        verify(applicationDocumentService).areAllRequiredDocumentsApproved(7L);
    }

    @Test
    void flagEnabled_documentsNotApproved_movesToAwaitingAllocation() {
        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(applicationDocumentService.areAllRequiredDocumentsApproved(7L)).thenReturn(false);

        autoAllocationService.attemptAutoAllocation(application);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(applicationRepository).save(application);
        verifyNoInteractions(allocationScoringService);
    }

    @Test
    void flagEnabled_noRoomAvailable_queuesTheStudent() {
        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(applicationDocumentService.areAllRequiredDocumentsApproved(7L)).thenReturn(true);
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(List.of());

        autoAllocationService.attemptAutoAllocation(application);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(allocationLifecycleService).enqueueWaitingList(anyLong(), anyLong());
    }

    /** GH-498: an expired required expiring document queues the student; nothing is offered. */
    @Test
    void flagEnabled_documentsNotReadyForAnOffer_queuesTheStudent() {
        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(applicationDocumentService.areAllRequiredDocumentsApproved(7L)).thenReturn(true);
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(List.of(
                com.beezhub.student_accommodation.model.dto.ScoredRoom.builder().roomId(5L).roomNumber("12")
                        .totalScore(java.math.BigDecimal.TEN).build()));
        when(allocationLifecycleService.createRoomAllocation(any())).thenThrow(
                new com.beezhub.student_accommodation.exceptions.DocumentsNotReadyException("expired"));

        autoAllocationService.attemptAutoAllocation(application);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(allocationLifecycleService).enqueueWaitingList(1L, 7L);
    }
}
