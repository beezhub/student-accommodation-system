package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.AllocationAuditLogResponse;
import com.beezhub.student_accommodation.model.entity.AllocationAuditLog;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.AllocationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationAuditService {

    private final AllocationAuditLogRepository allocationAuditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditAction(RoomAllocation allocation, AllocationAuditActionType actionType,
                               AppUser admin, String reason, Room oldRoom, Room newRoom) {
        logAuditActionWithConstraint(allocation, actionType, admin, reason, oldRoom, newRoom, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditActionWithConstraint(RoomAllocation allocation, AllocationAuditActionType actionType,
                                             AppUser admin, String reason, Room oldRoom, Room newRoom,
                                             String constraintBypassed) {
        try {
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
        } catch (Exception e) {
            log.error("Failed to save audit log for action {}", actionType, e);
            // Audit failure should not roll back transaction
        }
    }

    /**
     * Records a status transition audit entry within the caller's transaction.
     * Uses REQUIRED propagation so the audit entry is committed with the
     * allocation status change — consistent and safe for new allocations.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logStatusChange(RoomAllocation allocation, AllocationStatus previousStatus,
                                AllocationStatus newStatus) {
        try {
            AllocationAuditLog auditLog = AllocationAuditLog.builder()
                    .allocation(allocation)
                    .actionType(AllocationAuditActionType.STATUS_CHANGE)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .build();
            allocationAuditLogRepository.save(auditLog);
            log.info("Status change audit: {} -> {} for allocation {}",
                    previousStatus, newStatus, allocation != null ? allocation.getId() : "N/A");
        } catch (Exception e) {
            log.error("Failed to save status change audit for allocation {}",
                    allocation != null ? allocation.getId() : "N/A", e);
        }
    }

    /**
     * Returns the full audit history for a given allocation, ordered by creation time (oldest first).
     */
    @Transactional(readOnly = true)
    public List<AllocationAuditLogResponse> getHistory(Long allocationId) {
        List<AllocationAuditLog> logs = allocationAuditLogRepository
                .findByAllocation_IdOrderByCreatedAtDesc(allocationId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        return logs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ==================== Private Helpers ====================

    private AllocationAuditLogResponse toResponse(AllocationAuditLog entry) {
        return AllocationAuditLogResponse.builder()
                .id(entry.getId())
                .actionType(entry.getActionType())
                .reason(entry.getReason())
                .constraintBypassed(entry.getConstraintBypassed())
                .createdAt(entry.getCreatedAt())
                .allocationId(entry.getAllocation() != null ? entry.getAllocation().getId() : null)
                .adminUserId(entry.getAdminUser() != null ? entry.getAdminUser().getId() : null)
                .adminName(entry.getAdminUser() != null
                        ? entry.getAdminUser().getFirstName() + " " + entry.getAdminUser().getLastName() : null)
                .adminEmail(entry.getAdminUser() != null ? entry.getAdminUser().getEmail() : null)
                .oldRoomId(entry.getOldRoom() != null ? entry.getOldRoom().getId() : null)
                .oldRoomNumber(entry.getOldRoom() != null ? entry.getOldRoom().getRoomNumber() : null)
                .newRoomId(entry.getNewRoom() != null ? entry.getNewRoom().getId() : null)
                .newRoomNumber(entry.getNewRoom() != null ? entry.getNewRoom().getRoomNumber() : null)
                .previousStatus(entry.getPreviousStatus() != null ? entry.getPreviousStatus().name() : null)
                .newStatus(entry.getNewStatus() != null ? entry.getNewStatus().name() : null)
                .build();
    }
}
