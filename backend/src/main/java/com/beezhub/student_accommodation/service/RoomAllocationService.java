package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.service.allocation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing room allocations.
 * Acts as a facade delegating to specialized services:
 * - RoomAllocationQueryService: read operations
 * - AllocationEligibilityService: eligibility checks
 * - AllocationLifecycleService: create, accept, decline, move, update
 * - AllocationBatchService: batch allocation, proposals, scoring
 * - AllocationCheckInService: check-in, check-out, termination
 * - AllocationExpiryService: scheduled expiry jobs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAllocationService {

    // Decomposed Services
    private final RoomAllocationQueryService roomAllocationQueryService;
    private final AllocationEligibilityService allocationEligibilityService;
    private final AllocationLifecycleService allocationLifecycleService;
    private final AllocationBatchService allocationBatchService;
    private final AllocationCheckInService allocationCheckInService;
    private final AllocationExpiryService allocationExpiryService;

    // ==================== Read Methods (Delegated) ====================

    public List<RoomAllocationResponse> getAllRoomAllocations() {
        return roomAllocationQueryService.getAllRoomAllocations();
    }

    public PageResponse<RoomAllocationResponse> getAllRoomAllocations(Pageable pageable) {
        return roomAllocationQueryService.getAllRoomAllocations(pageable);
    }

    public PageResponse<RoomAllocationResponse> searchRoomAllocations(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            Pageable pageable) {
        return roomAllocationQueryService.searchRoomAllocations(studentId, roomId, buildingId, fromDate, toDate, pageable);
    }

    public PageResponse<RoomAllocationResponse> searchRoomAllocations(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            String studentName, String roomNumber, String status,
            Pageable pageable) {
        return roomAllocationQueryService.searchRoomAllocations(studentId, roomId, buildingId, fromDate, toDate, studentName, roomNumber, status, pageable);
    }

    public RoomAllocationResponse getRoomAllocationById(Long id) {
        return roomAllocationQueryService.getRoomAllocationById(id);
    }

    public RoomAllocationResponse getActiveAllocationByStudentId(Long studentId) {
        return roomAllocationQueryService.getActiveAllocationByStudentId(studentId);
    }

    public boolean isRoomAvailable(Long roomId) {
        return roomAllocationQueryService.isRoomAvailable(roomId);
    }

    public int getRoomAvailableCapacity(Long roomId) {
        return roomAllocationQueryService.getRoomAvailableCapacity(roomId);
    }

    public Room findAvailableRoom() {
        return roomAllocationQueryService.findAvailableRoom();
    }

    public PageResponse<RoomAllocationResponse> getAllocationsByStatus(String status, Pageable pageable) {
        return roomAllocationQueryService.getAllocationsByStatus(status, pageable);
    }

    public RoomAvailabilityResponse getRoomAvailability(Long roomId) {
        return roomAllocationQueryService.getRoomAvailability(roomId);
    }

    // ==================== Eligibility Methods (Delegated) ====================

    public AllocationEligibilityResponse checkStudentEligibility(Long studentId) {
        return allocationEligibilityService.checkStudentEligibility(studentId);
    }

    public List<EligibleRoomResponse> getEligibleRoomsForStudent(Long studentId, Long buildingId) {
        return allocationEligibilityService.getEligibleRoomsForStudent(studentId, buildingId);
    }

    // ==================== Lifecycle Methods (Delegated) ====================

    @Transactional
    public RoomAllocationResponse createRoomAllocation(RoomAllocationRequest request) {
        return allocationLifecycleService.createRoomAllocation(request);
    }

    @Transactional
    public RoomAllocationResponse acceptOffer(Long id, AcceptOfferRequest request) {
        return allocationLifecycleService.acceptOffer(id, request);
    }

    @Transactional
    public RoomAllocationResponse declineOffer(Long id, DeclineOfferRequest request) {
        return allocationLifecycleService.declineOffer(id, request);
    }

    @Transactional
    public RoomAllocationResponse updateRoomAllocation(Long id, RoomAllocationRequest request) {
        return allocationLifecycleService.updateRoomAllocation(id, request);
    }

    @Transactional
    public void deleteRoomAllocation(Long id) {
        allocationLifecycleService.deleteRoomAllocation(id);
    }

    @Transactional
    public RoomAllocationResponse allocateRoomByApplication(Long applicationId, Long roomId) {
        return allocationLifecycleService.allocateRoomByApplication(applicationId, roomId);
    }

    @Transactional
    public RoomAllocationResponse moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId) {
        return allocationLifecycleService.moveStudent(allocationId, newRoomId, reason, adminId);
    }

    // ==================== Check-In Methods (Delegated) ====================

    @Transactional
    public RoomAllocationResponse confirmCheckIn(Long id) {
        return allocationCheckInService.confirmCheckIn(id);
    }

    @Transactional
    public RoomAllocationResponse confirmCheckOut(Long id) {
        return allocationCheckInService.confirmCheckOut(id);
    }

    @Transactional
    public RoomAllocationResponse endAllocation(Long id) {
        return allocationCheckInService.endAllocation(id);
    }

    // ==================== Batch Allocation Methods (Delegated) ====================

    @Transactional
    public void allocateNextFromWaitingList() {
        allocationBatchService.allocateNextFromWaitingList();
    }

    @Transactional
    public BatchAllocationResult batchAllocateFromWaitingList(int maxAllocations) {
        return allocationBatchService.batchAllocateFromWaitingList(maxAllocations);
    }

    @Transactional
    public MessageResponse enqueueWaitingList(Long studentId, Long applicationId) {
        return allocationLifecycleService.enqueueWaitingList(studentId, applicationId);
    }

    @Transactional
    public BatchAllocationProposalResponse proposeAllocation(BatchAllocationRequest request, Long adminId) {
        return allocationBatchService.proposeAllocation(request, adminId);
    }

    @Transactional
    public RoomAllocationResponse confirmAllocation(Long stagedAllocationId, Long adminId) {
        return allocationBatchService.confirmAllocation(stagedAllocationId, adminId);
    }

    @Transactional
    public void rejectAllocation(Long stagedAllocationId, String reason, Long adminId) {
        allocationBatchService.rejectAllocation(stagedAllocationId, reason, adminId);
    }

    @Transactional
    public void confirmBatchAllocations(String batchId, Long adminId) {
        allocationBatchService.confirmBatchAllocations(batchId, adminId);
    }

    @Transactional
    public StagedAllocationResponse overrideConstraints(Long studentId, Long roomId, String reason,
            Long adminId, List<String> constraintsBypassed) {
        return allocationBatchService.overrideConstraints(studentId, roomId, reason, adminId, constraintsBypassed);
    }

    public PageResponse<StagedAllocationResponse> getStagedAllocations(String batchId, StagedAllocationStatus status,
            Pageable pageable) {
        return allocationBatchService.getStagedAllocations(batchId, status, pageable);
    }

    @Transactional
    public BatchConfirmationResult confirmBatchAllocationsWithResult(String batchId, Long adminId) {
        return allocationBatchService.confirmBatchAllocationsWithResult(batchId, adminId);
    }

    public List<WaitingListEntryResponse> getWaitingListWithScores(Long buildingId) {
        return allocationBatchService.getWaitingListWithScores(buildingId);
    }

    public RescoreResult rescoreWaitingList() {
        return allocationBatchService.rescoreWaitingList();
    }

    // ==================== Expiry Methods (Delegated) ====================

    public void expireOffers() {
        allocationExpiryService.expireOffers();
    }

    public void expireCheckIns() {
        allocationExpiryService.expireCheckIns();
    }
}
