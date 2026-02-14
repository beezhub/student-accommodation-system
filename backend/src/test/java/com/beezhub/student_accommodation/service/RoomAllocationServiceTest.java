package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.allocation.AllocationLifecycleService;
import com.beezhub.student_accommodation.service.allocation.RoomOccupancyManager;
import org.ff4j.FF4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomAllocationServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RoomAllocationMapper roomAllocationMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WaitingListRepository waitingListRepository;
    @Mock
    private StagedAllocationRepository stagedAllocationRepository;
    @Mock
    private AllocationAuditLogRepository allocationAuditLogRepository;
    @Mock
    private AllocationScoringService allocationScoringService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private RoomAttributesRepository roomAttributesRepository;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private RoomOccupancyManager roomOccupancyManager;
    @Mock
    private FF4j ff4j;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private AllocationLifecycleService allocationLifecycleService;

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    private Student student;
    private Room room;
    private Application application;
    private RoomAllocationRequest allocationRequest;
    private WaitingListEntry waitingListEntry;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setFirstName("John");
        appUser.setLastName("Doe");
        student.setAppUser(appUser);

        room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setCapacity(2);
        room.setCurrentOccupancy(0);

        application = new Application();
        application.setId(1L);
        application.setStudent(student);
        application.setStatus(ApplicationStatus.APPROVED);

        allocationRequest = new RoomAllocationRequest();
        allocationRequest.setStudentId(1L);
        allocationRequest.setRoomId(1L);
        allocationRequest.setApplicationId(1L);

        waitingListEntry = new WaitingListEntry();
        waitingListEntry.setId(1L);
        waitingListEntry.setStudent(student);
        waitingListEntry.setApplication(application);
        waitingListEntry.setStatus("WAITING");

        // Mock transactionTemplate to execute the callback
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void createRoomAllocation_shouldDelegateToLifecycleService() {
        RoomAllocationResponse expectedResponse = new RoomAllocationResponse();
        when(allocationLifecycleService.createRoomAllocation(allocationRequest)).thenReturn(expectedResponse);

        RoomAllocationResponse response = roomAllocationService.createRoomAllocation(allocationRequest);

        assertThat(response).isSameAs(expectedResponse);
        verify(allocationLifecycleService).createRoomAllocation(allocationRequest);
    }

    @Test
    void allocateNextFromWaitingList_shouldAllocateBestMatch() {
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(Collections.singletonList(waitingListEntry));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);

        ScoredRoom scoredRoom = new ScoredRoom();
        scoredRoom.setRoomId(1L);
        scoredRoom.setTotalScore(BigDecimal.TEN);
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(Collections.singletonList(scoredRoom));

        // Mock room availability for batch allocation
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // Mock dependencies for createRoomAllocation internally called (delegated)
        when(allocationLifecycleService.createRoomAllocation(any())).thenReturn(new RoomAllocationResponse());

        roomAllocationService.allocateNextFromWaitingList();

        verify(waitingListRepository).save(waitingListEntry);
        assertThat(waitingListEntry.getStatus()).isEqualTo("ALLOCATED");
        // Verify delegation inside the loop
        verify(allocationLifecycleService).createRoomAllocation(any());
    }

    @Test
    void enqueueWaitingList_shouldAddStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);

        MessageResponse response = roomAllocationService.enqueueWaitingList(1L, 1L);

        assertThat(response.getMessage()).contains("added to waiting list");
        verify(waitingListRepository).save(any(WaitingListEntry.class));
    }

    @Test
    void getWaitingListWithScores_shouldReturnScoredList() {
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(Collections.singletonList(waitingListEntry));
        
        ScoredRoom scoredRoom = new ScoredRoom();
        scoredRoom.setRoomId(1L);
        scoredRoom.setTotalScore(BigDecimal.TEN);
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any())).thenReturn(Collections.singletonList(scoredRoom));

        List<WaitingListEntryResponse> response = roomAllocationService.getWaitingListWithScores(null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getPriorityScore()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void checkStudentEligibility_shouldReturnEligible() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.APPROVED)).thenReturn(true);

        AllocationEligibilityResponse response = roomAllocationService.checkStudentEligibility(1L);

        assertThat(response.isEligible()).isTrue();
    }

    @Test
    void allocateNextFromWaitingList_shouldAutoAllocateWhenFeatureEnabled() {
        // Given: FF4j auto-allocation is enabled
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);

        // And: Student on waiting list with application
        waitingListEntry.setStudent(student);
        waitingListEntry.setApplication(application);
        waitingListEntry.setStatus("WAITING");
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"))
                .thenReturn(Collections.singletonList(waitingListEntry));

        // And: No active allocation for student
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);

        // And: Scoring service finds suitable room
        ScoredRoom scoredRoom = new ScoredRoom();
        scoredRoom.setRoomId(1L);
        scoredRoom.setRoomNumber("101");
        scoredRoom.setTotalScore(new BigDecimal("85.5"));
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                .thenReturn(Collections.singletonList(scoredRoom));

        // And: Room availability for batch allocation
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(room));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // When: Auto-allocation runs
        roomAllocationService.allocateNextFromWaitingList();

        // Then: Room should be allocated (via delegation)
        verify(allocationLifecycleService).createRoomAllocation(any());
        verify(waitingListRepository).save(any(WaitingListEntry.class));
    }

    @Test
    void allocateNextFromWaitingList_shouldSkipWhenFeatureDisabled() {
        // Given: FF4j auto-allocation is disabled
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        // When: Auto-allocation runs
        roomAllocationService.allocateNextFromWaitingList();

        // Then: No allocation should occur
        verify(waitingListRepository, never()).findByStatusOrderByCreatedAtAsc(any());
        verify(allocationLifecycleService, never()).createRoomAllocation(any());
    }

    @Test
    void allocateNextFromWaitingList_shouldSkipDeclinedRooms() {
        // Given: FF4j auto-allocation is enabled
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);

        // And: Student on waiting list
        waitingListEntry.setStudent(student);
        waitingListEntry.setApplication(application);
        waitingListEntry.setStatus("WAITING");
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"))
                .thenReturn(Collections.singletonList(waitingListEntry));

        // And: No active allocation for student
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);

        // And: Scoring service returns empty list (declined rooms are filtered out by the service)
        when(allocationScoringService.scoreAvailableRooms(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // When: Auto-allocation runs
        roomAllocationService.allocateNextFromWaitingList();

        // Then: No allocation should occur
        verify(allocationLifecycleService, never()).createRoomAllocation(any());
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    @Test
    void declineOffer_shouldDelegateAndTriggerAllocation() {
        DeclineOfferRequest request = new DeclineOfferRequest();
        when(allocationLifecycleService.declineOffer(1L, request)).thenReturn(new RoomAllocationResponse());
        
        // Mock dependencies for allocateNextFromWaitingList
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(true);
        when(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING")).thenReturn(Collections.emptyList());

        roomAllocationService.declineOffer(1L, request);

        verify(allocationLifecycleService).declineOffer(1L, request);
        verify(waitingListRepository).findByStatusOrderByCreatedAtAsc("WAITING"); // Verify triggering allocation
    }
}
