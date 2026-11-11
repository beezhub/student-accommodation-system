package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.BatchAllocationResult;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.dto.WaitingListEntryResponse;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationBatchServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private WaitingListRepository waitingListRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StagedAllocationRepository stagedAllocationRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private LegacyScoringStrategy allocationScoringService;
    @Mock
    private AllocationLifecycleService allocationLifecycleService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private AllocationAuditService allocationAuditService;
    @Mock
    private RoomOccupancyManager roomOccupancyManager;
    @Mock
    private RoomAllocationMapper roomAllocationMapper;
    @Mock
    private AllocationScoringOrchestrator allocationScoringOrchestrator;

    /**
     * Real, not a mock: the queue order is what GH-225 changed, and a stubbed scorer would assert
     * nothing about it. Backed by a mock repository that returns no rule, so the neutral weights
     * apply and ordering falls to arrival then id — the order these fixtures already expected.
     */
    @org.mockito.Spy
    private ApplicantPriorityScorer applicantPriorityScorer =
            new ApplicantPriorityScorer(org.mockito.Mockito.mock(
                    com.beezhub.student_accommodation.repository.AllocationRuleRepository.class));

    @Mock
    private FeatureFlagService featureFlagService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private Environment environment;
    @Mock
    private ObjectMapper objectMapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    @InjectMocks
    private AllocationBatchService allocationBatchService;

    @BeforeEach
    void setUp() {
        // Mock transaction template to execute callback immediately
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        
        lenient().when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        lenient().when(allocationLifecycleService.createRoomAllocation(any())).thenReturn(new com.beezhub.student_accommodation.model.dto.RoomAllocationResponse());
    }

    @Test
    void executeBatch_approvedApplications_stagedAllocationsCreated() {
        // Arrange
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
        application.setTenantId(1L);
        application.setId(10L);
        application.setStudent(student);
        application.setStatus(ApplicationStatus.APPROVED);
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");

        Room room = new Room();
        room.setId(100L);
        room.setCapacity(1);
        room.setCurrentOccupancy(0);

        ScoredRoom scoredRoom = ScoredRoom.builder()
                .roomId(100L)
                .totalScore(new BigDecimal("90.0"))
                .build();

        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));
        when(allocationScoringOrchestrator.buildScoringMatrix(any())).thenReturn(java.util.Map.of(1L, List.of(scoredRoom)));
        when(allocationScoringOrchestrator.getAvailableRoomsForBatch()).thenReturn(List.of(room));
        when(allocationScoringOrchestrator.newBedLedger(any())).thenReturn(BedLedgers.of(java.util.Map.of(100L, 1)));

        // Act
        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(10);

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(allocationLifecycleService).createRoomAllocation(any());
        verify(waitingListRepository).save(entry);
    }

    /**
     * The closed application comes first in the queue and the budget is one, which is what a
     * waiting-list refill runs with. If it is matched, the seat goes to an allocation that can only
     * fail, and the student who could take the room misses out.
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = ApplicationStatus.class, names = {"REJECTED", "EXPIRED", "CANCELLED"})
    void batchAllocateFromWaitingList_terminalApplication_notMatchedAndSeatGoesToNext(
            ApplicationStatus terminal) {
        WaitingListEntry closed = waitingEntry(1L, 10L, terminal, LocalDateTime.now().minusDays(2));
        WaitingListEntry live = waitingEntry(2L, 20L, ApplicationStatus.AWAITING_ALLOCATION,
                LocalDateTime.now().minusDays(1));

        Room room = new Room();
        room.setId(100L);
        room.setCapacity(1);
        ScoredRoom scoredRoom = ScoredRoom.builder().roomId(100L).totalScore(new BigDecimal("90.0")).build();

        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(closed, live));
        when(allocationScoringOrchestrator.buildScoringMatrix(any()))
                .thenReturn(java.util.Map.of(1L, List.of(scoredRoom), 2L, List.of(scoredRoom)));
        when(allocationScoringOrchestrator.getAvailableRoomsForBatch()).thenReturn(List.of(room));
        when(allocationScoringOrchestrator.newBedLedger(any()))
                .thenReturn(BedLedgers.of(java.util.Map.of(100L, 1)));

        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(1);

        assertEquals(1, result.getSuccessCount());
        assertEquals(List.of(2L), result.getAllocatedStudentIds());
        assertEquals("WAITING", closed.getStatus());
        assertEquals("ALLOCATED", live.getStatus());
    }

    private static WaitingListEntry waitingEntry(Long studentId, Long applicationId, ApplicationStatus status,
            LocalDateTime createdAt) {
        Student student = new Student();
        student.setId(studentId);
        Application application = new Application();
        application.setTenantId(1L);
        application.setId(applicationId);
        application.setStudent(student);
        application.setStatus(status);
        WaitingListEntry entry = new WaitingListEntry();
        entry.setId(applicationId);
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        entry.setCreatedAt(createdAt);
        return entry;
    }

    @Test
    void executeBatch_noEligible_noAllocations() {
        // Arrange
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
        application.setTenantId(1L);
        application.setStudent(student);
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);

        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));
        // Not reached: the service checks for available rooms before scoring.
        lenient().when(allocationScoringOrchestrator.buildScoringMatrix(any())).thenReturn(Collections.emptyMap());
        // Or rooms empty
        when(allocationScoringOrchestrator.getAvailableRoomsForBatch()).thenReturn(Collections.emptyList());

        // Act
        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(10);

        // Assert
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(allocationLifecycleService, never()).createRoomAllocation(any());
    }

    @Test
    void allocateNextFromWaitingList_shouldAllocateBestMatch() {
        // Arrange
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
        application.setTenantId(1L);
        application.setId(10L);
        application.setStudent(student);
        application.setStatus(ApplicationStatus.APPROVED);
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");

        Room room = new Room();
        room.setId(100L);
        room.setCapacity(1);
        room.setCurrentOccupancy(0);

        ScoredRoom scoredRoom = ScoredRoom.builder()
                .roomId(100L)
                .totalScore(new BigDecimal("90.0"))
                .build();

        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));
        when(allocationScoringOrchestrator.buildScoringMatrix(any())).thenReturn(java.util.Map.of(1L, List.of(scoredRoom)));
        when(allocationScoringOrchestrator.getAvailableRoomsForBatch()).thenReturn(List.of(room));
        when(allocationScoringOrchestrator.newBedLedger(any())).thenReturn(BedLedgers.of(java.util.Map.of(100L, 1)));

        // Act
        allocationBatchService.allocateNextFromWaitingList();

        // Assert
        verify(allocationLifecycleService).createRoomAllocation(any());
        verify(waitingListRepository).save(entry);
    }

    @Test
    void getWaitingListWithScores_shouldReturnScoredList() {
        // Arrange
        WaitingListEntryResponse responseItem = WaitingListEntryResponse.builder()
                .priorityScore(BigDecimal.TEN)
                .bestMatchRoomId(100L)
                .build();
        
        when(allocationScoringOrchestrator.getWaitingListWithScores(null)).thenReturn(List.of(responseItem));

        // Act
        var response = allocationBatchService.getWaitingListWithScores(null);

        // Assert
        assertEquals(1, response.size());
        assertEquals(BigDecimal.TEN, response.get(0).getPriorityScore());
        assertEquals(100L, response.get(0).getBestMatchRoomId());
    }

    @Test
    void allocateNextFromWaitingList_shouldSkipWhenFeatureDisabled() {
        // Given
        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        // Act
        allocationBatchService.allocateNextFromWaitingList();

        // Then
        verify(waitingListRepository, never()).findByStatusOrderByCreatedAtAsc(any());
        verify(allocationLifecycleService, never()).createRoomAllocation(any());
    }

    @Test
    void allocateNextFromWaitingList_shouldSkipDeclinedRooms() {
        // Given: auto-allocation is enabled
        when(featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);

        // And: Student on waiting list
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
        application.setTenantId(1L);
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));

        // And: Scoring service returns empty list (declined rooms are filtered out by the service).
        // Not reached: the service checks for available rooms before scoring.
        lenient().when(allocationScoringOrchestrator.buildScoringMatrix(any())).thenReturn(Collections.emptyMap());

        // Act
        allocationBatchService.allocateNextFromWaitingList();

        // Then
        verify(allocationLifecycleService, never()).createRoomAllocation(any());
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    @Test
    void batchAllocate_nullPreferredBuilding_stillAllocates() {
        // Application has no preferred building — allocation should proceed normally
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
        application.setTenantId(1L);
        application.setId(10L);
        application.setStudent(student);
        application.setStatus(ApplicationStatus.APPROVED);
        application.setPreferredBuilding(null); // explicitly no preference

        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");

        Room room = new Room();
        room.setId(100L);
        room.setCapacity(1);
        room.setCurrentOccupancy(0);

        ScoredRoom scoredRoom = ScoredRoom.builder()
                .roomId(100L)
                .totalScore(new BigDecimal("100.0"))
                .build();

        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));
        when(allocationScoringOrchestrator.buildScoringMatrix(any()))
                .thenReturn(java.util.Map.of(1L, List.of(scoredRoom)));
        when(allocationScoringOrchestrator.getAvailableRoomsForBatch()).thenReturn(List.of(room));
        when(allocationScoringOrchestrator.newBedLedger(any()))
                .thenReturn(BedLedgers.of(java.util.Map.of(100L, 1)));

        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(10);

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(allocationLifecycleService).createRoomAllocation(any());
    }
}
