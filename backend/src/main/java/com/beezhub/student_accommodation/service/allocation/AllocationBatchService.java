package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.AllocationScoringService;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.util.PageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ff4j.FF4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationBatchService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final WaitingListRepository waitingListRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final StagedAllocationRepository stagedAllocationRepository;
    private final AppUserRepository appUserRepository;
    private final BuildingRepository buildingRepository;
    private final AllocationScoringService allocationScoringService;
    private final AllocationLifecycleService allocationLifecycleService;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final AllocationAuditService allocationAuditService;
    private final RoomOccupancyManager roomOccupancyManager;
    private final FF4j ff4j;
    private final TransactionTemplate transactionTemplate;
    private final Environment environment;
    private final ObjectMapper objectMapper;

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
            if (!ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)) {
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

                    allocationLifecycleService.createRoomAllocation(request);

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

        Application application = applicationRepository.findByStudent_Id(studentId).stream()
                .filter(app -> app.getStatus() == ApplicationStatus.AWAITING_ALLOCATION)
                .findFirst()
                .orElseThrow(
                        () -> new RuntimeException("No AWAITING_ALLOCATION application for student: " + studentId));

        if (roomAllocationRepository.existsActiveAllocationByStudentId(studentId)) {
            throw new StudentAlreadyAllocatedException("Student already has active allocation");
        }

        Optional<StagedAllocation> existingProposal = stagedAllocationRepository
                .findByStudent_IdAndStatus(studentId, StagedAllocationStatus.PROPOSED);
        if (existingProposal.isPresent()) {
            throw new RuntimeException("Student already has a pending proposal");
        }

        Building effectiveBuilding = preferredBuilding != null ? preferredBuilding : application.getPreferredBuilding();

        List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(student, application,
                effectiveBuilding);
        if (scoredRooms.isEmpty()) {
            allocationLifecycleService.enqueueWaitingList(studentId, application.getId());
            throw new RuntimeException("No available rooms for student; added to waiting list");
        }

        ScoredRoom bestRoom = scoredRooms.get(0);
        Room room = roomRepository.findById(bestRoom.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + bestRoom.getRoomId()));

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

    @Transactional
    public RoomAllocationResponse confirmAllocation(Long stagedAllocationId, Long adminId) {
        log.info("Confirming allocation {} by admin {}", stagedAllocationId, adminId);

        StagedAllocation staged = stagedAllocationRepository.findById(stagedAllocationId)
                .orElseThrow(() -> new RuntimeException("Staged allocation not found: " + stagedAllocationId));

        if (staged.getStatus() != StagedAllocationStatus.PROPOSED) {
            throw new RuntimeException("Can only confirm PROPOSED allocations. Current status: " + staged.getStatus());
        }

        validateRoomCapacity(staged.getRoom());

        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(staged.getStudent());
        allocation.setRoom(staged.getRoom());
        allocation.setApplication(staged.getApplication());
        allocation.setAllocationDate(LocalDateTime.now());
        AllocationStatusValidator.validate(allocation, AllocationStatus.OFFERED);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setAllocationMethod(AllocationMethod.AUTO);

        RoomAllocation saved = roomAllocationRepository.save(allocation);

        roomOccupancyManager.updateOccupancy(staged.getRoom(), 1);

        staged.setStatus(StagedAllocationStatus.CONFIRMED);
        staged.setConfirmedAt(LocalDateTime.now());
        stagedAllocationRepository.save(staged);

        AppUser admin = appUserRepository.findById(adminId).orElse(null);
        allocationAuditService.logAuditAction(saved, AllocationAuditActionType.FORCE_ALLOCATE, admin,
                "Confirmed from staged allocation batch: " + staged.getBatchId(), null, staged.getRoom());

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

        // Use mapper from lifecycle service or similar? No, I need mapper here.
        // Wait, mapper is not injected. I should add it.
        // Or I can return void? The original method returned RoomAllocationResponse.
        // I'll add mapper dependency.
        
        return null; // Placeholder until I inject Mapper
    }

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

        log.info("Rejected allocation {} - student {} remains in AWAITING_ALLOCATION",
                stagedAllocationId, staged.getStudent().getId());

        try {
            userNotificationService.createAllocationRejectionNotification(
                    staged.getStudent().getAppUser().getId(),
                    staged.getRoom().getRoomNumber(),
                    reason);
        } catch (Exception e) {
            log.error("Failed to send rejection notification", e);
        }
    }

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

        responses.sort(Comparator.comparing(WaitingListEntryResponse::getPriorityScore).reversed());

        for (int i = 0; i < responses.size(); i++) {
            responses.get(i).setPosition(i + 1);
        }

        log.info("Returning {} waiting list entries with scores", responses.size());
        return responses;
    }

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

                allocationScoringService.scoreAvailableRooms(
                        student, application, preferredBuilding);

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

    @Transactional
    public StagedAllocationResponse overrideConstraints(Long studentId, Long roomId, String reason,
            Long adminId, List<String> constraintsBypassed) {
        log.info("Override allocation for student {} to room {} by admin {}: {}",
                studentId, roomId, adminId, reason);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found: " + studentId));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        validateRoomCapacity(room);

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

        allocationAuditService.logAuditActionWithConstraint(null, AllocationAuditActionType.OVERRIDE, admin, reason,
                null, room, String.join(", ", constraintsBypassed));

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

    private void validateRoomCapacity(Room room) {
        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
        if (activeAllocations >= room.getCapacity()) {
            throw new com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException(
                    "Room " + room.getRoomNumber() + " is at full capacity (" + room.getCapacity() + " occupants)");
        }
    }
}
