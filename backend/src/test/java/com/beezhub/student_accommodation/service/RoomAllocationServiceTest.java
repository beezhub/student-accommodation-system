package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.service.allocation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceTest {

    // Decomposed Services
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

    // ==================== Read Methods Tests ====================

    @Test
    void getAllRoomAllocations_shouldDelegateToQueryService() {
        roomAllocationService.getAllRoomAllocations();
        verify(roomAllocationQueryService).getAllRoomAllocations();
    }

    @Test
    void getAllRoomAllocations_paginated_shouldDelegateToQueryService() {
        Pageable pageable = Pageable.unpaged();
        roomAllocationService.getAllRoomAllocations(pageable);
        verify(roomAllocationQueryService).getAllRoomAllocations(pageable);
    }

    @Test
    void searchRoomAllocations_shouldDelegateToQueryService() {
        Pageable pageable = Pageable.unpaged();
        LocalDateTime now = LocalDateTime.now();
        roomAllocationService.searchRoomAllocations(1L, 2L, 3L, now, now, pageable);
        verify(roomAllocationQueryService).searchRoomAllocations(1L, 2L, 3L, now, now, pageable);
    }

    @Test
    void getRoomAllocationById_shouldDelegateToQueryService() {
        roomAllocationService.getRoomAllocationById(1L);
        verify(roomAllocationQueryService).getRoomAllocationById(1L);
    }

    @Test
    void getActiveAllocationByStudentId_shouldDelegateToQueryService() {
        roomAllocationService.getActiveAllocationByStudentId(1L);
        verify(roomAllocationQueryService).getActiveAllocationByStudentId(1L);
    }

    @Test
    void isRoomAvailable_shouldDelegateToQueryService() {
        roomAllocationService.isRoomAvailable(1L);
        verify(roomAllocationQueryService).isRoomAvailable(1L);
    }

    @Test
    void getRoomAvailableCapacity_shouldDelegateToQueryService() {
        roomAllocationService.getRoomAvailableCapacity(1L);
        verify(roomAllocationQueryService).getRoomAvailableCapacity(1L);
    }

    @Test
    void findAvailableRoom_shouldDelegateToQueryService() {
        roomAllocationService.findAvailableRoom();
        verify(roomAllocationQueryService).findAvailableRoom();
    }

    @Test
    void getAllocationsByStatus_shouldDelegateToQueryService() {
        Pageable pageable = Pageable.unpaged();
        roomAllocationService.getAllocationsByStatus("ACTIVE", pageable);
        verify(roomAllocationQueryService).getAllocationsByStatus("ACTIVE", pageable);
    }

    @Test
    void getRoomAvailability_shouldDelegateToQueryService() {
        roomAllocationService.getRoomAvailability(1L);
        verify(roomAllocationQueryService).getRoomAvailability(1L);
    }

    // ==================== Eligibility Methods Tests ====================

    @Test
    void checkStudentEligibility_shouldDelegateToEligibilityService() {
        roomAllocationService.checkStudentEligibility(1L);
        verify(allocationEligibilityService).checkStudentEligibility(1L);
    }

    @Test
    void getEligibleRoomsForStudent_shouldDelegateToEligibilityService() {
        roomAllocationService.getEligibleRoomsForStudent(1L, 2L);
        verify(allocationEligibilityService).getEligibleRoomsForStudent(1L, 2L);
    }

    // ==================== Lifecycle Methods Tests ====================

    @Test
    void createRoomAllocation_shouldDelegateToLifecycleService() {
        RoomAllocationRequest request = new RoomAllocationRequest();
        when(allocationLifecycleService.createRoomAllocation(request)).thenReturn(new RoomAllocationResponse());

        roomAllocationService.createRoomAllocation(request);

        verify(allocationLifecycleService).createRoomAllocation(request);
    }

    @Test
    void acceptOffer_shouldDelegateToLifecycleService() {
        AcceptOfferRequest request = new AcceptOfferRequest();
        when(allocationLifecycleService.acceptOffer(1L, request)).thenReturn(new RoomAllocationResponse());

        roomAllocationService.acceptOffer(1L, request);

        verify(allocationLifecycleService).acceptOffer(1L, request);
    }

    @Test
    void declineOffer_shouldDelegateToLifecycleService() {
        DeclineOfferRequest request = new DeclineOfferRequest();
        when(allocationLifecycleService.declineOffer(1L, request)).thenReturn(new RoomAllocationResponse());

        roomAllocationService.declineOffer(1L, request);

        verify(allocationLifecycleService).declineOffer(1L, request);
    }

    @Test
    void deleteRoomAllocation_shouldDelegateToLifecycleService() {
        roomAllocationService.deleteRoomAllocation(1L);
        verify(allocationLifecycleService).deleteRoomAllocation(1L);
    }

    @Test
    void moveStudent_shouldDelegateToLifecycleService() {
        roomAllocationService.moveStudent(1L, 2L, "Reason", 3L);
        verify(allocationLifecycleService).moveStudent(1L, 2L, "Reason", 3L);
    }

    @Test
    void updateRoomAllocation_shouldDelegateToLifecycleService() {
        RoomAllocationRequest request = new RoomAllocationRequest();
        roomAllocationService.updateRoomAllocation(1L, request);
        verify(allocationLifecycleService).updateRoomAllocation(1L, request);
    }

    @Test
    void confirmCheckIn_shouldDelegateToCheckInService() {
        roomAllocationService.confirmCheckIn(1L, false);
        verify(allocationCheckInService).confirmCheckIn(1L, false);
    }

    @Test
    void confirmCheckOut_shouldDelegateToCheckInService() {
        roomAllocationService.confirmCheckOut(1L);
        verify(allocationCheckInService).confirmCheckOut(1L);
    }

    @Test
    void endAllocation_shouldDelegateToCheckInService() {
        roomAllocationService.endAllocation(1L);
        verify(allocationCheckInService).endAllocation(1L);
    }

    @Test
    void allocateNextFromWaitingList_shouldDelegateToBatchService() {
        roomAllocationService.allocateNextFromWaitingList();
        verify(allocationBatchService).allocateNextFromWaitingList();
    }

    @Test
    void batchAllocateFromWaitingList_shouldDelegateToBatchService() {
        roomAllocationService.batchAllocateFromWaitingList(10);
        verify(allocationBatchService).batchAllocateFromWaitingList(10);
    }

    @Test
    void enqueueWaitingList_shouldDelegateToLifecycleService() {
        roomAllocationService.enqueueWaitingList(1L, 2L);
        verify(allocationLifecycleService).enqueueWaitingList(1L, 2L);
    }

    @Test
    void proposeAllocation_shouldDelegateToBatchService() {
        BatchAllocationRequest request = new BatchAllocationRequest();
        roomAllocationService.proposeAllocation(request, 1L);
        verify(allocationBatchService).proposeAllocation(request, 1L);
    }

    @Test
    void confirmAllocation_shouldDelegateToBatchService() {
        roomAllocationService.confirmAllocation(1L, 2L);
        verify(allocationBatchService).confirmAllocation(1L, 2L);
    }

    @Test
    void rejectAllocation_shouldDelegateToBatchService() {
        roomAllocationService.rejectAllocation(1L, "Reason", 2L);
        verify(allocationBatchService).rejectAllocation(1L, "Reason", 2L);
    }

    @Test
    void confirmBatchAllocations_shouldDelegateToBatchService() {
        roomAllocationService.confirmBatchAllocations("batch-1", 1L);
        verify(allocationBatchService).confirmBatchAllocations("batch-1", 1L);
    }

    @Test
    void expireOffers_shouldDelegateToExpiryService() {
        roomAllocationService.expireOffers();
        verify(allocationExpiryService).expireOffers();
    }

    @Test
    void expireCheckIns_shouldDelegateToExpiryService() {
        roomAllocationService.expireCheckIns();
        verify(allocationExpiryService).expireCheckIns();
    }
}
