package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.event.AllocationCreatedEvent;
import com.beezhub.student_accommodation.model.event.AllocationAcceptedEvent;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AllocationLifecycleService {

    @Value("${app.allocation.offer-expiry-days:7}")
    private int offerExpiryDays;

    @Value("${app.allocation.check-in-expiry-days:7}")
    private int checkInExpiryDays;

    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final WaitingListService waitingListService;
    private final RoomOccupancyManager roomOccupancyManager;
    private final AllocationMoveService allocationMoveService;
    private final AllocationBatchService allocationBatchService;
    private final AllocationAuditService allocationAuditService;
    private final MeterRegistry meterRegistry;

    public AllocationLifecycleService(
            RoomAllocationRepository roomAllocationRepository,
            StudentRepository studentRepository,
            RoomRepository roomRepository,
            ApplicationRepository applicationRepository,
            RoomAllocationMapper roomAllocationMapper,
            ApplicationEventPublisher eventPublisher,
            WaitingListService waitingListService,
            RoomOccupancyManager roomOccupancyManager,
            AllocationMoveService allocationMoveService,
            @Lazy AllocationBatchService allocationBatchService,
            AllocationAuditService allocationAuditService,
            MeterRegistry meterRegistry) {
        this.roomAllocationRepository = roomAllocationRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.applicationRepository = applicationRepository;
        this.roomAllocationMapper = roomAllocationMapper;
        this.eventPublisher = eventPublisher;
        this.waitingListService = waitingListService;
        this.roomOccupancyManager = roomOccupancyManager;
        this.allocationMoveService = allocationMoveService;
        this.allocationBatchService = allocationBatchService;
        this.allocationAuditService = allocationAuditService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Creates a new room allocation with comprehensive business rule validation.
     */
    @Transactional
    public RoomAllocationResponse createRoomAllocation(RoomAllocationRequest request) {
        log.info("Creating new room allocation for student: {} to room: {}", request.getStudentId(),
                request.getRoomId());

        Student student = getStudent(request.getStudentId());
        Room room = getRoom(request.getRoomId());

        validateAllocationPrerequisites(student, room);

        Application application = null;
        if (request.getApplicationId() != null) {
            application = validateAndGetApplication(request.getApplicationId(), student.getId());
        }

        RoomAllocation allocation = buildNewAllocation(request, student, room, application);
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        allocationAuditService.logStatusChange(saved, null, AllocationStatus.OFFERED);

        waitingListService.updateStatusToAllocated(student.getId());
        roomOccupancyManager.updateOccupancy(room, 1);

        sendAllocationNotification(saved);

        meterRegistry.counter("sas.allocations.created").increment();

        log.info("Successfully created room allocation with id: {}", saved.getId());
        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse acceptOffer(Long id, AcceptOfferRequest request) {
        log.info("Accepting room allocation offer with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        validateOfferAcceptance(allocation, request);

        AllocationStatus previousStatus = allocation.getStatus();
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setExpectedCheckInDate(request.getExpectedCheckInDate());
        allocation.setCheckInExpiryDate(LocalDateTime.now().plusDays(checkInExpiryDays));

        updateApplicationStatus(allocation, com.beezhub.student_accommodation.model.enums.ApplicationStatus.ALLOCATED);
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        allocationAuditService.logStatusChange(saved, previousStatus, AllocationStatus.ACCEPTED);

        waitingListService.updateStatusToAllocated(saved.getStudent().getId());
        eventPublisher.publishEvent(new AllocationAcceptedEvent(saved));

        meterRegistry.counter("sas.allocations.accepted").increment();

        log.info("Successfully accepted room allocation with id: {}", id);
        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse declineOffer(Long id, DeclineOfferRequest request) {
        log.info("Declining room allocation offer with id: {} for reason: {}", id, request.getDeclineReason());
        RoomAllocation allocation = getAllocation(id);

        long previousDeclineCount = getPreviousDeclineCount(allocation);

        AllocationStatus previousStatus = allocation.getStatus();
        AllocationStatusValidator.validate(allocation, AllocationStatus.DECLINED);

        roomOccupancyManager.updateOccupancy(allocation.getRoom(), -1);

        allocation.setStatus(AllocationStatus.DECLINED);
        allocation.setEndDate(LocalDateTime.now());
        allocation.setDeclineReason(request.getDeclineReason());
        allocation.setDeclineReasonText(request.getDeclineReasonText());

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        allocationAuditService.logStatusChange(saved, previousStatus, AllocationStatus.DECLINED);
        log.info("Successfully declined room allocation with id: {}. Previous decline count: {}", id,
                previousDeclineCount);

        handleDeclineConsequences(saved, previousDeclineCount);

        try {
            allocationBatchService.allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("Failed to allocate next from waiting list after decline {}", id, e);
        }

        meterRegistry.counter("sas.allocations.declined").increment();

        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public void deleteRoomAllocation(Long id) {
        log.info("Cancelling (deleting) room allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        AllocationStatus previousStatus = allocation.getStatus();
        AllocationStatusValidator.validate(allocation, AllocationStatus.CANCELLED);
        allocation.setStatus(AllocationStatus.CANCELLED);
        allocation.setEndDate(LocalDateTime.now());

        roomOccupancyManager.updateOccupancy(allocation.getRoom(), -1);
        RoomAllocation saved = roomAllocationRepository.save(allocation);
        allocationAuditService.logStatusChange(saved, previousStatus, AllocationStatus.CANCELLED);
        log.info("Successfully cancelled room allocation with id: {}", id);

        try {
            allocationBatchService.allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("Failed to allocate next from waiting list after deletion {}", id, e);
        }
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

    @Transactional
    public RoomAllocationResponse moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId) {
        return allocationMoveService.moveStudent(allocationId, newRoomId, reason, adminId);
    }

    @Transactional
    public RoomAllocationResponse updateRoomAllocation(Long id, RoomAllocationRequest request) {
        log.info("Updating room allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        Room oldRoom = allocation.getRoom();
        Student student = getStudent(request.getStudentId());
        Room newRoom = getRoom(request.getRoomId());

        boolean roomChanged = false;
        if (!oldRoom.getId().equals(newRoom.getId())) {
            validateRoomCapacity(newRoom);
            roomOccupancyManager.updateOccupancy(oldRoom, -1);
            roomOccupancyManager.updateOccupancy(newRoom, 1);
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

        if (roomChanged) {
            sendAllocationNotification(updated);
        }

        return roomAllocationMapper.toResponse(updated);
    }

    @Transactional
    public MessageResponse enqueueWaitingList(Long studentId, Long applicationId) {
        return waitingListService.enqueueWaitingList(studentId, applicationId);
    }

    // ==================== Private Helpers ====================

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));
    }

    private Room getRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));
    }

    private RoomAllocation getAllocation(Long id) {
        return roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));
    }

    private void validateAllocationPrerequisites(Student student, Room room) {
        if (roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())) {
            log.error("Student {} already has an active room allocation", student.getId());
            throw new StudentAlreadyAllocatedException(
                    "Student with ID " + student.getId() + " already has an active room allocation");
        }
        validateRoomCapacity(room);
    }

    private void validateRoomCapacity(Room room) {
        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
        if (activeAllocations >= room.getCapacity()) {
            log.error("Room {} is at full capacity. Current: {}, Max: {}", room.getRoomNumber(), activeAllocations,
                    room.getCapacity());
            throw new RoomCapacityExceededException(
                    "Room " + room.getRoomNumber() + " is at full capacity (" + room.getCapacity() + " occupants)");
        }
    }

    private Application validateAndGetApplication(Long applicationId, Long studentId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with id: " + applicationId));

        if (!application.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("Application does not belong to the specified student");
        }

        ApplicationStatus appStatus = application.getStatus();
        if (appStatus != ApplicationStatus.APPROVED && appStatus != ApplicationStatus.AWAITING_ALLOCATION) {
            throw new ApplicationNotApprovedException(
                    "Application must be APPROVED or AWAITING_ALLOCATION before allocating a room. Current status: "
                            + appStatus);
        }

        if (roomAllocationRepository.existsByApplicationId(applicationId)) {
            throw new ApplicationAlreadyAllocatedException(
                    "Application with ID " + applicationId + " has already been allocated a room");
        }
        return application;
    }

    private RoomAllocation buildNewAllocation(RoomAllocationRequest request, Student student, Room room,
            Application application) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);
        allocation.setTenantId(room.getTenantId());
        allocation.setAllocationDate(
                request.getAllocationDate() != null ? request.getAllocationDate() : LocalDateTime.now());
        allocation.setEndDate(request.getEndDate());
        AllocationStatusValidator.validate(allocation, AllocationStatus.OFFERED);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setOfferExpiryDate(LocalDateTime.now().plusDays(offerExpiryDays));
        return allocation;
    }

    private void sendAllocationNotification(RoomAllocation allocation) {
        try {
            log.info("Triggering room allocation notification for student {} to room {}",
                    allocation.getStudent().getId(), allocation.getRoom().getRoomNumber());
            eventPublisher.publishEvent(new AllocationCreatedEvent(allocation));
        } catch (Exception e) {
            log.error(
                    "Failed to trigger notification for room allocation {} - notification will be retried by scheduler",
                    allocation.getId(), e);
        }
    }

    private void validateOfferAcceptance(RoomAllocation allocation, AcceptOfferRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxDate = now.plusDays(checkInExpiryDays);
        if (request.getExpectedCheckInDate().isBefore(now.minusHours(1))
                || request.getExpectedCheckInDate().isAfter(maxDate)) {
            throw new IllegalArgumentException("Expected check-in date must be within " + checkInExpiryDays + " days from now");
        }
        if (allocation.getStatus() != AllocationStatus.OFFERED) {
            throw new IllegalStateException(
                    "Can only accept OFFERED allocations, current status: " + allocation.getStatus());
        }
        AllocationStatusValidator.validate(allocation, AllocationStatus.ACCEPTED);
    }

    private void updateApplicationStatus(RoomAllocation allocation,
            com.beezhub.student_accommodation.model.enums.ApplicationStatus status) {
        if (allocation.getApplication() != null) {
            allocation.getApplication().setStatus(status);
            applicationRepository.save(allocation.getApplication());
        }
    }

    private long getPreviousDeclineCount(RoomAllocation allocation) {
        if (allocation.getApplication() != null) {
            return roomAllocationRepository
                    .countDeclinedAllocationsByApplicationId(allocation.getApplication().getId());
        }
        return 0;
    }

    private void handleDeclineConsequences(RoomAllocation allocation, long previousDeclineCount) {
        waitingListService.reQueueStudent(allocation, (int) previousDeclineCount + 1);
    }
}
