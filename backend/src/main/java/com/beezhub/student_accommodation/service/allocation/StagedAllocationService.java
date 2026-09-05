package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.mapper.StagedAllocationMapper;
import com.beezhub.student_accommodation.model.dto.BatchAllocationProposalResponse;
import com.beezhub.student_accommodation.model.dto.BatchAllocationRequest;
import com.beezhub.student_accommodation.model.dto.BatchConfirmationResult;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.StagedAllocationResponse;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.event.AllocationCreatedEvent;
import com.beezhub.student_accommodation.model.event.StagedAllocationConfirmedEvent;
import com.beezhub.student_accommodation.model.event.StagedAllocationProposedEvent;
import com.beezhub.student_accommodation.model.event.StagedAllocationRejectedEvent;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StagedAllocationService {

    private final StagedAllocationRepository stagedAllocationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ApplicationRepository applicationRepository;
    private final AppUserRepository appUserRepository;
    private final BuildingRepository buildingRepository;

    private final LegacyScoringStrategy allocationScoringService;
    private final AllocationAuditService allocationAuditService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserNotificationService userNotificationService;

    private final StagedAllocationMapper stagedAllocationMapper;
    private final RoomAllocationMapper roomAllocationMapper;

    @Value("${app.allocation.offer-expiry-days:7}")
    private int offerExpiryDays;
    private final WaitingListService waitingListService;
    private final RoomOccupancyManager roomOccupancyManager;
    private final com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    @Transactional
    public RoomAllocationResponse confirmAllocation(Long stagedAllocationId, Long adminId) {
        log.info("Confirming allocation {} by admin {}", stagedAllocationId, adminId);
        StagedAllocation staged = getStagedAllocation(stagedAllocationId);
        validateStagedStatus(staged, StagedAllocationStatus.PROPOSED);
        validateRoomCapacity(staged.getRoom());

        RoomAllocation allocation = createConfirmedAllocation(staged);
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        updateStagedStatus(staged, StagedAllocationStatus.CONFIRMED);
        roomOccupancyManager.updateOccupancy(staged.getRoom(), 1);

        logConfirmationAudit(saved, staged, adminId);
        sendConfirmationNotification(saved);
        eventPublisher.publishEvent(new StagedAllocationConfirmedEvent(staged, saved));

        log.info("Confirmed allocation {} for student {} to room {}",
                saved.getId(), staged.getStudent().getId(), staged.getRoom().getRoomNumber());
        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public void rejectAllocation(Long stagedAllocationId, String reason, Long adminId) {
        log.info("Rejecting allocation {} by admin {}: {}", stagedAllocationId, adminId, reason);
        StagedAllocation staged = getStagedAllocation(stagedAllocationId);
        validateStagedStatus(staged, StagedAllocationStatus.PROPOSED);

        updateStagedStatus(staged, StagedAllocationStatus.REJECTED);
        sendRejectionNotification(staged, reason);
        eventPublisher.publishEvent(new StagedAllocationRejectedEvent(staged, reason));
    }

    @Transactional
    public StagedAllocationResponse overrideConstraints(Long studentId, Long roomId, String reason,
            Long adminId, List<String> constraintsBypassed) {
        log.info("Override allocation for student {} to room {} by admin {}: {}", studentId, roomId, adminId, reason);
        Student student = getStudent(studentId);
        Room room = getRoom(roomId);
        validateRoomCapacity(room);

        Application application = getAwaitingApplication(studentId);
        AppUser admin = getAdmin(adminId);

        StagedAllocation staged = createOverrideProposal(student, room, application, admin, constraintsBypassed);
        StagedAllocation saved = stagedAllocationRepository.save(staged);

        allocationAuditService.logAuditActionWithConstraint(null, AllocationAuditActionType.OVERRIDE, admin, reason,
                null, room, String.join(", ", constraintsBypassed));
        sendProposalNotification(student, room);
        eventPublisher.publishEvent(new StagedAllocationProposedEvent(saved));

        log.info("Created override proposal for student {} to room {}", studentId, room.getRoomNumber());
        return stagedAllocationMapper.toResponse(saved);
    }

    @Transactional
    public BatchAllocationProposalResponse proposeAllocation(BatchAllocationRequest request, Long adminId) {
        AppUser admin = getAdmin(adminId);
        Building preferredBuilding = getPreferredBuilding(request.getPreferredBuildingId());

        BatchAllocationProposalResponse response = initializeBatchResponse(request, adminId, admin);
        processBatchProposals(request, admin, preferredBuilding, response);

        log.info("Batch proposal completed: {} successful, {} failed", response.getSuccessfulProposals(),
                response.getFailedProposals());
        return response;
    }

    @Transactional
    public void confirmBatchAllocations(String batchId, Long adminId) {
        log.info("Confirming batch allocations for batch {} by admin {}", batchId, adminId);
        List<StagedAllocation> proposals = stagedAllocationRepository.findByBatchIdAndStatus(batchId,
                StagedAllocationStatus.PROPOSED);

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
                log.error("Failed to confirm allocation {} in batch {}: {}", staged.getId(), batchId, e.getMessage());
                failed++;
            }
        }
        log.info("Batch {} confirmation complete: {} confirmed, {} failed", batchId, confirmed, failed);
    }

    @Transactional
    public BatchConfirmationResult confirmBatchAllocationsWithResult(String batchId, Long adminId) {
        log.info("Confirming batch allocations with result for batch {} by admin {}", batchId, adminId);
        List<StagedAllocation> proposals = stagedAllocationRepository.findByBatchIdAndStatus(batchId,
                StagedAllocationStatus.PROPOSED);

        List<Long> confirmedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int attempted = proposals.size();

        for (StagedAllocation staged : proposals) {
            try {
                RoomAllocationResponse response = confirmAllocation(staged.getId(), adminId);
                confirmedIds.add(response.getId());
            } catch (Exception e) {
                log.error("Failed to confirm allocation {} in batch {}: {}", staged.getId(), batchId, e.getMessage());
                errors.add("Staged allocation " + staged.getId() + ": " + e.getMessage());
            }
        }

        int successCount = confirmedIds.size();
        int failureCount = errors.size();
        log.info("Batch {} result: {}/{} confirmed", batchId, successCount, attempted);

        return BatchConfirmationResult.builder()
                .success(failureCount == 0)
                .totalAttempted(attempted)
                .successCount(successCount)
                .failureCount(failureCount)
                .errors(errors.isEmpty() ? null : errors)
                .confirmedAllocationIds(confirmedIds.isEmpty() ? null : confirmedIds)
                .processedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Private Helpers ====================

    private StagedAllocation getStagedAllocation(Long id) {
        StagedAllocation staged = stagedAllocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staged allocation not found: " + id));
        tenantOwnershipGuard.assertOwned(staged, "Staged allocation", id);
        return staged;
    }

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found: " + id));
    }

    private Room getRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + id));
        tenantOwnershipGuard.assertOwned(room, "Room", id);
        return room;
    }

    private AppUser getAdmin(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + id));
    }

    private void validateStagedStatus(StagedAllocation staged, StagedAllocationStatus expected) {
        if (staged.getStatus() != expected) {
            throw new RuntimeException("Invalid status: " + staged.getStatus() + ", expected: " + expected);
        }
    }

    private void validateRoomCapacity(Room room) {
        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
        if (activeAllocations >= room.getCapacity()) {
            throw new RuntimeException("Room " + room.getRoomNumber() + " is at full capacity");
        }
    }

    private RoomAllocation createConfirmedAllocation(StagedAllocation staged) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(staged.getStudent());
        allocation.setRoom(staged.getRoom());
        allocation.setApplication(staged.getApplication());
        allocation.setAllocationDate(LocalDateTime.now());
        AllocationStatusValidator.validate(allocation, AllocationStatus.OFFERED);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setOfferExpiryDate(LocalDateTime.now().plusDays(offerExpiryDays));
        allocation.setAllocationMethod(AllocationMethod.AUTO);
        return allocation;
    }

    private void updateStagedStatus(StagedAllocation staged, StagedAllocationStatus status) {
        staged.setStatus(status);
        if (status == StagedAllocationStatus.CONFIRMED) {
            staged.setConfirmedAt(LocalDateTime.now());
        }
        stagedAllocationRepository.save(staged);
    }

    private void logConfirmationAudit(RoomAllocation saved, StagedAllocation staged, Long adminId) {
        AppUser admin = appUserRepository.findById(adminId).orElse(null);
        // Use in-current-transaction variant: saved allocation is not yet committed, so REQUIRES_NEW
        // would open a separate transaction that cannot see it, causing an FK violation.
        allocationAuditService.logAuditActionInCurrentTransaction(saved, AllocationAuditActionType.FORCE_ALLOCATE,
                admin, "Confirmed from staged allocation batch: " + staged.getBatchId(), null, staged.getRoom());
    }

    private void sendConfirmationNotification(RoomAllocation saved) {
        try {
            eventPublisher.publishEvent(new AllocationCreatedEvent(saved));
            userNotificationService.createAllocationConfirmationNotification(
                    saved.getStudent().getAppUser().getId(),
                    saved.getRoom().getRoomNumber(),
                    saved.getRoom().getBuilding().getName());
        } catch (Exception e) {
            log.error("Failed to send confirmation notification", e);
        }
    }

    private void sendRejectionNotification(StagedAllocation staged, String reason) {
        try {
            userNotificationService.createAllocationRejectionNotification(
                    staged.getStudent().getAppUser().getId(),
                    staged.getRoom().getRoomNumber(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send rejection notification", e);
        }
    }

    private Application getAwaitingApplication(Long studentId) {
        return applicationRepository.findByStudent_Id(studentId).stream()
                .filter(app -> app.getStatus() == ApplicationStatus.AWAITING_ALLOCATION)
                .findFirst()
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "No application awaiting allocation found for student ID: " + studentId));
    }

    private StagedAllocation createOverrideProposal(Student student, Room room, Application application, AppUser admin,
            List<String> constraints) {
        return StagedAllocation.builder()
                .student(student)
                .room(room)
                .application(application)
                .status(StagedAllocationStatus.PROPOSED)
                .score(allocationScoringService.scoreSingleRoom(student, application, room).getTotalScore())
                .warnings(stagedAllocationMapper
                        .serializeWarnings(List.of("CONSTRAINT_OVERRIDE: " + String.join(", ", constraints))))
                .proposedBy(admin)
                .batchId("OVERRIDE-" + System.currentTimeMillis())
                .build();
    }

    private void sendProposalNotification(Student student, Room room) {
        try {
            userNotificationService.createAllocationProposalNotification(
                    student.getAppUser().getId(), room.getRoomNumber(), room.getBuilding().getName());
        } catch (Exception e) {
            log.error("Failed to send proposal notification", e);
        }
    }

    private Building getPreferredBuilding(Long buildingId) {
        if (buildingId != null) {
            return buildingRepository.findById(buildingId).orElse(null);
        }
        return null;
    }

    private BatchAllocationProposalResponse initializeBatchResponse(BatchAllocationRequest request, Long adminId,
            AppUser admin) {
        return BatchAllocationProposalResponse.builder()
                .batchId(request.getBatchId())
                .totalStudents(request.getStudentIds().size())
                .createdAt(LocalDateTime.now())
                .createdById(adminId)
                .createdByName(admin.getFirstName() + " " + admin.getLastName())
                .proposals(new ArrayList<>())
                .warnings(new ArrayList<>())
                .failures(new ArrayList<>())
                .build();
    }

    private void processBatchProposals(BatchAllocationRequest request, AppUser admin, Building preferredBuilding,
            BatchAllocationProposalResponse response) {
        int successCount = 0;
        int failCount = 0;

        for (Long studentId : request.getStudentIds()) {
            if (stagedAllocationRepository.findByStudent_IdAndStatus(studentId, StagedAllocationStatus.PROPOSED)
                    .isPresent()) {
                log.info("Skipping student {} - already has a pending proposal", studentId);
                String studentName = studentRepository.findById(studentId)
                        .map(s -> s.getAppUser().getFirstName() + " " + s.getAppUser().getLastName())
                        .orElse("Unknown");
                response.addWarning("Student " + studentName + " (ID: " + studentId
                        + ") already has a pending proposal — skipped");
                continue;
            }
            try {
                StagedAllocationResponse proposal = createProposalForStudent(studentId, request.getBatchId(),
                        preferredBuilding, admin);
                response.getProposals().add(proposal);
                successCount++;
                sendProposalNotification(getStudent(studentId), getRoom(proposal.getRoomId()));
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
    }

    private StagedAllocationResponse createProposalForStudent(Long studentId, String batchId,
            Building preferredBuilding, AppUser admin) {
        Student student = getStudent(studentId);
        validateNoActiveAllocation(student.getId());
        validateNoPendingProposal(student.getId());

        Application application = getAwaitingApplication(studentId);
        Building effectiveBuilding = preferredBuilding != null ? preferredBuilding : application.getPreferredBuilding();

        List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(student, application,
                effectiveBuilding);
        if (scoredRooms.isEmpty()) {
            waitingListService.enqueueWaitingList(student, application);
            throw new RuntimeException("No available rooms for student; added to waiting list");
        }

        ScoredRoom bestRoom = scoredRooms.get(0);
        Room room = getRoom(bestRoom.getRoomId());

        StagedAllocation staged = StagedAllocation.builder()
                .student(student)
                .room(room)
                .application(application)
                .status(StagedAllocationStatus.PROPOSED)
                .score(bestRoom.getTotalScore())
                .warnings(stagedAllocationMapper.serializeWarnings(bestRoom.getWarnings()))
                .proposedBy(admin)
                .batchId(batchId)
                .build();

        StagedAllocation saved = stagedAllocationRepository.save(staged);
        eventPublisher.publishEvent(new StagedAllocationProposedEvent(saved));
        return stagedAllocationMapper.toResponse(saved);
    }

    private void validateNoActiveAllocation(Long studentId) {
        if (roomAllocationRepository.existsActiveAllocationByStudentId(studentId)) {
            throw new StudentAlreadyAllocatedException("Student already has active allocation");
        }
    }

    private void validateNoPendingProposal(Long studentId) {
        if (stagedAllocationRepository.findByStudent_IdAndStatus(studentId, StagedAllocationStatus.PROPOSED)
                .isPresent()) {
            throw new RuntimeException("Student already has a pending proposal");
        }
    }

    public PageResponse<StagedAllocationResponse> getStagedAllocations(String batchId, StagedAllocationStatus status,
            Pageable pageable) {
        log.info("Retrieving staged allocations for batchId: {}, status: {}", batchId, status);

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

        return PageMapper.toPageResponse(page.map(stagedAllocationMapper::toResponse));
    }
}
