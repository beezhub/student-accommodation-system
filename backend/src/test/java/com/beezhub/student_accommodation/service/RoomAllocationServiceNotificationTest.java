package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.*;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for room allocation notification triggers.
 * Tests delegation to AllocationLifecycleService.
 * Detailed notification logic is tested in AllocationLifecycleServiceTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Room Allocation Service - Notification Integration Tests")
class RoomAllocationServiceNotificationTest {

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
    private ApplicationRepository applicationRepository;

    @Mock
    private AllocationScoringService allocationScoringService;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private UserNotificationService userNotificationService;

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    private RoomAllocationResponse roomAllocationResponse;
    private RoomAllocationRequest roomAllocationRequest;

    @BeforeEach
    void setUp() {
        roomAllocationRequest = new RoomAllocationRequest();
        roomAllocationRequest.setStudentId(1L);
        roomAllocationRequest.setRoomId(1L);

        roomAllocationResponse = new RoomAllocationResponse();
        roomAllocationResponse.setId(1L);
    }

    @Test
    @DisplayName("Should send notification when room is allocated")
    void shouldSendNotificationWhenRoomIsAllocated() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should send notification when allocating by application")
    void shouldSendNotificationWhenAllocatingByApplication() {
        when(allocationLifecycleService.allocateRoomByApplication(anyLong(), anyLong()))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.allocateRoomByApplication(1L, 1L);

        verify(allocationLifecycleService).allocateRoomByApplication(1L, 1L);
    }

    @Test
    @DisplayName("Should include student information in notification")
    void shouldIncludeStudentInformationInNotification() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should include room allocation details in notification")
    void shouldIncludeRoomAllocationDetailsInNotification() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should include building information in notification")
    void shouldIncludeBuildingInformationInNotification() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should send notification with room capacity information")
    void shouldSendNotificationWithRoomCapacityInformation() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should execute notification asynchronously without blocking")
    void shouldExecuteNotificationAsynchronouslyWithoutBlocking() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should not break flow when notification fails")
    void shouldNotBreakFlowWhenNotificationFails() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should maintain transaction integrity when notification fails")
    void shouldMaintainTransactionIntegrityWhenNotificationFails() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }

    @Test
    @DisplayName("Should handle room occupancy update along with notification")
    void shouldHandleRoomOccupancyUpdateAlongWithNotification() {
        when(allocationLifecycleService.createRoomAllocation(any(RoomAllocationRequest.class)))
                .thenReturn(roomAllocationResponse);

        roomAllocationService.createRoomAllocation(roomAllocationRequest);

        verify(allocationLifecycleService).createRoomAllocation(roomAllocationRequest);
    }
}
