package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.BatchAllocationResult;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.*;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for RoomAllocationService batch allocation methods.
 * Tests delegation to AllocationBatchService.
 * Detailed batch allocation logic is tested in AllocationBatchServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceBatchTest {

    @Mock
    private RoomAllocationQueryService roomAllocationQueryService;

    @Mock
    private AllocationEligibilityService allocationEligibilityService;

    @Mock
    private AllocationLifecycleService allocationLifecycleService;

    @Mock
    private AllocationBatchService allocationBatchService;

    @Mock
    private AllocationCheckInService allocationCheckInService;

    @Mock
    private AllocationExpiryService allocationExpiryService;

    @Mock
    private ApplicationDocumentService applicationDocumentService;

    @Mock
    private AllocationEligibilityService allocationEligibilityService;
    @Mock
    private AllocationScoringService allocationScoringService;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private UserNotificationService userNotificationService;

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    @Test
    void batchAllocate_shouldAllocateMultipleStudents_whenRoomsAvailable() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(2)
                .successCount(2)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(10)).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(2);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void batchAllocate_shouldReturnEmptyResult_whenFeatureFlagDisabled() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(0)
                .successCount(0)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(anyInt())).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void batchAllocate_shouldReturnEmptyResult_whenNoWaitingStudents() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(0)
                .successCount(0)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(anyInt())).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void batchAllocate_shouldReturnEmptyResult_whenNoAvailableRooms() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(0)
                .successCount(0)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(anyInt())).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(0);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void batchAllocate_shouldSkipStudentsWithActiveAllocations() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(2)
                .successCount(1)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(anyInt())).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void batchAllocate_shouldRespectMaxAllocationsLimit() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(5)
                .successCount(5)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(5)).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(5);

        assertThat(result.getSuccessCount()).isEqualTo(5);
        verify(allocationBatchService).batchAllocateFromWaitingList(5);
    }

    @Test
    void batchAllocate_shouldRespectRoomCapacity() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(2)
                .successCount(2)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(anyInt())).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void batchAllocate_shouldProcessInFIFOOrder() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(3)
                .successCount(3)
                .failureCount(0)
                .build();
        when(allocationBatchService.batchAllocateFromWaitingList(anyInt())).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(3);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void batchAllocate_shouldHandlePartialFailures() {
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .totalAttempted(2)
                .successCount(1)
                .failureCount(1)
                .build();
        expectedResult.getErrors().add("Error allocating student 2");
        when(allocationBatchService.batchAllocateFromWaitingList(anyInt())).thenReturn(expectedResult);

        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }
}
