package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.*;
import com.beezhub.student_accommodation.specification.RoomAllocationSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for managing room allocations.
 * Acts as a facade delegating to specialized services:
 * - AllocationLifecycleService: create, accept, decline, move, update
 * - AllocationBatchService: batch allocation, proposals, scoring
 * - AllocationCheckInService: check-in, check-out, termination
 * - AllocationExpiryService: scheduled expiry jobs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAllocationService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ApplicationRepository applicationRepository;
    private final BuildingRepository buildingRepository;
    private final RoomAttributesRepository roomAttributesRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final AllocationScoringService allocationScoringService;
    
    // Decomposed Services
    private final AllocationLifecycleService allocationLifecycleService;
    private final AllocationBatchService allocationBatchService;
    private final AllocationCheckInService allocationCheckInService;
    private final AllocationExpiryService allocationExpiryService;

    // ==================== Read Methods (Retained) ====================

    public List<RoomAllocationResponse> getAllRoomAllocations() {
        log.info("Fetching all room allocations");
        List<RoomAllocation> allocations = roomAllocationRepository.findAllWithDetails();
        List<RoomAllocationResponse> allocationResponses = roomAllocationMapper.toResponseList(allocations);
        log.info("Found {} room allocations", allocationResponses.size());
        return allocationResponses;
    }

    public PageResponse<RoomAllocationResponse> getAllRoomAllocations(Pageable pageable) {
        log.info("Fetching paginated room allocations");
        Page<RoomAllocation> allocations = roomAllocationRepository.findAll(pageable);
        Page<RoomAllocationResponse> responsePage = allocations.map(roomAllocationMapper::toResponse);
        log.info("Found {} room allocations in page", allocations.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<RoomAllocationResponse> searchRoomAllocations(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            Pageable pageable) {
        return searchRoomAllocations(studentId, roomId, buildingId, fromDate, toDate, null, null, null, pageable);
    }

    public PageResponse<RoomAllocationResponse> searchRoomAllocations(Long studentId, Long roomId, Long buildingId,
            LocalDateTime fromDate, LocalDateTime toDate,
            String studentName, String roomNumber, String status,
            Pageable pageable) {
        log.info(
                "Searching room allocations with filters - studentId: {}, roomId: {}, buildingId: {}, fromDate: {}, toDate: {}, studentName: {}, roomNumber: {}, status: {}",
                studentId, roomId, buildingId, fromDate, toDate, studentName, roomNumber, status);
        Specification<RoomAllocation> spec = RoomAllocationSpecification.filterBy(studentId, roomId, buildingId,
                fromDate, toDate, studentName, roomNumber, status);
        Page<RoomAllocation> allocations = roomAllocationRepository.findAll(spec, pageable);
        Page<RoomAllocationResponse> responsePage = allocations.map(roomAllocationMapper::toResponse);
        log.info("Found {} room allocations matching criteria", allocations.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public RoomAllocationResponse getRoomAllocationById(Long id) {
        log.info("Fetching room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));
        return roomAllocationMapper.toResponse(allocation);
    }

    public RoomAllocationResponse getActiveAllocationByStudentId(Long studentId) {
        log.info("Fetching active allocation for student: {}", studentId);
        RoomAllocation allocation = roomAllocationRepository.findActiveAllocationByStudentId(studentId)
                .orElseThrow(() -> new RoomAllocationNotFoundException(
                        "No active allocation found for student with id: " + studentId));
        return roomAllocationMapper.toResponse(allocation);
    }

    public boolean isRoomAvailable(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        boolean available = activeAllocations < room.getCapacity();

        log.info("Room {} availability check: {} (Current: {}, Capacity: {})",
                room.getRoomNumber(), available, activeAllocations, room.getCapacity());

        return available;
    }

    public int getRoomAvailableCapacity(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        int available = room.getCapacity() - (int) activeAllocations;

        log.info("Room {} has {} available spots (Capacity: {}, Occupied: {})",
                room.getRoomNumber(), available, room.getCapacity(), activeAllocations);

        return Math.max(0, available);
    }

    public Room findAvailableRoom() {
        Optional<Room> room = roomRepository.findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc("AVAILABLE");
        room.ifPresent(r -> log.info("Found available room {}", r.getRoomNumber()));
        return room.orElse(null);
    }

    public AllocationEligibilityResponse checkStudentEligibility(Long studentId) {
        log.info("Checking allocation eligibility for student: {}", studentId);

        studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        AllocationEligibilityResponse response = new AllocationEligibilityResponse();
        response.setStudentId(studentId);
        response.setEligible(true);

        boolean hasActiveAllocation = roomAllocationRepository.existsActiveAllocationByStudentId(studentId);
        response.setHasActiveAllocation(hasActiveAllocation);

        if (hasActiveAllocation) {
            response.setEligible(false);
            response.setReason("Student already has an active room allocation");
            log.info("Student {} is not eligible: already has active allocation", studentId);
            return response;
        }

        boolean hasApprovedApplication = applicationRepository.existsByStudent_IdAndStatus(
                studentId,
                com.beezhub.student_accommodation.model.enums.ApplicationStatus.APPROVED);
        response.setHasApprovedApplication(hasApprovedApplication);

        response.setReason("Student is eligible for room allocation");
        log.info("Student {} is eligible for room allocation", studentId);
        return response;
    }

    public List<EligibleRoomResponse> getEligibleRoomsForStudent(Long studentId, Long buildingId) {
        log.info("Getting eligible rooms for student: {}, building: {}", studentId, buildingId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        Building building = null;
        if (buildingId != null) {
            building = buildingRepository.findById(buildingId).orElse(null);
        }

        List<Room> eligibleRooms = allocationScoringService.getEligibleRoomsForStudent(student, building);

        List<EligibleRoomResponse> responses = eligibleRooms.stream()
                .map(room -> mapToEligibleRoomResponse(room))
                .collect(java.util.stream.Collectors.toList());

        log.info("Found {} eligible rooms for student {}", responses.size(), studentId);
        return responses;
    }

    private EligibleRoomResponse mapToEligibleRoomResponse(Room room) {
        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
        int availableSpots = room.getCapacity() - (int) activeAllocations;

        // Get room attributes via repository lookup
        RoomAttributes roomAttrs = roomAttributesRepository.findByRoom_Id(room.getId()).orElse(null);

        return EligibleRoomResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .capacity(room.getCapacity())
                .currentOccupancy((int) activeAllocations)
                .availableSpots(Math.max(0, availableSpots))
                .status(room.getStatus())
                .buildingId(room.getBuilding() != null ? room.getBuilding().getId() : null)
                .buildingName(room.getBuilding() != null ? room.getBuilding().getName() : null)
                .buildingAddress(room.getBuilding() != null ? room.getBuilding().getAddress() : null)
                .genderRestriction(roomAttrs != null && roomAttrs.getGenderRestriction() != null
                        ? roomAttrs.getGenderRestriction().name()
                        : null)
                .accessibilityCompliant(roomAttrs != null ? roomAttrs.getAccessibilityCompliant() : null)
                .minYearOfStudy(roomAttrs != null ? roomAttrs.getMinYearOfStudy() : null)
                .maxYearOfStudy(roomAttrs != null ? roomAttrs.getMaxYearOfStudy() : null)
                .build();
    }

    public PageResponse<RoomAllocationResponse> getAllocationsByStatus(String status, Pageable pageable) {
        log.info("Fetching allocations with status: {}", status);

        Specification<RoomAllocation> spec = (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"),
                status);

        Page<RoomAllocation> allocations = roomAllocationRepository.findAll(spec, pageable);
        Page<RoomAllocationResponse> responsePage = allocations.map(roomAllocationMapper::toResponse);

        log.info("Found {} allocations with status: {}", allocations.getNumberOfElements(), status);
        return PageMapper.toPageResponse(responsePage);
    }

    public RoomAvailabilityResponse getRoomAvailability(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        int availableSpots = room.getCapacity() - (int) activeAllocations;

        RoomAvailabilityResponse response = new RoomAvailabilityResponse();
        response.setRoomId(room.getId());
        response.setRoomNumber(room.getRoomNumber());
        response.setBuildingName(room.getBuilding() != null ? room.getBuilding().getName() : "");
        response.setCapacity(room.getCapacity());
        response.setCurrentOccupancy((int) activeAllocations);
        response.setAvailableSpots(Math.max(0, availableSpots));
        response.setAvailable(availableSpots > 0);
        response.setStatus(room.getStatus());

        log.info("Room {} availability: {} spots available (Capacity: {}, Occupied: {})",
                room.getRoomNumber(), response.getAvailableSpots(), room.getCapacity(), activeAllocations);

        return response;
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
        log.info("Allocating room {} for application: {}", roomId, applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with id: " + applicationId));

        if (!"APPROVED".equalsIgnoreCase(String.valueOf(application.getStatus()))) {
            throw new ApplicationNotApprovedException(
                    "Application must be APPROVED before allocating a room. Current status: "
                            + application.getStatus());
        }

        if (roomAllocationRepository.existsByApplicationId(applicationId)) {
            throw new ApplicationAlreadyAllocatedException(
                    "Application with ID " + applicationId + " has already been allocated a room");
        }

        RoomAllocationRequest request = new RoomAllocationRequest();
        request.setStudentId(application.getStudent().getId());
        request.setRoomId(roomId);
        request.setApplicationId(applicationId);

        return allocationLifecycleService.createRoomAllocation(request);
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
