package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomAllocationServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RoomAllocationMapper roomAllocationMapper;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private RoomAttributesRepository roomAttributesRepository;
    @Mock
    private AllocationScoringService allocationScoringService;
    
    // Decomposed Services
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
        roomAllocationService.confirmCheckIn(1L);
        verify(allocationCheckInService).confirmCheckIn(1L);
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
