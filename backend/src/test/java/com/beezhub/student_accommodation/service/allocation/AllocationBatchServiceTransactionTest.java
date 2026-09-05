package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.BatchAllocationResult;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the batch through Spring's real transaction machinery
 * ({@link AbstractPlatformTransactionManager} handles propagation, suspension and rollback-only
 * marking) with an in-memory transaction manager, so the commit/rollback behaviour asserted here
 * is exactly what a JPA transaction manager would do, without needing a database.
 */
class AllocationBatchServiceTransactionTest {

    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();
    private final TransactionTemplateConfig config = new TransactionTemplateConfig();

    private final WaitingListRepository waitingListRepository = mock(WaitingListRepository.class);
    private final AllocationLifecycleService lifecycleService = mock(AllocationLifecycleService.class);
    private final FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    private final Environment environment = mock(Environment.class);
    private final AllocationScoringOrchestrator orchestrator = mock(AllocationScoringOrchestrator.class);
    private final StagedAllocationService stagedAllocationService = mock(StagedAllocationService.class);
    private final RoomAllocationMapper roomAllocationMapper = mock(RoomAllocationMapper.class);

    private WaitingListEntry entry1;
    private WaitingListEntry entry2;
    private WaitingListEntry entry3;

    @BeforeEach
    void setUp() {
        entry1 = waitingEntry(1L);
        entry2 = waitingEntry(2L);
        entry3 = waitingEntry(3L);

        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"))
                .thenReturn(List.of(entry1, entry2, entry3));
        when(orchestrator.buildScoringMatrix(any())).thenReturn(Map.of(
                1L, List.of(scored(101L)),
                2L, List.of(scored(102L)),
                3L, List.of(scored(103L))));
        when(orchestrator.getAvailableRoomsForBatch()).thenReturn(List.of(new Room()));
        when(orchestrator.buildRoomCapacityMap(any()))
                .thenReturn(new HashMap<>(Map.of(101L, 1, 102L, 1, 103L, 1)));

        // Student 2 cannot be allocated; students 1 and 3 succeed.
        when(lifecycleService.createRoomAllocation(argThat((RoomAllocationRequest r) -> r != null && r.getStudentId() == 2L)))
                .thenThrow(new StudentAlreadyAllocatedException("Student 2 already allocated"));
        when(lifecycleService.createRoomAllocation(argThat((RoomAllocationRequest r) -> r != null && r.getStudentId() != 2L)))
                .thenReturn(new RoomAllocationResponse());
    }

    private AllocationBatchService serviceUsing(TransactionTemplate template) {
        return new AllocationBatchService(waitingListRepository, lifecycleService, featureFlagService, template,
                environment, orchestrator, stagedAllocationService, roomAllocationMapper, new SimpleMeterRegistry());
    }

    @Test
    void batch_oneItemFails_onlyThatItemRollsBackAndCountsMatchCommits() {
        AllocationBatchService service = serviceUsing(config.requiresNewTransactionTemplate(transactionManager));

        BatchAllocationResult result = service.batchAllocateFromWaitingList(10);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getAllocatedStudentIds()).containsExactly(1L, 3L);
        assertThat(transactionManager.commits.get()).isEqualTo(2);
        assertThat(transactionManager.rollbacks.get()).isEqualTo(1);
        verify(waitingListRepository).save(entry1);
        verify(waitingListRepository, never()).save(entry2);
        verify(waitingListRepository).save(entry3);
    }

    @Test
    void batch_calledInsideCallerTransaction_callerStillCommits() {
        AllocationBatchService service = serviceUsing(config.requiresNewTransactionTemplate(transactionManager));
        TransactionTemplate caller = config.transactionTemplate(transactionManager);

        BatchAllocationResult result = caller.execute(status -> service.batchAllocateFromWaitingList(10));

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(1);
        // two committed items plus the caller's own transaction
        assertThat(transactionManager.commits.get()).isEqualTo(3);
        assertThat(transactionManager.rollbacks.get()).isEqualTo(1);
    }

    @Test
    void batch_withRequiredTemplateInsideCallerTransaction_reproducesTheOriginalBug() {
        // Characterises why REQUIRES_NEW is mandatory: a REQUIRED template joins the caller's
        // transaction, the failing item marks it rollback-only, the result still says 2 succeeded,
        // and the caller's commit then throws and discards every allocation.
        TransactionTemplate required = config.transactionTemplate(transactionManager);
        AllocationBatchService service = serviceUsing(required);
        BatchAllocationResult[] captured = new BatchAllocationResult[1];

        assertThatThrownBy(() -> required.execute(status -> {
            captured[0] = service.batchAllocateFromWaitingList(10);
            return null;
        })).isInstanceOf(UnexpectedRollbackException.class);

        assertThat(captured[0].getSuccessCount()).isEqualTo(2);
        assertThat(transactionManager.commits.get()).isZero();
        assertThat(transactionManager.rollbacks.get()).isEqualTo(1);
    }

    @Test
    void batchEntryPoints_areNotTransactional() throws NoSuchMethodException {
        assertThat(AllocationBatchService.class.getMethod("batchAllocateFromWaitingList", int.class)
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
        assertThat(AllocationBatchService.class.getMethod("allocateNextFromWaitingList")
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
        assertThat(AllocationBatchService.class.getMethod("scheduledBatchAllocation")
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
    }

    // ---- fixtures ----

    private static WaitingListEntry waitingEntry(long studentId) {
        Student student = new Student();
        student.setId(studentId);
        Application application = new Application();
        application.setId(100 + studentId);
        application.setTenantId(1L);
        application.setStudent(student);
        WaitingListEntry entry = new WaitingListEntry();
        entry.setId(studentId);
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        return entry;
    }

    private static ScoredRoom scored(long roomId) {
        return ScoredRoom.builder().roomId(roomId).totalScore(new BigDecimal("80")).build();
    }

    /**
     * Minimal in-memory transaction manager. Spring's abstract base does all propagation work;
     * this only tracks the current transaction per thread and counts commits and rollbacks.
     */
    static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        static final class Tx {
            boolean rollbackOnly;
        }

        static final class Holder implements SmartTransactionObject {
            Tx tx;

            Holder(Tx tx) {
                this.tx = tx;
            }

            @Override
            public boolean isRollbackOnly() {
                return tx != null && tx.rollbackOnly;
            }

            @Override
            public void flush() {
                // nothing to flush
            }
        }

        private final ThreadLocal<Tx> current = new ThreadLocal<>();
        final AtomicInteger commits = new AtomicInteger();
        final AtomicInteger rollbacks = new AtomicInteger();

        @Override
        protected Object doGetTransaction() {
            return new Holder(current.get());
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((Holder) transaction).tx != null;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            Tx tx = new Tx();
            ((Holder) transaction).tx = tx;
            current.set(tx);
        }

        @Override
        protected Object doSuspend(Object transaction) {
            Tx suspended = current.get();
            current.remove();
            ((Holder) transaction).tx = null;
            return suspended;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            current.set((Tx) suspendedResources);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits.incrementAndGet();
            current.remove();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks.incrementAndGet();
            current.remove();
        }

        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            ((Holder) status.getTransaction()).tx.rollbackOnly = true;
        }
    }
}
