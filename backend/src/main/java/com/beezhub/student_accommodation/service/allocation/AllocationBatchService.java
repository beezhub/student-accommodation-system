package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.util.PageMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AllocationBatchService {

    private final WaitingListRepository waitingListRepository;
    private final AllocationLifecycleService allocationLifecycleService;
    private final FeatureFlagService featureFlagService;
    private final TransactionTemplate transactionTemplate;
    private final Environment environment;
    private final AllocationScoringOrchestrator allocationScoringOrchestrator;
    private final StagedAllocationService stagedAllocationService;
    private final RoomAllocationMapper roomAllocationMapper;
    private final MeterRegistry meterRegistry;

    public AllocationBatchService(
            WaitingListRepository waitingListRepository,
            @Lazy AllocationLifecycleService allocationLifecycleService,
            FeatureFlagService featureFlagService,
            @Qualifier(TransactionTemplateConfig.REQUIRES_NEW) TransactionTemplate transactionTemplate,
            Environment environment,
            AllocationScoringOrchestrator allocationScoringOrchestrator,
            StagedAllocationService stagedAllocationService,
            RoomAllocationMapper roomAllocationMapper,
            MeterRegistry meterRegistry) {
        this.waitingListRepository = waitingListRepository;
        this.allocationLifecycleService = allocationLifecycleService;
        this.featureFlagService = featureFlagService;
        this.transactionTemplate = transactionTemplate;
        this.environment = environment;
        this.allocationScoringOrchestrator = allocationScoringOrchestrator;
        this.stagedAllocationService = stagedAllocationService;
        this.roomAllocationMapper = roomAllocationMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Batch allocation job body — called by ScheduledJobRunner.
     * Processes waiting list students when rooms are available.
     */
    public int scheduledBatchAllocation() {
        // Check if batch job is enabled
        boolean batchEnabled = environment.getProperty("app.allocation.batch-enabled", Boolean.class, true);
        if (!batchEnabled) {
            return 0;
        }

        log.info("Starting batch allocation job");

        long waitingCount = waitingListRepository.countByStatus("WAITING");

        if (waitingCount == 0) {
            log.info("No students on waiting list, skipping batch allocation");
            return 0;
        }

        int batchSize = environment.getProperty("app.allocation.batch-size", Integer.class, 50);
        BatchAllocationResult result = batchAllocateFromWaitingList(batchSize);

        log.info("Batch allocation completed: {} succeeded, {} failed out of {} waiting",
                result.getSuccessCount(), result.getFailureCount(), waitingCount);

        return result.getSuccessCount();
    }

    /**
     * Allocates the next student from waiting list (wrapper for batch allocation).
     * Delegates to batchAllocateFromWaitingList with limit of 1.
     *
     * <p>Deliberately not {@code @Transactional}: each allocation commits in its own
     * REQUIRES_NEW transaction, and this is also invoked from an AFTER_COMMIT listener.
     */
    public void allocateNextFromWaitingList() {
        // Delegate to batch method with limit of 1
        BatchAllocationResult result = batchAllocateFromWaitingList(1);

        log.info("Single allocation attempt: {} succeeded, {} failed",
                result.getSuccessCount(), result.getFailureCount());
    }

    /**
     * Batch allocates multiple waiting students to available rooms using optimal
     * matching.
     * Processes up to maxAllocations students in FIFO order from waiting list.
     *
     * <p>Deliberately not {@code @Transactional}. Each allocation runs in its own REQUIRES_NEW
     * transaction via {@code transactionTemplate}, so one failure (room full, student already
     * allocated) rolls back that item only and the reported counts match what was committed.
     * An outer transaction here would be joined by nothing but would be marked rollback-only
     * by a failing item, reverting every allocation while the result still claimed success.
     *
     * @param maxAllocations Maximum number of allocations to create
     * @return BatchAllocationResult with success/failure statistics
     */
    @Timed(value = "service.allocation.batchRun", percentiles = {0.5, 0.95, 0.99})
    public BatchAllocationResult batchAllocateFromWaitingList(int maxAllocations) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);

        // Feature flag check
        try {
            if (!featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)) {
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

        // 2. Match within each tenant separately.
        // The scheduled job runs with no TenantContext, so the Hibernate filter is off and the
        // room pool was every tenant's rooms: a tenant-A applicant could be offered a tenant-B
        // room, and the allocation was then stamped with the room's tenant (GH-125). Setting the
        // context per group scopes the room query, the scoring matrix and the stamped tenant.
        List<AllocationPair> allocationPairs = matchWithinEachTenant(waitingList, maxAllocations);

        if (allocationPairs.isEmpty()) {
            log.info("No allocatable student/room pairs found");
            return BatchAllocationResult.builder()
                    .processedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 3. Create allocations in batch
        BatchAllocationResult result = createAllocationsInBatch(allocationPairs);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        result.setProcessedAt(LocalDateTime.now());

        log.info("Batch allocation completed: {} succeeded, {} failed, {}ms processing time",
                result.getSuccessCount(), result.getFailureCount(), result.getProcessingTimeMs());

        sample.stop(meterRegistry.timer("sas.batch.allocation.duration"));
        return result;
    }

    /**
     * Groups the waiting list by the tenant that owns each application and matches each group with
     * {@link TenantContext} set to that tenant, so the room pool, the scoring matrix and the tenant
     * stamped on the resulting allocation all belong to one tenant.
     *
     * <p>Entries whose tenant cannot be determined are skipped rather than matched against every
     * tenant's rooms, and logged so they are visible.
     *
     * @param maxAllocations budget shared across all tenants, spent in waiting-list order
     */
    private List<AllocationPair> matchWithinEachTenant(List<WaitingListEntry> waitingList, int maxAllocations) {
        Map<Long, List<WaitingListEntry>> byTenant = new LinkedHashMap<>();
        for (WaitingListEntry entry : waitingList) {
            Long tenantId = tenantIdOf(entry);
            if (tenantId == null) {
                log.warn("action=batchEntrySkipped reason=noTenant waitingListEntryId={}", entry.getId());
                continue;
            }
            byTenant.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(entry);
        }

        List<AllocationPair> pairs = new ArrayList<>();
        Long previousTenant = TenantContext.getTenantId();
        try {
            for (Map.Entry<Long, List<WaitingListEntry>> group : byTenant.entrySet()) {
                if (pairs.size() >= maxAllocations) {
                    break;
                }
                TenantContext.setTenantId(group.getKey());
                pairs.addAll(matchOneTenant(group.getValue(), maxAllocations - pairs.size(), group.getKey()));
            }
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
        return pairs;
    }

    /** Greedy matching for one tenant, with the tenant already set in the context. */
    private List<AllocationPair> matchOneTenant(List<WaitingListEntry> entries, int budget, Long tenantId) {
        List<Room> availableRooms = allocationScoringOrchestrator.getAvailableRoomsForBatch();
        if (availableRooms.isEmpty()) {
            log.info("No available rooms for tenant {}", tenantId);
            return List.of();
        }

        Map<Long, List<ScoredRoom>> scoringMatrix = allocationScoringOrchestrator.buildScoringMatrix(entries);
        Map<Long, Integer> roomCapacityRemaining = allocationScoringOrchestrator.buildRoomCapacityMap(availableRooms);

        // Stated here rather than inherited from the repository method name. This is the order
        // AllocationScoringOrchestrator.getWaitingListWithScores renders, and the two must not
        // drift: the admin screen showing one order while the job serves another is GH-140.
        List<WaitingListEntry> queue = new ArrayList<>(entries);
        queue.sort(Comparator
                .comparing(WaitingListEntry::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WaitingListEntry::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        List<AllocationPair> pairs = new ArrayList<>();
        for (WaitingListEntry entry : queue) {
            if (pairs.size() >= budget) {
                break;
            }
            List<ScoredRoom> scoredRooms = scoringMatrix.get(entry.getStudent().getId());
            if (scoredRooms == null || scoredRooms.isEmpty()) {
                continue;
            }
            for (ScoredRoom scoredRoom : scoredRooms) {
                Integer remaining = roomCapacityRemaining.get(scoredRoom.getRoomId());
                if (remaining != null && remaining > 0) {
                    pairs.add(new AllocationPair(entry, scoredRoom));
                    roomCapacityRemaining.put(scoredRoom.getRoomId(), remaining - 1);
                    break; // Move to next student
                }
            }
        }
        log.info("Matched {} students to rooms for tenant {}", pairs.size(), tenantId);
        return pairs;
    }

    /**
     * The application's tenant owns the buildings the student applied to, so it decides which room
     * pool they belong in. The waiting-list row's own stamp is the fallback, and is unreliable
     * because entries created by a platform admin are stamped with the platform tenant (GH-146).
     */
    private static Long tenantIdOf(WaitingListEntry entry) {
        Application application = entry.getApplication();
        if (application != null) {
            if (isRealTenant(application.getTenantId())) {
                return application.getTenantId();
            }
            // Applications created by a platform admin are stamped with the platform tenant
            // (GH-146). The building they applied to still identifies the owning tenant.
            Building preferred = application.getPreferredBuilding();
            if (preferred != null && isRealTenant(preferred.getTenantId())) {
                return preferred.getTenantId();
            }
        }
        return isRealTenant(entry.getTenantId()) ? entry.getTenantId() : null;
    }

    private static boolean isRealTenant(Long tenantId) {
        return tenantId != null && !TenantContext.PLATFORM_TENANT_ID.equals(tenantId);
    }

    private BatchAllocationResult createAllocationsInBatch(List<AllocationPair> allocationPairs) {
        BatchAllocationResult.BatchAllocationResultBuilder resultBuilder = BatchAllocationResult.builder();
        resultBuilder.totalAttempted(allocationPairs.size());

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        List<Long> allocatedStudentIds = new ArrayList<>();
        List<Long> allocatedRoomIds = new ArrayList<>();

        Long previousTenant = TenantContext.getTenantId();
        for (AllocationPair pair : allocationPairs) {
            try {
                // Create the allocation under its own tenant so the tenant filter scopes the reads
                // inside createRoomAllocation and TenantListener stamps the right tenant. Matching
                // restored the context before returning, so it has to be set again here (GH-125).
                Long pairTenantId = tenantIdOf(pair.getWaitingListEntry());
                if (pairTenantId != null) {
                    TenantContext.setTenantId(pairTenantId);
                }
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
            } finally {
                if (previousTenant == null) {
                    TenantContext.clear();
                } else {
                    TenantContext.setTenantId(previousTenant);
                }
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
        return stagedAllocationService.proposeAllocation(request, adminId);
    }

    @Transactional
    public RoomAllocationResponse confirmAllocation(Long stagedAllocationId, Long adminId) {
        return stagedAllocationService.confirmAllocation(stagedAllocationId, adminId);
    }

    @Transactional
    public void rejectAllocation(Long stagedAllocationId, String reason, Long adminId) {
        stagedAllocationService.rejectAllocation(stagedAllocationId, reason, adminId);
    }

    @Transactional
    public void confirmBatchAllocations(String batchId, Long adminId) {
        stagedAllocationService.confirmBatchAllocations(batchId, adminId);
    }

    @Transactional
    public BatchConfirmationResult confirmBatchAllocationsWithResult(String batchId, Long adminId) {
        return stagedAllocationService.confirmBatchAllocationsWithResult(batchId, adminId);
    }

    public PageResponse<StagedAllocationResponse> getStagedAllocations(String batchId, StagedAllocationStatus status,
            Pageable pageable) {
        return stagedAllocationService.getStagedAllocations(batchId, status, pageable);
    }

    public List<WaitingListEntryResponse> getWaitingListWithScores(Long buildingId) {
        // This logic belongs in AllocationScoringOrchestrator or WaitingListService?
        // It uses AllocationScoringService.
        // Keeping here for now but using orchestrator if possible.
        // Actually, this is a query method.
        // I will move this to AllocationScoringOrchestrator? No, it returns Response
        // DTOs.
        // Maybe AllocationQueryService?
        // I will keep it here for now but verify line count.
        // It is ~50 lines.

        // Re-implementing logic here for now to ensure compilation, but should be
        // moved.
        // For now, I'll copy the logic back or find a new home.
        // Wait, AllocationBatchService is supposed to be batch orchestration.
        // I'll keep it here.

        return allocationScoringOrchestrator.getWaitingListWithScores(buildingId);
    }

    public RescoreResult rescoreWaitingList() {
        return allocationScoringOrchestrator.rescoreWaitingList();
    }

    @Transactional
    public StagedAllocationResponse overrideConstraints(Long studentId, Long roomId, String reason,
            Long adminId, List<String> constraintsBypassed) {
        return stagedAllocationService.overrideConstraints(studentId, roomId, reason, adminId, constraintsBypassed);
    }
}
