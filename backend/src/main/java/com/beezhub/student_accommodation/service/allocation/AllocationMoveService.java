package com.beezhub.student_accommodation.service.allocation;

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
import com.beezhub.student_accommodation.service.AllocationScoringService;
import com.beezhub.student_accommodation.service.UserNotificationService;
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
    private final AllocationScoringService allocationScoringService;
    private final AppUserRepository appUserRepository;
    private final AllocationAuditService allocationAuditService;
    private final UserNotificationService userNotificationService;
    private final RoomOccupancyManager roomOccupancyManager;

    @Transactional
    public RoomAllocationResponse moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId) {
        log.info("Moving allocation {} to room {} by admin {}: {}", allocationId, newRoomId, adminId, reason);

        RoomAllocation allocation = roomAllocationRepository.findByIdWithDetails(allocationId)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + allocationId));
        
        validateMoveEligibility(allocation);

        Room oldRoom = allocation.getRoom();
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + newRoomId));

        validateRoomCapacity(newRoom);
        validateHardConstraints(allocation.getStudent(), newRoom);

        roomOccupancyManager.updateOccupancy(oldRoom, -1);
        roomOccupancyManager.updateOccupancy(newRoom, 1);

        allocation.setRoom(newRoom);
        RoomAllocation updated = roomAllocationRepository.save(allocation);

        AppUser admin = appUserRepository.findById(adminId).orElse(null);
        allocationAuditService.logAuditAction(allocation, AllocationAuditActionType.MOVE, admin, reason, oldRoom, newRoom);

        sendMoveNotification(allocation.getStudent(), oldRoom, newRoom, reason);

        log.info("Moved student {} from room {} to room {}", allocation.getStudent().getId(), oldRoom.getRoomNumber(), newRoom.getRoomNumber());
        return roomAllocationMapper.toResponse(updated);
    }

    private void validateMoveEligibility(RoomAllocation allocation) {
        if (!"ACTIVE".equals(allocation.getStatus().name())) {
            throw new RuntimeException("Can only move ACTIVE allocations");
        }
    }

    private void validateRoomCapacity(Room room) {
        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
        if (activeAllocations >= room.getCapacity()) {
            log.error("Room {} is at full capacity. Current: {}, Max: {}", room.getRoomNumber(), activeAllocations, room.getCapacity());
            throw new RoomCapacityExceededException(
                    "Room " + room.getRoomNumber() + " is at full capacity (" + room.getCapacity() + " occupants)");
        }
    }

    private void validateHardConstraints(Student student, Room room) {
        if (!allocationScoringService.validateHardConstraints(student, room, false)) {
            throw new RuntimeException("New room does not meet hard constraints for student");
        }
    }

    private void sendMoveNotification(Student student, Room oldRoom, Room newRoom, String reason) {
        try {
            userNotificationService.createRoomMoveNotification(
                    student.getAppUser().getId(),
                    oldRoom.getRoomNumber(),
                    newRoom.getRoomNumber(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send move notification", e);
        }
    }
}
