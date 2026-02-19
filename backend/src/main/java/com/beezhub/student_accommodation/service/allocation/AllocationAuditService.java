package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.AllocationAuditLog;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.repository.AllocationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
}
