package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.service.allocation.*;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException;
import com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException;
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
    private final ApplicationDocumentService applicationDocumentService;
    private final ApplicationRepository applicationRepository;
    private final AllocationScoringService allocationScoringService;
    private final SystemSettingService systemSettingService;
    private final UserNotificationService userNotificationService;

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
        return roomAllocationQueryService.searchRoomAllocations(studentId, roomId, buildingId, fromDate, toDate,
                pageable);
    }

    public PageResponse<RoomAllocationResponse> searchRoomAllocations(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            String studentName, String roomNumber, String status,
            Pageable pageable) {
        return roomAllocationQueryService.searchRoomAllocations(studentId, roomId, buildingId, fromDate, toDate,
                studentName, roomNumber, status, pageable);
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

    // ==================== Batch Allocation Methods (Delegated)
    // ====================

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

    // ==================== Auto-Allocation ====================

    /**
     * Attempts to automatically allocate a room for an approved application.
     *
     * This method is triggered when an application status changes to APPROVED
     * and the auto-allocation feature flag is enabled.
     *
     * Flow:
     * 1. Check if auto-allocation feature is enabled
     * 2. Validate all required documents are approved
     * 3. Score available rooms using AllocationScoringService
     * 4. Create room allocation for the best-matching room
     * 5. Fall back to AWAITING_ALLOCATION status if no rooms available
     *
     * If auto-allocation fails for any reason, the application can be
     * manually allocated by an admin.
     *
     * @param application The approved application to allocate
     */
    @Transactional
    public void attemptAutoAllocation(Application application) {
        log.info("Attempting auto-allocation for application {}", application.getId());

        // Check if auto-allocation is enabled
        if (!systemSettingService.isAutoAllocationEnabled()) {
            log.info("Auto-allocation feature is disabled, skipping auto-allocation for application {}",
                    application.getId());
            return;
        }

        // Check if all required documents are approved
        if (!applicationDocumentService.areAllRequiredDocumentsApproved(application.getId())) {
            log.info("Documents not yet approved for application {}, setting status to AWAITING_ALLOCATION",
                    application.getId());
            application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
            applicationRepository.save(application);
            return;
        }

        // Get student and preferred building
        Student student = application.getStudent();
        Building preferredBuilding = application.getPreferredBuilding();

        // Score available rooms
        List<ScoredRoom> scoredRooms = allocationScoringService
                .scoreAvailableRooms(student, application, preferredBuilding);

        // Check if any rooms are available
        if (scoredRooms.isEmpty()) {
            log.info("No rooms available for application {}, adding to waiting list", application.getId());

            // Set status to AWAITING_ALLOCATION
            application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
            applicationRepository.save(application);

            // Add to waiting list
            try {
                allocationLifecycleService.enqueueWaitingList(student.getId(), application.getId());
                log.info("Added student {} to waiting list for application {}", student.getId(),
                        application.getId());

                // Send waiting list notification
                Long studentUserId = student.getAppUser().getId();
                userNotificationService.createWaitingListNotification(studentUserId,
                        application.getId());
            } catch (Exception e) {
                log.error("Failed to add student {} to waiting list for application {}",
                        student.getId(),
                        application.getId(), e);
            }
            return;
        }

        // Get the highest scored room (first in list)
        ScoredRoom bestRoom = scoredRooms.get(0);
        log.info("Best match for application {}: Room {} with score {}", application.getId(),
                bestRoom.getRoomNumber(), bestRoom.getTotalScore());

        // Create room allocation request
        RoomAllocationRequest allocationRequest = new RoomAllocationRequest();
        allocationRequest.setStudentId(student.getId());
        allocationRequest.setRoomId(bestRoom.getRoomId());
        allocationRequest.setApplicationId(application.getId());

        try {
            // Create the allocation
            allocationLifecycleService.createRoomAllocation(allocationRequest);

            // Set allocation method to AUTO
            application.setAllocationMethod(AllocationMethod.AUTO);
            applicationRepository.save(application);

            log.info("Successfully auto-allocated room {} to student {} for application {} with score {}",
                    bestRoom.getRoomNumber(), student.getId(), application.getId(),
                    bestRoom.getTotalScore());

        } catch (RoomCapacityExceededException | StudentAlreadyAllocatedException e) {
            // Race condition - room became full or student already has allocation
            log.warn(
                    "Auto-allocation failed for application {} due to: {}. Adding to waiting list instead.",
                    application.getId(), e.getMessage());

            // Move to AWAITING_ALLOCATION and add to waiting list
            application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
            applicationRepository.save(application);

            try {
                allocationLifecycleService.enqueueWaitingList(student.getId(), application.getId());
                Long studentUserId = student.getAppUser().getId();
                userNotificationService.createWaitingListNotification(studentUserId,
                        application.getId());
            } catch (Exception waitingListError) {
                log.error("Failed to add student {} to waiting list after allocation failure",
                        student.getId(), waitingListError);
            }

        } catch (Exception e) {
            // Unexpected error - log and set to AWAITING_ALLOCATION for manual handling
            log.error("Unexpected error during auto-allocation for application {}, will require manual allocation",
                    application.getId(), e);

            application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
            applicationRepository.save(application);
        }
    }
}
