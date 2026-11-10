package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException;
import com.beezhub.student_accommodation.exceptions.AllocationConstraintViolationException;
import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import com.beezhub.student_accommodation.model.event.AllocationMovedEvent;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationMoveService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomRepository roomRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final LegacyScoringStrategy allocationScoringService;
    private final AppUserRepository appUserRepository;
    private final AllocationAuditService allocationAuditService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserNotificationService userNotificationService;
    private final RoomOccupancyManager roomOccupancyManager;
    private final TenantOwnershipGuard tenantOwnershipGuard;
    private final StayCapacityGuard stayCapacityGuard;

    @Transactional
    public RoomAllocationResponse moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId) {
        log.info("Moving allocation {} to room {} by admin {}: {}", allocationId, newRoomId, adminId, reason);

        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(allocationId)
                .orElseThrow(() -> new RoomAllocationNotFoundException(
                        "Room allocation not found with id: " + allocationId));

        validateMoveEligibility(allocation);

        Room oldRoom = allocation.getRoom();
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + newRoomId));
        tenantOwnershipGuard.assertOwned(newRoom, "Room", newRoomId);

        // Free for the rest of the student's planned stay (GH-496), and free in fact today: a
        // resident who has not checked out still sleeps there.
        stayCapacityGuard.assertBedFree(newRoom, StayPeriod.of(allocation));
        stayCapacityGuard.assertBedPhysicallyFree(newRoom, allocation.getId());
        validateHardConstraints(allocation.getStudent(), newRoom);

        allocation.setRoom(newRoom);
        RoomAllocation updated = roomAllocationRepository.save(allocation);

        // Both rooms, after the move. Syncing before it would count the allocation against the
        // room it is leaving and not yet against the one it is joining (GH-136).
        roomOccupancyManager.syncOccupancy(oldRoom);
        roomOccupancyManager.syncOccupancy(newRoom);

        AppUser admin = appUserRepository.findById(adminId).orElse(null);
        allocationAuditService.logAuditAction(allocation, AllocationAuditActionType.MOVE, admin, reason, oldRoom,
                newRoom);

        sendMoveNotification(allocation, oldRoom, newRoom, reason);

        log.info("Moved student {} from room {} to room {}", allocation.getStudent().getId(), oldRoom.getRoomNumber(),
                newRoom.getRoomNumber());
        return roomAllocationMapper.toResponse(updated);
    }

    private void validateMoveEligibility(RoomAllocation allocation) {
        if (!"ACTIVE".equals(allocation.getStatus().name())) {
            throw new InvalidAllocationStatusTransitionException("Can only move ACTIVE allocations");
        }
    }

    private void validateHardConstraints(Student student, Room room) {
        if (!allocationScoringService.validateHardConstraints(student, room, false)) {
            throw new AllocationConstraintViolationException(
                    "New room does not meet hard constraints for student");
        }
    }

    private void sendMoveNotification(RoomAllocation allocation, Room oldRoom, Room newRoom, String reason) {
        try {
            eventPublisher.publishEvent(new AllocationMovedEvent(allocation, oldRoom, LocalDateTime.now(), reason));
            userNotificationService.createRoomMoveNotification(
                    allocation.getStudent().getAppUser().getId(),
                    oldRoom.getRoomNumber(),
                    newRoom.getRoomNumber(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send move notification", e);
        }
    }
}
