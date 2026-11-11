package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.service.document.DocumentReadinessGuard;
import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GH-125: the scheduled batch runs with no {@link TenantContext}, so the Hibernate filter was off
 * and the room pool was every tenant's rooms. A tenant-A applicant could be offered a tenant-B
 * room, and the allocation was then stamped with the room's tenant.
 *
 * <p>The room query is scoped by that context rather than by an argument, so these tests record the
 * tenant in force at each call and assert the matching happened inside the right one.
 */
class AllocationBatchTenantScopeTest {

    private static final long TENANT_A = 11L;
    private static final long TENANT_B = 22L;

    private final WaitingListRepository waitingListRepository = mock(WaitingListRepository.class);
    private final AllocationLifecycleService lifecycleService = mock(AllocationLifecycleService.class);
    private final FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    private final Environment environment = mock(Environment.class);
    private final AllocationScoringOrchestrator orchestrator = mock(AllocationScoringOrchestrator.class);
    private final StagedAllocationService stagedAllocationService = mock(StagedAllocationService.class);
    private final RoomAllocationMapper roomAllocationMapper = mock(RoomAllocationMapper.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

    private AllocationBatchService service;

    /** Tenant in force each time the room pool was queried, in call order. */
    private final List<Long> roomQueryTenants = new ArrayList<>();
    /** Tenant in force each time an allocation was created, keyed by student id. */
    private final Map<Long, Long> allocationTenants = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new AllocationBatchService(waitingListRepository, lifecycleService, featureFlagService,
                transactionTemplate, environment, orchestrator, stagedAllocationService, roomAllocationMapper,
                new SimpleMeterRegistry(), neutralPriorityScorer(),
                mock(DocumentReadinessGuard.class));

        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(i ->
                ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null));

        when(orchestrator.getAvailableRoomsForBatch()).thenAnswer(i -> {
            roomQueryTenants.add(TenantContext.getTenantId());
            return List.of(roomFor(TenantContext.getTenantId()));
        });
        when(orchestrator.newBedLedger(any())).thenAnswer(i ->
                BedLedgers.of(Map.of(roomIdFor(TenantContext.getTenantId()), 1)));
        when(orchestrator.buildScoringMatrix(any())).thenAnswer(i -> {
            List<WaitingListEntry> entries = i.getArgument(0);
            Map<Long, List<ScoredRoom>> matrix = new HashMap<>();
            for (WaitingListEntry entry : entries) {
                matrix.put(entry.getStudent().getId(), List.of(ScoredRoom.builder()
                        .roomId(roomIdFor(TenantContext.getTenantId()))
                        .totalScore(new BigDecimal("90"))
                        .build()));
            }
            return matrix;
        });
        when(lifecycleService.createRoomAllocation(any())).thenAnswer(i -> {
            RoomAllocationRequest request = i.getArgument(0);
            allocationTenants.put(request.getStudentId(), TenantContext.getTenantId());
            return new RoomAllocationResponse();
        });
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static long roomIdFor(Long tenantId) {
        return 1000 + tenantId;
    }

    private static Room roomFor(Long tenantId) {
        Room room = new Room();
        room.setId(roomIdFor(tenantId));
        room.setCapacity(1);
        return room;
    }

    private static WaitingListEntry entryFor(long studentId, Long applicationTenantId, Building preferredBuilding) {
        Student student = new Student();
        student.setId(studentId);
        Application application = new Application();
        application.setId(500 + studentId);
        application.setStudent(student);
        application.setTenantId(applicationTenantId);
        application.setPreferredBuilding(preferredBuilding);
        WaitingListEntry entry = new WaitingListEntry();
        entry.setId(studentId);
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        return entry;
    }

    @Test
    void eachTenantIsMatchedAgainstItsOwnRoomsOnly() {
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(
                entryFor(1L, TENANT_A, null),
                entryFor(2L, TENANT_B, null)));

        service.batchAllocateFromWaitingList(10);

        // The room pool was queried once per tenant, each time with that tenant in context.
        assertThat(roomQueryTenants).containsExactlyInAnyOrder(TENANT_A, TENANT_B);
        // Each student was allocated while their own tenant was in force.
        assertThat(allocationTenants).containsEntry(1L, TENANT_A).containsEntry(2L, TENANT_B);
    }

    @Test
    void platformStampedApplication_fallsBackToThePreferredBuildingsTenant() {
        // Applications created by a platform admin are stamped tenant 0 (GH-146).
        Building building = new Building();
        building.setId(9L);
        building.setTenantId(TENANT_B);

        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(
                entryFor(3L, TenantContext.PLATFORM_TENANT_ID, building)));

        service.batchAllocateFromWaitingList(10);

        assertThat(roomQueryTenants).containsExactly(TENANT_B);
        assertThat(allocationTenants).containsEntry(3L, TENANT_B);
    }

    @Test
    void entryWithNoResolvableTenant_isSkippedRatherThanMatchedAgainstEveryTenant() {
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(
                entryFor(4L, null, null)));

        service.batchAllocateFromWaitingList(10);

        assertThat(roomQueryTenants).isEmpty();
        assertThat(allocationTenants).isEmpty();
    }

    @Test
    void theTenantContextIsRestoredAfterwards() {
        TenantContext.setTenantId(TENANT_A);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(
                entryFor(5L, TENANT_B, null)));

        service.batchAllocateFromWaitingList(10);

        assertThat(TenantContext.getTenantId()).isEqualTo(TENANT_A);
    }

    @Test
    void theAllocationBudgetIsSharedAcrossTenants() {
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(
                entryFor(6L, TENANT_A, null),
                entryFor(7L, TENANT_B, null)));

        service.batchAllocateFromWaitingList(1);

        assertThat(allocationTenants).hasSize(1);
    }

    /**
     * A real scorer over a rule repository with nothing in it, so the neutral weights apply and the
     * queue falls back to arrival then id — the order these fixtures already expected (GH-225).
     */
    private static ApplicantPriorityScorer neutralPriorityScorer() {
        return new ApplicantPriorityScorer(org.mockito.Mockito.mock(
                com.beezhub.student_accommodation.repository.AllocationRuleRepository.class));
    }
}
