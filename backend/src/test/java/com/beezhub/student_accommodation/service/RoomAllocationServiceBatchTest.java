package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.BatchAllocationResult;
import com.beezhub.student_accommodation.service.allocation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    @Test
    void batchAllocate_shouldDelegateToBatchService() {
        // Arrange
        int maxAllocations = 10;
        BatchAllocationResult expectedResult = BatchAllocationResult.builder()
                .successCount(5)
                .failureCount(0)
                .build();

        when(allocationBatchService.batchAllocateFromWaitingList(maxAllocations)).thenReturn(expectedResult);

        // Act
        BatchAllocationResult result = roomAllocationService.batchAllocateFromWaitingList(maxAllocations);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
        verify(allocationBatchService).batchAllocateFromWaitingList(maxAllocations);
    }

    @Test
    void allocateNextFromWaitingList_shouldDelegateToBatchService() {
        // Act
        roomAllocationService.allocateNextFromWaitingList();

        // Assert
        verify(allocationBatchService).allocateNextFromWaitingList();
    }
}
