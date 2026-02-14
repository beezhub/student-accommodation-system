package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.model.dto.BatchAllocationResult;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.AllocationScoringService;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ff4j.FF4j;
import org.junit.jupiter.api.BeforeEach;
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
    private AllocationScoringService allocationScoringService;
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
    private FF4j ff4j;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private Environment environment;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AllocationBatchService allocationBatchService;

    @BeforeEach
    void setUp() {
        // Mock transaction template to execute callback immediately
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        
        lenient().when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        lenient().when(allocationLifecycleService.createRoomAllocation(any())).thenReturn(new com.beezhub.student_accommodation.model.dto.RoomAllocationResponse());
    }

    @Test
    void executeBatch_approvedApplications_stagedAllocationsCreated() {
        // Arrange
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
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
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(100L)).thenReturn(0L);
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(List.of(scoredRoom));

        // Act
        BatchAllocationResult result = allocationBatchService.batchAllocateFromWaitingList(10);

        // Assert
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(allocationLifecycleService).createRoomAllocation(any());
        verify(waitingListRepository).save(entry);
    }

    @Test
    void executeBatch_noEligible_noAllocations() {
        // Arrange
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
        application.setStudent(student);
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);

        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));
        when(roomRepository.findAll()).thenReturn(Collections.emptyList()); // No rooms
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(Collections.emptyList());

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
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(100L)).thenReturn(0L);
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(List.of(scoredRoom));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);

        // Act
        allocationBatchService.allocateNextFromWaitingList();

        // Assert
        verify(allocationLifecycleService).createRoomAllocation(any());
        verify(waitingListRepository).save(entry);
    }

    @Test
    void getWaitingListWithScores_shouldReturnScoredList() {
        // Arrange
        Student student = new Student();
        student.setId(1L);
        AppUser appUser = new AppUser();
        appUser.setFirstName("John");
        appUser.setLastName("Doe");
        appUser.setEmail("john@example.com");
        student.setAppUser(appUser);
        
        Application application = new Application();
        application.setId(10L);
        application.setStatus(ApplicationStatus.APPROVED);
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setId(1L);
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));
        
        ScoredRoom scoredRoom = ScoredRoom.builder()
                .roomId(100L)
                .roomNumber("101")
                .totalScore(BigDecimal.TEN)
                .build();
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(List.of(scoredRoom));

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
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        // Act
        allocationBatchService.allocateNextFromWaitingList();

        // Then
        verify(waitingListRepository, never()).findByStatusOrderByCreatedAtAsc(any());
        verify(allocationLifecycleService, never()).createRoomAllocation(any());
    }

    @Test
    void allocateNextFromWaitingList_shouldSkipDeclinedRooms() {
        // Given: FF4j auto-allocation is enabled
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);

        // And: Student on waiting list
        Student student = new Student();
        student.setId(1L);
        Application application = new Application();
        
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStudent(student);
        entry.setApplication(application);
        entry.setStatus("WAITING");
        
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(List.of(entry));

        // And: No active allocation for student
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);

        // And: Scoring service returns empty list (declined rooms are filtered out by the service)
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(Collections.emptyList());

        // Act
        allocationBatchService.allocateNextFromWaitingList();

        // Then
        verify(allocationLifecycleService, never()).createRoomAllocation(any());
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }
}
