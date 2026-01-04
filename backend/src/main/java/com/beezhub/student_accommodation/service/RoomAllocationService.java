package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.AllocationEligibilityResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.RoomAvailabilityResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.specification.RoomAllocationSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAllocationService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final NotificationService notificationService;
    private final WaitingListRepository waitingListRepository;

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
        log.info("Searching room allocations with filters - studentId: {}, roomId: {}, buildingId: {}, fromDate: {}, toDate: {}",
                studentId, roomId, buildingId, fromDate, toDate);
        Specification<RoomAllocation> spec = RoomAllocationSpecification.filterBy(studentId, roomId, buildingId, fromDate, toDate);
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
     *
     * Business Rules:
     * 1. Only APPROVED applications can be allocated rooms
     * 2. Room must have available capacity (not fully occupied)
     * 3. Student cannot have multiple active allocations
     * 4. Application can only be allocated once
     * 5. Room status updates to occupied when allocated
     * 6. TODO: Send notification email to student
     */
    @Transactional
    public RoomAllocationResponse createRoomAllocation(RoomAllocationRequest request) {
        log.info("Creating new room allocation for student: {} to room: {}", request.getStudentId(), request.getRoomId());

        // Validate student exists
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + request.getStudentId()));

        // Validate room exists
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + request.getRoomId()));

        // Business Rule 3: Student cannot have multiple active allocations
        if (roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())) {
            log.error("Student {} already has an active room allocation", student.getId());
            throw new StudentAlreadyAllocatedException(
                    "Student with ID " + student.getId() + " already has an active room allocation");
        }

        // Business Rule 2: Room must have available capacity
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
        allocation.setAllocationDate(request.getAllocationDate() != null ? request.getAllocationDate() : LocalDateTime.now());
        allocation.setEndDate(request.getEndDate());
        allocation.setStatus("ACTIVE");

        // Save allocation
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        // Business Rule 6: Update room occupancy and status
        updateRoomOccupancy(room, 1);

        log.info("Successfully created room allocation with id: {}", saved.getId());

        // Business Rule 5: Send notification email to student
        try {
            log.info("Triggering room allocation notification for student {} to room {}", student.getId(), room.getRoomNumber());
            notificationService.sendRoomAllocationNotification(saved);
        } catch (Exception e) {
            // Log error but don't fail the request - notification will be retried
            log.error("Failed to trigger notification for room allocation {} - notification will be retried by scheduler",
                    saved.getId(), e);
        }

        return roomAllocationMapper.toResponse(saved);
    }

    /**
     * Validates that the application is APPROVED and belongs to the student.
     * Business Rule 1: Only APPROVED applications can be allocated rooms
     * Business Rule 4: Application can only be allocated once
     */
    private Application validateAndGetApplication(Long applicationId, Long studentId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with id: " + applicationId));

        // Check if application belongs to the student
        if (!application.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("Application does not belong to the specified student");
        }

        // Business Rule 1: Only APPROVED applications can be allocated
        if (!"APPROVED".equalsIgnoreCase(String.valueOf(application.getStatus()))) {
            log.error("Application {} is not approved. Current status: {}", applicationId, application.getStatus());
            throw new ApplicationNotApprovedException(
                    "Application must be APPROVED before allocating a room. Current status: " + application.getStatus());
        }

        // Business Rule 4: Application can only be allocated once
        if (roomAllocationRepository.existsByApplicationId(applicationId)) {
            log.error("Application {} has already been allocated a room", applicationId);
            throw new ApplicationAlreadyAllocatedException(
                    "Application with ID " + applicationId + " has already been allocated a room");
        }

        return application;
    }

    /**
     * Validates room has available capacity.
     * Business Rule 2: Room must have available capacity
     */
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

    /**
     * Updates room occupancy and status when allocation is created or removed.
     */
    private void updateRoomOccupancy(Room room, int change) {
        int newOccupancy = (room.getCurrentOccupancy() != null ? room.getCurrentOccupancy() : 0) + change;
        room.setCurrentOccupancy(newOccupancy);

        // Update room status based on occupancy
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
    public RoomAllocationResponse updateRoomAllocation(Long id, RoomAllocationRequest request) {
        log.info("Updating room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        Room oldRoom = allocation.getRoom();
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + request.getStudentId()));

        Room newRoom = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + request.getRoomId()));

        // If room is changing, validate capacity and update occupancy
        boolean roomChanged = false;
        if (!oldRoom.getId().equals(newRoom.getId())) {
            validateRoomCapacity(newRoom);
            updateRoomOccupancy(oldRoom, -1);  // Decrease old room
            updateRoomOccupancy(newRoom, 1);    // Increase new room
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
                // Log error but don't fail the request - notification will be retried
                log.error("Failed to trigger notification for room allocation update {} - notification will be retried by scheduler",
                        updated.getId(), e);
            }
        }

        return roomAllocationMapper.toResponse(updated);
    }

    @Transactional
    public void deleteRoomAllocation(Long id) {
        log.info("Deleting room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        Room room = allocation.getRoom();

        // Update room occupancy
        updateRoomOccupancy(room, -1);

        roomAllocationRepository.delete(allocation);
        log.info("Successfully deleted room allocation with id: {}", id);

        try {
            allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("Failed to allocate next from waiting list after deletion {}", id, e);
        }
    }

    /**
     * Ends an active allocation (soft delete approach).
     */
    @Transactional
    public RoomAllocationResponse endAllocation(Long id) {
        log.info("Ending room allocation with id: {}", id);
        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));

        if (!"ACTIVE".equals(allocation.getStatus())) {
            throw new IllegalStateException("Can only end ACTIVE allocations");
        }

        allocation.setStatus("COMPLETED");
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

    /**
     * Gets the active allocation for a specific student.
     */
    public RoomAllocationResponse getActiveAllocationByStudentId(Long studentId) {
        log.info("Fetching active allocation for student: {}", studentId);
        RoomAllocation allocation = roomAllocationRepository.findActiveAllocationByStudentId(studentId)
                .orElseThrow(() -> new RoomAllocationNotFoundException(
                        "No active allocation found for student with id: " + studentId));
        return roomAllocationMapper.toResponse(allocation);
    }

    /**
     * Allocates a room based on an approved application.
     * This is the primary method for processing approved applications.
     */
    @Transactional
    public RoomAllocationResponse allocateRoomByApplication(Long applicationId, Long roomId) {
        log.info("Allocating room {} for application: {}", roomId, applicationId);

        // Get and validate application
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with id: " + applicationId));

        // Validate application is APPROVED
        if (!"APPROVED".equalsIgnoreCase(String.valueOf(application.getStatus()))) {
            throw new ApplicationNotApprovedException(
                    "Application must be APPROVED before allocating a room. Current status: " + application.getStatus());
        }

        // Check if application already allocated
        if (roomAllocationRepository.existsByApplicationId(applicationId)) {
            throw new ApplicationAlreadyAllocatedException(
                    "Application with ID " + applicationId + " has already been allocated a room");
        }

        // Create allocation request
        RoomAllocationRequest request = new RoomAllocationRequest();
        request.setStudentId(application.getStudent().getId());
        request.setRoomId(roomId);
        request.setApplicationId(applicationId);

        return createRoomAllocation(request);
    }

    /**
     * Checks if a room has available capacity.
     */
    public boolean isRoomAvailable(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        boolean available = activeAllocations < room.getCapacity();

        log.info("Room {} availability check: {} (Current: {}, Capacity: {})",
                room.getRoomNumber(), available, activeAllocations, room.getCapacity());

        return available;
    }

    /**
     * Gets available capacity for a room.
     */
    public int getRoomAvailableCapacity(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + roomId));

        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(roomId);
        int available = room.getCapacity() - (int) activeAllocations;

        log.info("Room {} has {} available spots (Capacity: {}, Occupied: {})",
                room.getRoomNumber(), available, room.getCapacity(), activeAllocations);

        return Math.max(0, available);
    }

    /**
     * Validates if a student is eligible for room allocation.
     * Returns validation result with reasons if not eligible.
     */
    public AllocationEligibilityResponse checkStudentEligibility(Long studentId) {
        log.info("Checking allocation eligibility for student: {}", studentId);

        // Check if student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        AllocationEligibilityResponse response = new AllocationEligibilityResponse();
        response.setStudentId(studentId);
        response.setEligible(true);

        // Check if student already has active allocation
        boolean hasActiveAllocation = roomAllocationRepository.existsActiveAllocationByStudentId(studentId);
        response.setHasActiveAllocation(hasActiveAllocation);

        if (hasActiveAllocation) {
            response.setEligible(false);
            response.setReason("Student already has an active room allocation");
            log.info("Student {} is not eligible: already has active allocation", studentId);
            return response;
        }

        // Check if student has approved application
        boolean hasApprovedApplication = applicationRepository.existsByStudent_IdAndStatus(
            studentId,
            com.beezhub.student_accommodation.model.enums.ApplicationStatus.APPROVED
        );
        response.setHasApprovedApplication(hasApprovedApplication);

        response.setReason("Student is eligible for room allocation");
        log.info("Student {} is eligible for room allocation", studentId);
        return response;
    }

    /**
     * Gets all allocations by status.
     */
    public PageResponse<RoomAllocationResponse> getAllocationsByStatus(String status, Pageable pageable) {
        log.info("Fetching allocations with status: {}", status);

        // Simple specification for status filtering
        Specification<RoomAllocation> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);

        Page<RoomAllocation> allocations = roomAllocationRepository.findAll(spec, pageable);
        Page<RoomAllocationResponse> responsePage = allocations.map(roomAllocationMapper::toResponse);

        log.info("Found {} allocations with status: {}", allocations.getNumberOfElements(), status);
        return PageMapper.toPageResponse(responsePage);
    }

    /**
     * Gets detailed room availability information.
     */
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

    public Room findAvailableRoom() {
        Optional<Room> room = roomRepository.findFirstByStatusOrderByBuilding_IdAscRoomNumberAsc("AVAILABLE");
        room.ifPresent(r -> log.info("Found available room {}", r.getRoomNumber()));
        return room.orElse(null);
    }

    /**
     * Attempts to allocate next waiting student when a room frees up.
     */
    @Transactional
    public void allocateNextFromWaitingList() {
        Room room = findAvailableRoom();
        if (room == null) {
            log.info("No available rooms to allocate waiting list");
            return;
        }
        WaitingListEntry entry = waitingListRepository.findFirstByStatusOrderByCreatedAtAsc("WAITING")
                .orElse(null);
        if (entry == null) {
            log.info("No waiting list entries to allocate");
            return;
        }
        RoomAllocationRequest request = new RoomAllocationRequest();
        request.setStudentId(entry.getStudent().getId());
        request.setRoomId(room.getId());
        if (entry.getApplication() != null) {
            request.setApplicationId(entry.getApplication().getId());
        }
        createRoomAllocation(request);
        entry.setStatus("ALLOCATED");
        waitingListRepository.save(entry);
        log.info("Allocated waiting student {} to room {}", entry.getStudent().getId(), room.getRoomNumber());
    }
}
