package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.specification.RoomAllocationSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAllocationService {

    private static final int MAX_DECLINE_COUNT = 2;

    @Value("${app.allocation.offer-expiry-days:7}")
    private int offerExpiryDays;

    @Value("${app.allocation.check-in-expiry-days:7}")
    private int checkInExpiryDays;

    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final NotificationService notificationService;
    private final WaitingListRepository waitingListRepository;
    private final StagedAllocationRepository stagedAllocationRepository;
    private final AllocationAuditLogRepository allocationAuditLogRepository;
    private final AllocationScoringService allocationScoringService;
    private final AppUserRepository appUserRepository;
    private final BuildingRepository buildingRepository;
    private final RoomAttributesRepository roomAttributesRepository;
    private final UserNotificationService userNotificationService;
    private final ObjectMapper objectMapper;
    private final org.ff4j.FF4j ff4j;
    private final TransactionTemplate transactionTemplate;
    private final Environment environment;

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

    /**
     * Creates a new room allocation with comprehensive business rule validation.
     */
    @Transactional
    public RoomAllocationResponse createRoomAllocation(RoomAllocationRequest request) {
        log.info("Creating new room allocation for student: {} to room: {}", request.getStudentId(),
                request.getRoomId());

        // Validate student exists
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(
                        () -> new StudentNotFoundException("Student not found with id: " + request.getStudentId()));

        // Validate room exists
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + request.getRoomId()));

        // Student cannot have multiple active allocations
        if (roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())) {
            log.error("Student {} already has an active room allocation", student.getId());
            throw new StudentAlreadyAllocatedException(
                    "Student with ID " + student.getId() + " already has an active room allocation");
        }

        // Room must have available capacity
        validateRoomCapacity(room);

        // If application ID is provided, validate application
        Application application = null;
        if (request.getApplicationId() != null) {
            application = validateAndGetApplication(request.getApplicationId(), student.getId());
        }

        // Create allocation
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);
        allocation.setAllocationDate(
                request.getAllocationDate() != null ? request.getAllocationDate() : LocalDateTime.now());
        allocation.setEndDate(request.getEndDate());
        // Initial status for new allocation
        validateStatusTransition(allocation, AllocationStatus.OFFERED);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setOfferExpiryDate(LocalDateTime.now().plusDays(offerExpiryDays));

        // Save allocation
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        // Update waiting list entry if student was waiting
        waitingListRepository.findByStudent_IdAndStatus(student.getId(), "WAITING")
            .ifPresent(entry -> {
                entry.setStatus("ALLOCATED");
                waitingListRepository.save(entry);
                log.info("Updated waiting list entry for student {} to ALLOCATED", student.getId());
            });

        // Update room occupancy and status
        updateRoomOccupancy(room, 1);

        log.info("Successfully created room allocation with id: {}", saved.getId());

        // Send notification email to student
        try {
            log.info("Triggering room allocation notification for student {} to room {}", student.getId(),
                    room.getRoomNumber());
            notificationService.sendRoomAllocationNotification(saved);
        } catch (Exception e) {
            log.error(
                    "Failed to trigger notification for room allocation {} - notification will be retried by scheduler",
                    saved.getId(), e);
        }

        return roomAllocationMapper.toResponse(saved);
    }

    private Application validateAndGetApplication(Long applicationId, Long studentId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with id: " + applicationId));

        if (!application.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("Application does not belong to the specified student");
        }

        if (!"APPROVED".equalsIgnoreCase(String.valueOf(application.getStatus()))) {
            log.error("Application {} is not approved. Current status: {}", applicationId, application.getStatus());
            throw new ApplicationNotApprovedException(
                    "Application must be APPROVED before allocating a room. Current status: "
                            + application.getStatus());
        }

        if (roomAllocationRepository.existsByApplicationId(applicationId)) {
            log.error("Application {} has already been allocated a room", applicationId);
            throw new ApplicationAlreadyAllocatedException(
                    "Application with ID " + applicationId + " has already been allocated a room");
        }

        return application;
    }

    private void validateRoomCapacity(Room room) {
        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());

        if (activeAllocations >= room.getCapacity()) {
            log.error("Room {} is at full capacity. Current: {}, Max: {}",
                    room.getRoomNumber(), activeAllocations, room.getCapacity());
            throw new RoomCapacityExceededException(
                    "Room " + room.getRoomNumber() + " is at full capacity (" + room.getCapacity() + " occupants)");
        }

        log.info("Room {} has available capacity. Current: {}, Max: {}",
                room.getRoomNumber(), activeAllocations, room.getCapacity());
    }

    private void updateRoomOccupancy(Room room, int change) {
        int newOccupancy = (room.getCurrentOccupancy() != null ? room.getCurrentOccupancy() : 0) + change;
        room.setCurrentOccupancy(newOccupancy);

        if (newOccupancy == 0) {
            room.setStatus("AVAILABLE");
        } else if (newOccupancy >= room.getCapacity()) {
            room.setStatus("FULL");
        } else {
            room.setStatus("OCCUPIED");
        }

        roomRepository.save(room);
        log.info("Updated room {} occupancy to {} (status: {})",
                room.getRoomNumber(), newOccupancy, room.getStatus());
    }

    @Transactional
    public RoomAllocationResponse acceptOffer(Long id, AcceptOfferRequest request) {
        log.info("Accepting room allocation offer with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        // Validate expected check-in date
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxDate = now.plusDays(7);
        if (request.getExpectedCheckInDate().isBefore(now.minusHours(1))
                || request.getExpectedCheckInDate().isAfter(maxDate)) {
            throw new IllegalArgumentException("Expected check-in date must be within 7 days from now");
        }

        // Validate status transition
        validateStatusTransition(allocation, AllocationStatus.ACCEPTED);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setExpectedCheckInDate(request.getExpectedCheckInDate());
        allocation.setCheckInExpiryDate(LocalDateTime.now().plusDays(checkInExpiryDays));

        // Update application status to ALLOCATED
        if (allocation.getApplication() != null) {
            allocation.getApplication()
                    .setStatus(com.beezhub.student_accommodation.model.enums.ApplicationStatus.ALLOCATED);
            applicationRepository.save(allocation.getApplication());
        }

        RoomAllocation saved = roomAllocationRepository.save(allocation);

        // Update waiting list entry if student was waiting
        waitingListRepository.findByStudent_IdAndStatus(saved.getStudent().getId(), "WAITING")
            .ifPresent(entry -> {
                entry.setStatus("ALLOCATED");
                waitingListRepository.save(entry);
                log.info("Updated waiting list entry for student {} to ALLOCATED", saved.getStudent().getId());
            });

        log.info("Successfully accepted room allocation with id: {}", id);

        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse declineOffer(Long id, DeclineOfferRequest request) {
        log.info("Declining room allocation offer with id: {} for reason: {}", id, request.getDeclineReason());
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        // Count previous declines for this application
        long previousDeclineCount = 0;
        if (allocation.getApplication() != null) {
            previousDeclineCount = roomAllocationRepository
                    .countDeclinedAllocationsByApplicationId(allocation.getApplication().getId());
        }

        // Release the room
        Room room = allocation.getRoom();
        updateRoomOccupancy(room, -1);

        // Validate status transition and update allocation with decline details
        validateStatusTransition(allocation, AllocationStatus.DECLINED);
        allocation.setStatus(AllocationStatus.DECLINED);
        allocation.setEndDate(LocalDateTime.now());
        allocation.setDeclineReason(request.getDeclineReason());
        allocation.setDeclineReasonText(request.getDeclineReasonText());

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        log.info("Successfully declined room allocation with id: {}. Previous decline count: {}", id, previousDeclineCount);

        // Process based on decline count
        if (previousDeclineCount < MAX_DECLINE_COUNT) {
            // Re-queue the student
            reQueueStudent(allocation, (int) previousDeclineCount + 1);
        } else {
            // Cancel application after 3rd decline
            cancelApplicationAfterMaxDeclines(allocation);
        }

        // Trigger waiting list allocation for the freed room
        try {
            allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("Failed to allocate next from waiting list after decline {}", id, e);
        }

        return roomAllocationMapper.toResponse(saved);
    }

    /**
     * Re-queues a student after declining an offer (if within the decline limit).
     * Tries to directly allocate another room first; only falls back to the waiting list
     * if no room is available.
     */
    private void reQueueStudent(RoomAllocation allocation, int totalDeclineCount) {
        Application application = allocation.getApplication();
        Student student = allocation.getStudent();

        if (application == null) {
            log.warn("No application linked to allocation {}; cannot re-queue", allocation.getId());
            return;
        }

        log.info("Re-queueing student {} after decline {} of {}", student.getId(), totalDeclineCount, MAX_DECLINE_COUNT);

        // Always send to waiting list for fair allocation
        log.info("Adding student {} to waiting list after decline {}", student.getId(), totalDeclineCount);
        application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
        String remarks = String.format("Declined offer %d of %d allowed. Added to waiting list.",
                totalDeclineCount, MAX_DECLINE_COUNT);
        application.setRemarks(remarks);
        applicationRepository.save(application);

        if (!waitingListRepository.existsByStudent_IdAndStatus(student.getId(), "WAITING")) {
            enqueueWaitingList(student, application);
        }

        // Send re-queue notification
        try {
            notificationService.sendOfferDeclinedRequeueNotification(allocation, totalDeclineCount, MAX_DECLINE_COUNT);
        } catch (Exception e) {
            log.error("Failed to send re-queue notification for student {}", student.getId(), e);
        }
    }


    /**
     * Cancels the application when the student has reached the maximum decline limit.
     * Removes from waiting list and sends final notification.
     */
    private void cancelApplicationAfterMaxDeclines(RoomAllocation allocation) {
        Application application = allocation.getApplication();
        Student student = allocation.getStudent();

        if (application == null) {
            log.warn("No application linked to allocation {}; cannot cancel", allocation.getId());
            return;
        }

        log.info("Cancelling application {} after reaching max decline limit for student {}",
                application.getId(), student.getId());

        // Update application status to CANCELLED
        application.setStatus(ApplicationStatus.CANCELLED);
        String remarks = String.format("Application cancelled: Maximum decline limit reached (%d declines). " +
                "Student declined all offered rooms.", MAX_DECLINE_COUNT + 1);
        application.setRemarks(remarks);
        applicationRepository.save(application);

        // Remove from waiting list
        waitingListRepository.findByStudent_IdAndStatus(student.getId(), "WAITING")
                .ifPresent(entry -> {
                    entry.setStatus("CANCELLED");
                    waitingListRepository.save(entry);
                    log.info("Cancelled waiting list entry for student {}", student.getId());
                });

        // Send max decline reached notification
        try {
            notificationService.sendOfferDeclinedMaxReachedNotification(allocation, MAX_DECLINE_COUNT + 1);
        } catch (Exception e) {
            log.error("Failed to send max decline notification for student {}", student.getId(), e);
        }
    }

    @Transactional
    public RoomAllocationResponse confirmCheckIn(Long id) {
        log.info("Confirming check-in for allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        // Validate status transition
        validateStatusTransition(allocation, AllocationStatus.ACTIVE);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setCheckInDate(LocalDateTime.now());

        // Update application status to CHECKED_IN
        if (allocation.getApplication() != null) {
            Application application = allocation.getApplication();
            application.setStatus(com.beezhub.student_accommodation.model.enums.ApplicationStatus.CHECKED_IN);
            applicationRepository.save(application);
            log.info("Updated application {} status to CHECKED_IN", application.getId());
        }

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        log.info("Successfully checked in allocation with id: {}", id);
        
        // Send check-in confirmation notifications
        try {
            if (allocation.getStudent() != null && allocation.getStudent().getAppUser() != null) {
                userNotificationService.createCheckInConfirmedNotification(
                    allocation.getStudent().getAppUser().getId(),
                    allocation.getRoom().getRoomNumber(),
                    allocation.getRoom().getBuilding() != null ? 
                        allocation.getRoom().getBuilding().getName() : "Unknown Building"
                );
            }
        } catch (Exception e) {
            log.error("Failed to send check-in confirmation notification for allocation {}", id, e);
        }
        
        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse confirmCheckOut(Long id) {
        log.info("Confirming check-out for allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        // Validate status transition
        validateStatusTransition(allocation, AllocationStatus.COMPLETED);
        allocation.setStatus(AllocationStatus.COMPLETED);
        allocation.setCheckOutDate(LocalDateTime.now());
        allocation.setEndDate(LocalDateTime.now());

        // Update room occupancy
        updateRoomOccupancy(allocation.getRoom(), -1);

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        log.info("Successfully checked out allocation with id: {}", id);

        try {
            allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("Failed to allocate next from waiting list after check-out {}", id, e);
        }

        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public void deleteRoomAllocation(Long id) {
        log.info("Cancelling (deleting) room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        // Use soft delete (CANCELLED) instead of hard delete
        validateStatusTransition(allocation, AllocationStatus.CANCELLED);
        allocation.setStatus(AllocationStatus.CANCELLED);
        allocation.setEndDate(LocalDateTime.now());

        Room room = allocation.getRoom();
        updateRoomOccupancy(room, -1);

        roomAllocationRepository.save(allocation);
        log.info("Successfully cancelled room allocation with id: {}", id);

        try {
            allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("Failed to allocate next from waiting list after deletion {}", id, e);
        }
    }

    public Room findAvailableRoom() {
        Optional<Room> room = roomRepository.findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc("AVAILABLE");
        room.ifPresent(r -> log.info("Found available room {}", r.getRoomNumber()));
        return room.orElse(null);
    }

    @Transactional
    public RoomAllocationResponse updateRoomAllocation(Long id, RoomAllocationRequest request) {
        log.info("Updating room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        Room oldRoom = allocation.getRoom();
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(
                        () -> new StudentNotFoundException("Student not found with id: " + request.getStudentId()));

        Room newRoom = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + request.getRoomId()));

        // If room is changing, validate capacity and update occupancy
        boolean roomChanged = false;
        if (!oldRoom.getId().equals(newRoom.getId())) {
            validateRoomCapacity(newRoom);
            updateRoomOccupancy(oldRoom, -1);
            updateRoomOccupancy(newRoom, 1);
            roomChanged = true;
        }

        allocation.setStudent(student);
        allocation.setRoom(newRoom);

        if (request.getAllocationDate() != null) {
            allocation.setAllocationDate(request.getAllocationDate());
        }

        if (request.getEndDate() != null) {
            allocation.setEndDate(request.getEndDate());
        }

        RoomAllocation updated = roomAllocationRepository.save(allocation);
        log.info("Successfully updated room allocation with id: {}", id);

        // Send notification if room was changed
        if (roomChanged) {
            try {
                log.info("Triggering room change notification for student {} from room {} to room {}",
                        student.getId(), oldRoom.getRoomNumber(), newRoom.getRoomNumber());
                notificationService.sendRoomAllocationNotification(updated);
            } catch (Exception e) {
                log.error(
                        "Failed to trigger notification for room allocation update {} - notification will be retried by scheduler",
                        updated.getId(), e);
            }
        }

        return roomAllocationMapper.toResponse(updated);
    }

    @Transactional
    public RoomAllocationResponse endAllocation(Long id) {
        log.info("Ending room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        // Validate status transition
        validateStatusTransition(allocation, AllocationStatus.COMPLETED);
        allocation.setStatus(AllocationStatus.COMPLETED);
        allocation.setEndDate(LocalDateTime.now());

        // Update room occupancy
        updateRoomOccupancy(allocation.getRoom(), -1);

        RoomAllocation updated = roomAllocationRepository.save(allocation);
        log.info("Successfully ended room allocation with id: {}", id);

        try {
            allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("Failed to allocate next from waiting list after ending allocation {}", id, e);
        }

        return roomAllocationMapper.toResponse(updated);
    }

    public RoomAllocationResponse getActiveAllocationByStudentId(Long studentId) {
        log.info("Fetching active allocation for student: {}", studentId);
        RoomAllocation allocation = roomAllocationRepository.findActiveAllocationByStudentId(studentId)
                .orElseThrow(() -> new RoomAllocationNotFoundException(
                        "No active allocation found for student with id: " + studentId));
        return roomAllocationMapper.toResponse(allocation);
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

        return createRoomAllocation(request);
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

    /**
     * Gets eligible rooms for a student based on hard constraints.
     * Filters rooms by gender, accessibility, year of study, and institution.
     *
     * @param studentId  The student ID
     * @param buildingId Optional building ID to filter by
     * @return List of eligible rooms
     */
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

    /**
     * Allocates the next student from waiting list (wrapper for batch allocation).
     * Delegates to batchAllocateFromWaitingList with limit of 1.
     */
    @Transactional
    public void allocateNextFromWaitingList() {
        // Delegate to batch method with limit of 1
        BatchAllocationResult result = batchAllocateFromWaitingList(1);

        log.info("Single allocation attempt: {} succeeded, {} failed",
                result.getSuccessCount(), result.getFailureCount());
    }

    /**
     * Batch allocates multiple waiting students to available rooms using optimal matching.
     * Processes up to maxAllocations students in FIFO order from waiting list.
     *
     * @param maxAllocations Maximum number of allocations to create
     * @return BatchAllocationResult with success/failure statistics
     */
    @Transactional
    public BatchAllocationResult batchAllocateFromWaitingList(int maxAllocations) {
        long startTime = System.currentTimeMillis();

        // Feature flag check
        try {
            if (!ff4j.check(com.beezhub.student_accommodation.config.FF4jConfig.FEATURE_AUTO_ALLOCATION)) {
                log.info("Auto-allocation is disabled via Feature Toggle. Skipping batch allocation.");
                return BatchAllocationResult.builder()
                        .processedAt(LocalDateTime.now())
                        .processingTimeMs(0)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error checking auto-allocation feature flag: {}", e.getMessage(), e);
            return BatchAllocationResult.builder()
                    .processedAt(LocalDateTime.now())
                    .processingTimeMs(0)
                    .build();
        }

        log.info("Starting batch allocation for up to {} students from waiting list", maxAllocations);

        // 1. Fetch all waiting students
        List<WaitingListEntry> waitingList = waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING");
        if (waitingList.isEmpty()) {
            log.info("No students on waiting list");
            return BatchAllocationResult.builder()
                    .processedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 2. Build scoring matrix
        Map<Long, List<ScoredRoom>> scoringMatrix = buildScoringMatrix(waitingList);

        // 3. Fetch available rooms and track capacity
        List<Room> availableRooms = getAvailableRoomsForBatch();
        if (availableRooms.isEmpty()) {
            log.info("No available rooms for batch allocation");
            return BatchAllocationResult.builder()
                    .processedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        Map<Long, Integer> roomCapacityRemaining = buildRoomCapacityMap(availableRooms);

        // 4. Build allocation pairs (greedy matching)
        List<AllocationPair> allocationPairs = new ArrayList<>();
        int allocatedCount = 0;

        for (WaitingListEntry entry : waitingList) {
            if (allocatedCount >= maxAllocations) break;

            Student student = entry.getStudent();
            List<ScoredRoom> scoredRooms = scoringMatrix.get(student.getId());

            if (scoredRooms == null || scoredRooms.isEmpty()) continue;

            // Find first available room (sorted by score DESC)
            for (ScoredRoom scoredRoom : scoredRooms) {
                Integer remaining = roomCapacityRemaining.get(scoredRoom.getRoomId());
                if (remaining != null && remaining > 0) {
                    // Allocate this student to this room
                    allocationPairs.add(new AllocationPair(entry, scoredRoom));
                    roomCapacityRemaining.put(scoredRoom.getRoomId(), remaining - 1);
                    allocatedCount++;
                    break; // Move to next student
                }
            }
        }

        // 5. Create allocations in batch
        BatchAllocationResult result = createAllocationsInBatch(allocationPairs);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setProcessedAt(LocalDateTime.now());

        log.info("Batch allocation completed: {} succeeded, {} failed, {}ms processing time",
                result.getSuccessCount(), result.getFailureCount(), result.getProcessingTimeMs());

        return result;
    }

    /**
     * Builds scoring matrix for all waiting students.
     * Returns map of studentId -> sorted list of scored rooms.
     */
    private Map<Long, List<ScoredRoom>> buildScoringMatrix(List<WaitingListEntry> waitingList) {
        Map<Long, List<ScoredRoom>> scoringMatrix = new HashMap<>();

        for (WaitingListEntry entry : waitingList) {
            Student student = entry.getStudent();

            // Skip if already has active allocation
            if (roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())) {
                continue;
            }

            Application app = entry.getApplication();
            Building preferredBuilding = app != null ? app.getPreferredBuilding() : null;

            List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(
                    student, app, preferredBuilding);

            if (!scoredRooms.isEmpty()) {
                scoringMatrix.put(student.getId(), scoredRooms);
            }
        }

        return scoringMatrix;
    }

    /**
     * Builds map of roomId -> available spots.
     * Tracks remaining capacity as allocations are made.
     */
    private Map<Long, Integer> buildRoomCapacityMap(List<Room> rooms) {
        Map<Long, Integer> capacityMap = new HashMap<>();

        for (Room room : rooms) {
            long activeCount = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
            int availableSpots = room.getCapacity() - (int) activeCount;
            if (availableSpots > 0) {
                capacityMap.put(room.getId(), availableSpots);
            }
        }

        return capacityMap;
    }

    /**
     * Gets all rooms that have available capacity.
     */
    private List<Room> getAvailableRoomsForBatch() {
        List<Room> allRooms = roomRepository.findAll();
        List<Room> availableRooms = new ArrayList<>();

        for (Room room : allRooms) {
            long activeCount = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
            if (activeCount < room.getCapacity()) {
                availableRooms.add(room);
            }
        }

        return availableRooms;
    }

    /**
     * Creates allocations for all pairs with individual transaction isolation.
     * Handles partial failures gracefully.
     */
    private BatchAllocationResult createAllocationsInBatch(List<AllocationPair> allocationPairs) {
        BatchAllocationResult.BatchAllocationResultBuilder resultBuilder = BatchAllocationResult.builder();
        resultBuilder.totalAttempted(allocationPairs.size());

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        List<Long> allocatedStudentIds = new ArrayList<>();
        List<Long> allocatedRoomIds = new ArrayList<>();

        for (AllocationPair pair : allocationPairs) {
            try {
                // Create allocation in separate transaction for resilience
                transactionTemplate.execute(status -> {
                    RoomAllocationRequest request = new RoomAllocationRequest();
                    request.setStudentId(pair.getStudent().getId());
                    request.setRoomId(pair.getRoomId());
                    if (pair.getApplication() != null) {
                        request.setApplicationId(pair.getApplication().getId());
                    }

                    createRoomAllocation(request);

                    // Update waiting list entry
                    pair.getWaitingListEntry().setStatus("ALLOCATED");
                    waitingListRepository.save(pair.getWaitingListEntry());

                    return null;
                });

                successCount++;
                allocatedStudentIds.add(pair.getStudent().getId());
                allocatedRoomIds.add(pair.getRoomId());

                log.debug("Successfully allocated student {} to room {}",
                        pair.getStudent().getId(), pair.getRoomId());

            } catch (Exception e) {
                failureCount++;
                String errorMsg = String.format("Failed to allocate student %d to room %d: %s",
                        pair.getStudent().getId(), pair.getRoomId(), e.getMessage());
                errors.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        return resultBuilder
                .successCount(successCount)
                .failureCount(failureCount)
                .errors(errors)
                .allocatedStudentIds(allocatedStudentIds)
                .allocatedRoomIds(allocatedRoomIds)
                .build();
    }

    @Transactional
    public MessageResponse enqueueWaitingList(Long studentId, Long applicationId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found with id: " + applicationId));

        if (waitingListRepository.existsByStudent_IdAndStatus(studentId, "WAITING")) {
            log.info("Student {} is already on the waiting list", studentId);
            return new MessageResponse("Student is already on the waiting list");
        }

        enqueueWaitingList(student, application);
        return new MessageResponse("Student added to waiting list");
    }

    private void enqueueWaitingList(Student student, Application application) {
        // Check if student is already on the waiting list to prevent duplicates
        if (waitingListRepository.existsByStudent_IdAndStatus(student.getId(), "WAITING")) {
            log.info("Student {} is already on the waiting list, skipping", student.getId());
            return;
        }

        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        waitingListRepository.save(entry);
        log.warn("No rooms available; student {} placed on waiting list", student.getId());
    }

    // ==================== Batch Allocation Proposal Methods ====================

    /**
     * Proposes allocations for a batch of students.
     * Creates staged allocations that require admin confirmation.
     *
     * @param request The batch allocation request
     * @param adminId The admin user ID
     * @return BatchAllocationProposalResponse with all proposals and warnings
     */
    @Transactional
    public BatchAllocationProposalResponse proposeAllocation(BatchAllocationRequest request, Long adminId) {
        log.info("Creating batch allocation proposal for {} students, batch: {}",
                request.getStudentIds().size(), request.getBatchId());

        AppUser admin = appUserRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminId));

        Building preferredBuilding = null;
        if (request.getPreferredBuildingId() != null) {
            preferredBuilding = buildingRepository.findById(request.getPreferredBuildingId()).orElse(null);
        }

        BatchAllocationProposalResponse response = BatchAllocationProposalResponse.builder()
                .batchId(request.getBatchId())
                .totalStudents(request.getStudentIds().size())
                .createdAt(LocalDateTime.now())
                .createdById(adminId)
                .createdByName(admin.getFirstName() + " " + admin.getLastName())
                .proposals(new ArrayList<>())
                .warnings(new ArrayList<>())
                .failures(new ArrayList<>())
                .build();

        int successCount = 0;
        int failCount = 0;

        for (Long studentId : request.getStudentIds()) {
            try {
                StagedAllocationResponse proposal = createProposalForStudent(studentId, request.getBatchId(),
                        preferredBuilding, admin);
                response.getProposals().add(proposal);
                successCount++;

                // Send notification to student
                try {
                    userNotificationService.createAllocationProposalNotification(
                            studentRepository.findById(studentId).get().getAppUser().getId(),
                            proposal.getRoomNumber(),
                            proposal.getBuildingName());
                } catch (Exception e) {
                    log.error("Failed to send proposal notification for student {}", studentId, e);
                }

            } catch (Exception e) {
                log.error("Failed to create proposal for student {}: {}", studentId, e.getMessage());
                String studentName = studentRepository.findById(studentId)
                        .map(s -> s.getAppUser().getFirstName() + " " + s.getAppUser().getLastName())
                        .orElse("Unknown");
                response.addFailure(studentId, studentName, e.getMessage());
                failCount++;
            }
        }

        response.setSuccessfulProposals(successCount);
        response.setFailedProposals(failCount);

        log.info("Batch proposal completed: {} successful, {} failed", successCount, failCount);
        return response;
    }

    private StagedAllocationResponse createProposalForStudent(Long studentId, String batchId,
            Building preferredBuilding, AppUser admin) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found: " + studentId));

        // Get latest AWAITING_ALLOCATION application
        Application application = applicationRepository.findByStudent_Id(studentId).stream()
                .filter(app -> app.getStatus() == ApplicationStatus.AWAITING_ALLOCATION)
                .findFirst()
                .orElseThrow(
                        () -> new RuntimeException("No AWAITING_ALLOCATION application for student: " + studentId));

        // Check if student already has active allocation
        if (roomAllocationRepository.existsActiveAllocationByStudentId(studentId)) {
            throw new StudentAlreadyAllocatedException("Student already has active allocation");
        }

        // Check if student already has pending proposal
        Optional<StagedAllocation> existingProposal = stagedAllocationRepository
                .findByStudent_IdAndStatus(studentId, StagedAllocationStatus.PROPOSED);
        if (existingProposal.isPresent()) {
            throw new RuntimeException("Student already has a pending proposal");
        }

        // Use preferred building from request or application
        Building effectiveBuilding = preferredBuilding != null ? preferredBuilding : application.getPreferredBuilding();

        // Score available rooms
        List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(student, application,
                effectiveBuilding);
        if (scoredRooms.isEmpty()) {
            enqueueWaitingList(student, application);
            throw new RuntimeException("No available rooms for student; added to waiting list");
        }

        ScoredRoom bestRoom = scoredRooms.get(0);
        Room room = roomRepository.findById(bestRoom.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + bestRoom.getRoomId()));

        // Create staged allocation
        StagedAllocation staged = StagedAllocation.builder()
                .student(student)
                .room(room)
                .application(application)
                .status(StagedAllocationStatus.PROPOSED)
                .score(bestRoom.getTotalScore())
                .warnings(serializeWarnings(bestRoom.getWarnings()))
                .proposedBy(admin)
                .batchId(batchId)
                .build();

        StagedAllocation saved = stagedAllocationRepository.save(staged);
        log.info("Created proposal for student {} to room {} with score {}",
                studentId, room.getRoomNumber(), bestRoom.getTotalScore());

        return mapToStagedAllocationResponse(saved);
    }

    /**
     * Confirms a single staged allocation.
     */
    @Transactional
    public RoomAllocationResponse confirmAllocation(Long stagedAllocationId, Long adminId) {
        log.info("Confirming allocation {} by admin {}", stagedAllocationId, adminId);

        StagedAllocation staged = stagedAllocationRepository.findById(stagedAllocationId)
                .orElseThrow(() -> new RuntimeException("Staged allocation not found: " + stagedAllocationId));

        if (staged.getStatus() != StagedAllocationStatus.PROPOSED) {
            throw new RuntimeException("Can only confirm PROPOSED allocations. Current status: " + staged.getStatus());
        }

        // Validate room still has capacity
        validateRoomCapacity(staged.getRoom());

        // Create the actual room allocation
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(staged.getStudent());
        allocation.setRoom(staged.getRoom());
        allocation.setApplication(staged.getApplication());
        allocation.setAllocationDate(LocalDateTime.now());
        // Initial status for new allocation from staged
        validateStatusTransition(allocation, AllocationStatus.OFFERED);
        allocation.setStatus(AllocationStatus.OFFERED); // Staged allocation creates an OFFER that student must accept
        allocation.setAllocationMethod(AllocationMethod.AUTO);

        RoomAllocation saved = roomAllocationRepository.save(allocation);

        // Update room occupancy
        updateRoomOccupancy(staged.getRoom(), 1);

        // Update staged allocation status
        staged.setStatus(StagedAllocationStatus.CONFIRMED);
        staged.setConfirmedAt(LocalDateTime.now());
        stagedAllocationRepository.save(staged);

        // Update application status to ALLOCATED
        // Update application status to ALLOCATED - MOVED TO acceptOffer
        // Application remains APPROVED/OFFERED until student accepts
        /*
         * if (staged.getApplication() != null) {
         * staged.getApplication().setStatus(ApplicationStatus.ALLOCATED);
         * applicationRepository.save(staged.getApplication());
         * }
         */

        // Log to audit trail
        AppUser admin = appUserRepository.findById(adminId).orElse(null);
        logAuditAction(saved, AllocationAuditActionType.FORCE_ALLOCATE, admin,
                "Confirmed from staged allocation batch: " + staged.getBatchId(), null, staged.getRoom());

        // Send confirmation notification
        try {
            notificationService.sendRoomAllocationNotification(saved);
            userNotificationService.createAllocationConfirmationNotification(
                    staged.getStudent().getAppUser().getId(),
                    staged.getRoom().getRoomNumber(),
                    staged.getRoom().getBuilding().getName());
        } catch (Exception e) {
            log.error("Failed to send confirmation notification", e);
        }

        log.info("Confirmed allocation {} for student {} to room {}",
                saved.getId(), staged.getStudent().getId(), staged.getRoom().getRoomNumber());

        return roomAllocationMapper.toResponse(saved);
    }

    /**
     * Rejects a staged allocation.
     */
    @Transactional
    public void rejectAllocation(Long stagedAllocationId, String reason, Long adminId) {
        log.info("Rejecting allocation {} by admin {}: {}", stagedAllocationId, adminId, reason);

        StagedAllocation staged = stagedAllocationRepository.findById(stagedAllocationId)
                .orElseThrow(() -> new RuntimeException("Staged allocation not found: " + stagedAllocationId));

        if (staged.getStatus() != StagedAllocationStatus.PROPOSED) {
            throw new RuntimeException("Can only reject PROPOSED allocations");
        }

        staged.setStatus(StagedAllocationStatus.REJECTED);
        stagedAllocationRepository.save(staged);

        // Keep student in AWAITING_ALLOCATION status (can be re-proposed)
        log.info("Rejected allocation {} - student {} remains in AWAITING_ALLOCATION",
                stagedAllocationId, staged.getStudent().getId());

        // Send rejection notification
        try {
            userNotificationService.createAllocationRejectionNotification(
                    staged.getStudent().getAppUser().getId(),
                    staged.getRoom().getRoomNumber(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send rejection notification", e);
        }
    }

    /**
     * Confirms all allocations in a batch.
     */
    @Transactional
    public void confirmBatchAllocations(String batchId, Long adminId) {
        log.info("Confirming batch allocations for batch {} by admin {}", batchId, adminId);

        List<StagedAllocation> proposals = stagedAllocationRepository
                .findByBatchIdAndStatus(batchId, StagedAllocationStatus.PROPOSED);

        if (proposals.isEmpty()) {
            log.warn("No PROPOSED allocations found for batch {}", batchId);
            return;
        }

        int confirmed = 0;
        int failed = 0;

        for (StagedAllocation staged : proposals) {
            try {
                confirmAllocation(staged.getId(), adminId);
                confirmed++;
            } catch (Exception e) {
                log.error("Failed to confirm allocation {} in batch {}: {}",
                        staged.getId(), batchId, e.getMessage());
                failed++;
            }
        }

        log.info("Batch {} confirmation complete: {} confirmed, {} failed", batchId, confirmed, failed);
    }

    // ==================== Move and Override Methods ====================

    /**
     * Moves a student from one room to another.
     */
    @Transactional
    public RoomAllocationResponse moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId) {
        log.info("Moving allocation {} to room {} by admin {}: {}",
                allocationId, newRoomId, adminId, reason);

        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(allocationId)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Allocation not found: " + allocationId));

        if (!"ACTIVE".equals(allocation.getStatus())) {
            throw new RuntimeException("Can only move ACTIVE allocations");
        }

        Room oldRoom = allocation.getRoom();
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new RoomNotFoundException("New room not found: " + newRoomId));

        // Validate new room has capacity
        validateRoomCapacity(newRoom);

        // Validate hard constraints
        if (!allocationScoringService.validateHardConstraints(allocation.getStudent(), newRoom, false)) {
            throw new RuntimeException("New room does not meet hard constraints for student");
        }

        // Update room occupancies
        updateRoomOccupancy(oldRoom, -1);
        updateRoomOccupancy(newRoom, 1);

        // Update allocation
        allocation.setRoom(newRoom);
        RoomAllocation updated = roomAllocationRepository.save(allocation);

        // Log to audit trail
        AppUser admin = appUserRepository.findById(adminId).orElse(null);
        logAuditAction(allocation, AllocationAuditActionType.MOVE, admin, reason, oldRoom, newRoom);

        // Send notification
        try {
            userNotificationService.createRoomMoveNotification(
                    allocation.getStudent().getAppUser().getId(),
                    oldRoom.getRoomNumber(),
                    newRoom.getRoomNumber(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send move notification", e);
        }

        log.info("Moved student {} from room {} to room {}",
                allocation.getStudent().getId(), oldRoom.getRoomNumber(), newRoom.getRoomNumber());

        return roomAllocationMapper.toResponse(updated);
    }

    /**
     * Overrides constraints to force allocate a student to a room.
     */
    @Transactional
    public StagedAllocationResponse overrideConstraints(Long studentId, Long roomId, String reason,
            Long adminId, List<String> constraintsBypassed) {
        log.info("Override allocation for student {} to room {} by admin {}: {}",
                studentId, roomId, adminId, reason);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found: " + studentId));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        // Check room capacity
        validateRoomCapacity(room);

        // Get application
        Application application = applicationRepository.findByStudent_Id(studentId).stream()
                .filter(app -> app.getStatus() == ApplicationStatus.AWAITING_ALLOCATION)
                .findFirst()
                .orElse(null);

        if (application == null) {
            throw new ApplicationNotFoundException(
                    "No application awaiting allocation found for student ID: " + studentId);
        }

        AppUser admin = appUserRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminId));

        // Create staged allocation bypassing constraints
        StagedAllocation staged = StagedAllocation.builder()
                .student(student)
                .room(room)
                .application(application)
                .status(StagedAllocationStatus.PROPOSED)
                .score(allocationScoringService.scoreSingleRoom(student, application, room).getTotalScore())
                .warnings(serializeWarnings(List.of("CONSTRAINT_OVERRIDE: " + String.join(", ", constraintsBypassed))))
                .proposedBy(admin)
                .batchId("OVERRIDE-" + System.currentTimeMillis())
                .build();

        StagedAllocation saved = stagedAllocationRepository.save(staged);

        // Log override to audit
        logAuditActionWithConstraint(null, AllocationAuditActionType.OVERRIDE, admin, reason,
                null, room, String.join(", ", constraintsBypassed));

        // Send notification
        try {
            userNotificationService.createAllocationProposalNotification(
                    student.getAppUser().getId(),
                    room.getRoomNumber(),
                    room.getBuilding().getName());
        } catch (Exception e) {
            log.error("Failed to send override proposal notification", e);
        }

        log.info("Created override proposal for student {} to room {}", studentId, room.getRoomNumber());

        return mapToStagedAllocationResponse(saved);
    }

    // ==================== Staged Allocations Query Methods ====================

    /**
     * Gets paginated staged allocations filtered by batch and/or status.
     */
    public PageResponse<StagedAllocationResponse> getStagedAllocations(String batchId, StagedAllocationStatus status,
            Pageable pageable) {
        log.info("Getting staged allocations - batchId: {}, status: {}", batchId, status);

        Page<StagedAllocation> page;

        if (batchId != null && status != null) {
            page = stagedAllocationRepository.findByBatchIdAndStatus(batchId, status, pageable);
        } else if (batchId != null) {
            page = stagedAllocationRepository.findByBatchId(batchId, pageable);
        } else if (status != null) {
            page = stagedAllocationRepository.findByStatus(status, pageable);
        } else {
            page = stagedAllocationRepository.findAll(pageable);
        }

        Page<StagedAllocationResponse> responsePage = page.map(this::mapToStagedAllocationResponse);
        log.info("Found {} staged allocations", page.getTotalElements());

        return PageMapper.toPageResponse(responsePage);
    }

    /**
     * Confirms batch allocations and returns result statistics.
     */
    @Transactional
    public BatchConfirmationResult confirmBatchAllocationsWithResult(String batchId, Long adminId) {
        log.info("Confirming batch allocations for batch {} by admin {}", batchId, adminId);

        List<StagedAllocation> proposals = stagedAllocationRepository
                .findByBatchIdAndStatus(batchId, StagedAllocationStatus.PROPOSED);

        if (proposals.isEmpty()) {
            log.warn("No PROPOSED allocations found for batch {}", batchId);
            return BatchConfirmationResult.builder()
                    .success(true)
                    .confirmed(0)
                    .failed(0)
                    .build();
        }

        int confirmed = 0;
        int failed = 0;

        for (StagedAllocation staged : proposals) {
            try {
                confirmAllocation(staged.getId(), adminId);
                confirmed++;
            } catch (Exception e) {
                log.error("Failed to confirm allocation {} in batch {}: {}",
                        staged.getId(), batchId, e.getMessage());
                failed++;
            }
        }

        log.info("Batch {} confirmation complete: {} confirmed, {} failed", batchId, confirmed, failed);

        return BatchConfirmationResult.builder()
                .success(true)
                .confirmed(confirmed)
                .failed(failed)
                .build();
    }

    // ==================== Waiting List with Scores ====================

    /**
     * Gets waiting list entries with scoring information.
     */
    public List<WaitingListEntryResponse> getWaitingListWithScores(Long buildingId) {
        log.info("Getting waiting list with scores for building {}", buildingId);

        List<WaitingListEntry> entries = waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING");
        List<WaitingListEntryResponse> responses = new ArrayList<>();

        Building targetBuilding = buildingId != null ? buildingRepository.findById(buildingId).orElse(null) : null;

        AtomicInteger position = new AtomicInteger(1);

        for (WaitingListEntry entry : entries) {
            Student student = entry.getStudent();
            Application application = entry.getApplication();

            WaitingListEntryResponse response = WaitingListEntryResponse.builder()
                    .id(entry.getId())
                    .studentId(student.getId())
                    .studentNumber(student.getStudentNumber())
                    .studentName(student.getAppUser().getFirstName() + " " + student.getAppUser().getLastName())
                    .studentEmail(student.getAppUser().getEmail())
                    .studentGender(student.getGender())
                    .status(entry.getStatus())
                    .createdAt(entry.getCreatedAt())
                    .position(position.getAndIncrement())
                    .build();

            if (application != null) {
                response.setApplicationId(application.getId());
                response.setApplicationCode(application.getApplicationCode());
                response.setApplicationStatus(application.getStatus().name());

                if (application.getPreferredBuilding() != null) {
                    response.setPreferredBuildingId(application.getPreferredBuilding().getId());
                    response.setPreferredBuildingName(application.getPreferredBuilding().getName());
                }
            }

            // Score available rooms for this student
            Building effectiveBuilding = targetBuilding != null ? targetBuilding
                    : (application != null ? application.getPreferredBuilding() : null);
            List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(student, application,
                    effectiveBuilding);

            if (!scoredRooms.isEmpty()) {
                ScoredRoom bestRoom = scoredRooms.get(0);
                response.setBestRoomScore(bestRoom.getTotalScore());
                response.setBestMatchRoomId(bestRoom.getRoomId());
                response.setBestMatchRoomNumber(bestRoom.getRoomNumber());
                response.setPriorityScore(bestRoom.getTotalScore());
            } else {
                response.setPriorityScore(BigDecimal.ZERO);
            }

            responses.add(response);
        }

        // Sort by priority score descending
        responses.sort(Comparator.comparing(WaitingListEntryResponse::getPriorityScore).reversed());

        // Re-assign positions after sorting
        for (int i = 0; i < responses.size(); i++) {
            responses.get(i).setPosition(i + 1);
        }

        log.info("Returning {} waiting list entries with scores", responses.size());
        return responses;
    }

    /**
     * Rescores all waiting list entries using the latest allocation rules.
     * This is an idempotent operation that refreshes scores based on current rules
     * and room availability.
     *
     * @return RescoreResult with count of rescored entries
     */
    public RescoreResult rescoreWaitingList() {
        log.info("Rescoring all waiting list entries using latest rules");

        List<WaitingListEntry> entries = waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING");

        if (entries.isEmpty()) {
            log.info("No waiting list entries to rescore");
            return RescoreResult.builder()
                    .success(true)
                    .rescored(0)
                    .message("No waiting list entries to rescore")
                    .build();
        }

        int rescored = 0;
        for (WaitingListEntry entry : entries) {
            try {
                Student student = entry.getStudent();
                Application application = entry.getApplication();
                Building preferredBuilding = application != null ? application.getPreferredBuilding() : null;

                // Rescore by getting scored rooms (this uses latest rules)
                allocationScoringService.scoreAvailableRooms(
                        student, application, preferredBuilding);

                // The actual score is computed and stored in responses during
                // getWaitingListWithScores
                // This operation effectively validates that scoring works for each entry
                rescored++;
            } catch (Exception e) {
                log.error("Failed to rescore entry {} for student {}: {}",
                        entry.getId(), entry.getStudent().getId(), e.getMessage());
            }
        }

        log.info("Rescored {} waiting list entries", rescored);

        return RescoreResult.builder()
                .success(true)
                .rescored(rescored)
                .message("Successfully rescored " + rescored + " waiting list entries")
                .build();
    }

    // ==================== Helper Methods ====================

    private void logAuditAction(RoomAllocation allocation, AllocationAuditActionType actionType,
            AppUser admin, String reason, Room oldRoom, Room newRoom) {
        logAuditActionWithConstraint(allocation, actionType, admin, reason, oldRoom, newRoom, null);
    }

    private void logAuditActionWithConstraint(RoomAllocation allocation, AllocationAuditActionType actionType,
            AppUser admin, String reason, Room oldRoom, Room newRoom,
            String constraintBypassed) {
        AllocationAuditLog auditLog = AllocationAuditLog.builder()
                .allocation(allocation)
                .actionType(actionType)
                .adminUser(admin)
                .reason(reason)
                .oldRoom(oldRoom)
                .newRoom(newRoom)
                .constraintBypassed(constraintBypassed)
                .build();
        allocationAuditLogRepository.save(auditLog);
        log.info("Audit log created: {} for allocation {}", actionType,
                allocation != null ? allocation.getId() : "N/A");
    }

    private String serializeWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(warnings);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize warnings", e);
            return String.join("; ", warnings);
        }
    }

    private List<String> deserializeWarnings(String warningsJson) {
        if (warningsJson == null || warningsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(warningsJson, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize warnings", e);
            return List.of(warningsJson);
        }
    }

    private StagedAllocationResponse mapToStagedAllocationResponse(StagedAllocation staged) {
        Student student = staged.getStudent();
        Room room = staged.getRoom();
        Application application = staged.getApplication();

        long currentOccupancy = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());

        return StagedAllocationResponse.builder()
                .id(staged.getId())
                .status(staged.getStatus())
                .score(staged.getScore())
                .warnings(deserializeWarnings(staged.getWarnings()))
                .batchId(staged.getBatchId())
                .createdAt(staged.getCreatedAt())
                .updatedAt(staged.getUpdatedAt())
                .confirmedAt(staged.getConfirmedAt())
                .studentId(student.getId())
                .studentNumber(student.getStudentNumber())
                .studentFirstName(student.getAppUser().getFirstName())
                .studentLastName(student.getAppUser().getLastName())
                .studentEmail(student.getAppUser().getEmail())
                .studentGender(student.getGender())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomCapacity(room.getCapacity())
                .currentOccupancy((int) currentOccupancy)
                .buildingId(room.getBuilding() != null ? room.getBuilding().getId() : null)
                .buildingName(room.getBuilding() != null ? room.getBuilding().getName() : null)
                .applicationId(application != null ? application.getId() : null)
                .applicationCode(application != null ? application.getApplicationCode() : null)
                .applicationStatus(application != null ? application.getStatus().name() : null)
                .proposedById(staged.getProposedBy() != null ? staged.getProposedBy().getId() : null)
                .proposedByName(staged.getProposedBy() != null
                        ? staged.getProposedBy().getFirstName() + " " + staged.getProposedBy().getLastName()
                        : null)
                .build();
    }

    /**
     * Periodic batch allocation job.
     * Processes waiting list students when rooms are available.
     * Runs every 6 hours (configurable via app.allocation.batch-interval-ms).
     */
    @Scheduled(fixedDelayString = "${app.allocation.batch-interval-ms:21600000}")
    public void scheduledBatchAllocation() {
        // Check if batch job is enabled
        boolean batchEnabled = environment.getProperty("app.allocation.batch-enabled", Boolean.class, true);
        if (!batchEnabled) {
            return;
        }

        log.info("Starting scheduled batch allocation job");

        try {
            long waitingCount = waitingListRepository.countByStatus("WAITING");

            if (waitingCount == 0) {
                log.info("No students on waiting list, skipping batch allocation");
                return;
            }

            int batchSize = environment.getProperty("app.allocation.batch-size", Integer.class, 50);
            BatchAllocationResult result = batchAllocateFromWaitingList(batchSize);

            log.info("Scheduled batch allocation completed: {} succeeded, {} failed out of {} waiting",
                    result.getSuccessCount(), result.getFailureCount(), waitingCount);

        } catch (Exception e) {
            log.error("Scheduled batch allocation failed", e);
        }
    }

    /**
     * Scheduled job to expire offers that haven't been responded to within the expiry period.
     * Runs every hour (configurable via app.allocation.expiry-check-interval-ms).
     */
    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void expireOffers() {
        log.info("Starting scheduled job to expire unresponsive offers");

        try {
            List<RoomAllocation> expiredOffers = roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(
                    AllocationStatus.OFFERED, LocalDateTime.now());

            if (expiredOffers.isEmpty()) {
                log.info("No expired offers found");
                return;
            }

            log.info("Found {} expired offers to process", expiredOffers.size());
            int successCount = 0;
            int failureCount = 0;

            for (RoomAllocation allocation : expiredOffers) {
                try {
                    expireSingleOffer(allocation);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to expire offer for allocation {}: {}", allocation.getId(), e.getMessage(), e);
                }
            }

            log.info("Offer expiry job completed: {} succeeded, {} failed", successCount, failureCount);

            // Trigger waiting list allocation for freed rooms
            if (successCount > 0) {
                try {
                    // Use batch allocation if configured, otherwise fall back to single allocation
                    boolean useBatchOnExpiry = environment.getProperty("app.allocation.batch-on-expiry", Boolean.class, true);
                    if (useBatchOnExpiry) {
                        log.info("Triggering batch allocation after expiring {} offers", successCount);
                        batchAllocateFromWaitingList(successCount);
                    } else {
                        allocateNextFromWaitingList();
                    }
                } catch (Exception e) {
                    log.error("Failed to allocate from waiting list after expiring offers", e);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in expireOffers job", e);
        }
    }

    /**
     * Scheduled job to expire check-ins that haven't been completed within the expiry period.
     * Runs every hour (configurable via app.allocation.expiry-check-interval-ms).
     */
    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void expireCheckIns() {
        log.info("Starting scheduled job to expire overdue check-ins");

        try {
            List<RoomAllocation> expiredCheckIns = roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(
                    AllocationStatus.ACCEPTED, LocalDateTime.now());

            if (expiredCheckIns.isEmpty()) {
                log.info("No expired check-ins found");
                return;
            }

            log.info("Found {} expired check-ins to process", expiredCheckIns.size());
            int successCount = 0;
            int failureCount = 0;

            for (RoomAllocation allocation : expiredCheckIns) {
                try {
                    expireSingleCheckIn(allocation);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to expire check-in for allocation {}: {}", allocation.getId(), e.getMessage(), e);
                }
            }

            log.info("Check-in expiry job completed: {} succeeded, {} failed", successCount, failureCount);

            // Trigger waiting list allocation for freed rooms
            if (successCount > 0) {
                try {
                    // Use batch allocation if configured, otherwise fall back to single allocation
                    boolean useBatchOnExpiry = environment.getProperty("app.allocation.batch-on-expiry", Boolean.class, true);
                    if (useBatchOnExpiry) {
                        log.info("Triggering batch allocation after expiring {} check-ins", successCount);
                        batchAllocateFromWaitingList(successCount);
                    } else {
                        allocateNextFromWaitingList();
                    }
                } catch (Exception e) {
                    log.error("Failed to allocate from waiting list after expiring check-ins", e);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in expireCheckIns job", e);
        }
    }

    /**
     * Expires a single offer allocation.
     * Uses programmatic transaction management to ensure per-item isolation.
     */
    private void expireSingleOffer(RoomAllocation allocation) {
        transactionTemplate.execute(status -> {
            try {
                // Re-fetch to ensure latest state and avoid race conditions
                RoomAllocation current = roomAllocationRepository.findByIdWithDetails(allocation.getId())
                        .orElse(null);

                if (current == null) {
                    log.warn("Allocation {} no longer exists, skipping expiry", allocation.getId());
                    return null;
                }

                // Verify transition to EXPIRED is valid (might have been accepted in the meantime)
                if (!current.getStatus().canTransitionTo(AllocationStatus.EXPIRED)) {
                    log.info("Allocation {} status {} cannot transition to EXPIRED, skipping expiry",
                            current.getId(), current.getStatus());
                    return null;
                }

                log.info("Expiring offer for allocation {}, student {}, room {}",
                        current.getId(), current.getStudent().getId(), current.getRoom().getRoomNumber());

                // Update allocation status
                validateStatusTransition(current, AllocationStatus.EXPIRED);
                current.setStatus(AllocationStatus.EXPIRED);
                current.setEndDate(LocalDateTime.now());
                roomAllocationRepository.save(current);

                // Free the room
                updateRoomOccupancy(current.getRoom(), -1);

                // Cancel application
                cancelApplicationAfterOfferExpiry(current);

                // Send notifications
                try {
                    notificationService.sendOfferExpiredNotification(current);
                    userNotificationService.createOfferExpiredNotification(
                            current.getStudent().getAppUser().getId(),
                            current.getRoom().getRoomNumber(),
                            current.getRoom().getBuilding() != null ?
                                    current.getRoom().getBuilding().getName() : "Unknown Building");
                } catch (Exception e) {
                    log.error("Failed to send expiry notifications for allocation {}", current.getId(), e);
                }

                log.info("Successfully expired offer for allocation {}", current.getId());
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("Failed to expire offer for allocation " + allocation.getId(), e);
            }
        });
    }

    /**
     * Expires a single check-in allocation.
     * Uses programmatic transaction management to ensure per-item isolation.
     */
    private void expireSingleCheckIn(RoomAllocation allocation) {
        transactionTemplate.execute(status -> {
            try {
                // Re-fetch to ensure latest state and avoid race conditions
                RoomAllocation current = roomAllocationRepository.findByIdWithDetails(allocation.getId())
                        .orElse(null);

                if (current == null) {
                    log.warn("Allocation {} no longer exists, skipping check-in expiry", allocation.getId());
                    return null;
                }

                // Verify transition to CHECK_IN_EXPIRED is valid (might have been checked in in the meantime)
                if (!current.getStatus().canTransitionTo(AllocationStatus.CHECK_IN_EXPIRED)) {
                    log.info("Allocation {} status {} cannot transition to CHECK_IN_EXPIRED, skipping check-in expiry",
                            current.getId(), current.getStatus());
                    return null;
                }

                log.info("Expiring check-in for allocation {}, student {}, room {}",
                        current.getId(), current.getStudent().getId(), current.getRoom().getRoomNumber());

                // Update allocation status
                validateStatusTransition(current, AllocationStatus.CHECK_IN_EXPIRED);
                current.setStatus(AllocationStatus.CHECK_IN_EXPIRED);
                current.setEndDate(LocalDateTime.now());
                roomAllocationRepository.save(current);

                // Free the room
                updateRoomOccupancy(current.getRoom(), -1);

                // Cancel application
                cancelApplicationAfterCheckInExpiry(current);

                // Send notifications
                try {
                    notificationService.sendCheckInExpiredNotification(current);
                    userNotificationService.createCheckInExpiredNotification(
                            current.getStudent().getAppUser().getId(),
                            current.getRoom().getRoomNumber(),
                            current.getRoom().getBuilding() != null ?
                                    current.getRoom().getBuilding().getName() : "Unknown Building");
                } catch (Exception e) {
                    log.error("Failed to send check-in expiry notifications for allocation {}", current.getId(), e);
                }

                log.info("Successfully expired check-in for allocation {}", current.getId());
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("Failed to expire check-in for allocation " + allocation.getId(), e);
            }
        });
    }

    /**
     * Cancels application after offer expiry.
     * Follows the same pattern as cancelApplicationAfterMaxDeclines but with different messaging.
     */
    private void cancelApplicationAfterOfferExpiry(RoomAllocation allocation) {
        Application application = allocation.getApplication();
        Student student = allocation.getStudent();

        if (application == null) {
            log.warn("No application linked to allocation {}; cannot cancel", allocation.getId());
            return;
        }

        log.info("Cancelling application {} after offer expiry for student {}",
                application.getId(), student.getId());

        // Update application status to CANCELLED
        application.setStatus(ApplicationStatus.CANCELLED);
        String remarks = "Application cancelled: Room offer expired without response within " + offerExpiryDays + " days.";
        application.setRemarks(remarks);
        applicationRepository.save(application);

        // Cancel waiting list entry if exists
        waitingListRepository.findByStudent_IdAndStatus(student.getId(), "WAITING")
                .ifPresent(entry -> {
                    entry.setStatus("CANCELLED");
                    waitingListRepository.save(entry);
                    log.info("Cancelled waiting list entry for student {}", student.getId());
                });
    }

    /**
     * Cancels application after check-in expiry.
     */
    private void cancelApplicationAfterCheckInExpiry(RoomAllocation allocation) {
        Application application = allocation.getApplication();
        Student student = allocation.getStudent();

        if (application == null) {
            log.warn("No application linked to allocation {}; cannot cancel", allocation.getId());
            return;
        }

        log.info("Cancelling application {} after check-in expiry for student {}",
                application.getId(), student.getId());

        // Update application status to CANCELLED
        application.setStatus(ApplicationStatus.CANCELLED);
        String remarks = "Application cancelled: Student failed to check in within " + checkInExpiryDays +
                " days of accepting the offer.";
        application.setRemarks(remarks);
        applicationRepository.save(application);

        // Cancel waiting list entry if exists
        waitingListRepository.findByStudent_IdAndStatus(student.getId(), "WAITING")
                .ifPresent(entry -> {
                    entry.setStatus("CANCELLED");
                    waitingListRepository.save(entry);
                    log.info("Cancelled waiting list entry for student {}", student.getId());
                });
    }

    /**
     * Validates that a status transition is allowed according to the AllocationStatus state machine.
     * Throws InvalidAllocationStatusTransitionException if the transition is not valid.
     *
     * @param allocation the allocation being transitioned
     * @param newStatus the target status
     * @throws com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException if transition is invalid
     */
    private void validateStatusTransition(RoomAllocation allocation, AllocationStatus newStatus) {
        AllocationStatus currentStatus = allocation.getStatus();
        if (currentStatus != null && !currentStatus.canTransitionTo(newStatus)) {
            throw new com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException(
                    currentStatus, newStatus);
        }
    }
}
