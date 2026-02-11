package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DeclineReason;
import com.beezhub.student_accommodation.repository.*;
import org.ff4j.FF4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private FF4j ff4j;

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
    }

    @Test
    void createRoomAllocation_shouldCreateAllocation() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(roomAllocationRepository.existsByApplicationId(1L)).thenReturn(false);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(new RoomAllocation());
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        RoomAllocationResponse response = roomAllocationService.createRoomAllocation(allocationRequest);

        assertThat(response).isNotNull();
        verify(roomAllocationRepository).save(any(RoomAllocation.class));
        verify(notificationService).sendRoomAllocationNotification(any(RoomAllocation.class));
    }

    @Test
    void createRoomAllocation_shouldThrowException_whenRoomFull() {
        room.setCapacity(1);
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> roomAllocationService.createRoomAllocation(allocationRequest))
                .isInstanceOf(RoomCapacityExceededException.class);
    }

    @Test
    void createRoomAllocation_shouldThrowException_whenStudentAlreadyAllocated() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> roomAllocationService.createRoomAllocation(allocationRequest))
                .isInstanceOf(StudentAlreadyAllocatedException.class);
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
        
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        
        // Mock dependencies for createRoomAllocation internally called
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(new RoomAllocation());

        roomAllocationService.allocateNextFromWaitingList();

        verify(waitingListRepository).save(waitingListEntry);
        assertThat(waitingListEntry.getStatus()).isEqualTo("ALLOCATED");
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

        // And: Room exists
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        // And: Student exists
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        // And: Application exists
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

        // And: No existing allocation for application
        when(roomAllocationRepository.existsByApplicationId(1L)).thenReturn(false);

        // And: Save returns a valid allocation
        RoomAllocation savedAllocation = new RoomAllocation();
        savedAllocation.setId(1L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(savedAllocation);

        // When: Auto-allocation runs
        roomAllocationService.allocateNextFromWaitingList();

        // Then: Room should be allocated
        verify(roomAllocationRepository).save(any(RoomAllocation.class));
        verify(waitingListRepository).save(any(WaitingListEntry.class));
        verify(notificationService).sendRoomAllocationNotification(any());
    }

    @Test
    void allocateNextFromWaitingList_shouldSkipWhenFeatureDisabled() {
        // Given: FF4j auto-allocation is disabled
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        // When: Auto-allocation runs
        roomAllocationService.allocateNextFromWaitingList();

        // Then: No allocation should occur
        verify(waitingListRepository, never()).findByStatusOrderByCreatedAtAsc(any());
        verify(roomAllocationRepository, never()).save(any());
    }

    @Test
    void allocateNextFromWaitingList_shouldHandleFF4jDatabaseError() {
        // Given: FF4j database is unavailable
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION))
                .thenThrow(new org.ff4j.exception.FeatureAccessException("Database unavailable"));

        // When: Auto-allocation runs
        roomAllocationService.allocateNextFromWaitingList();

        // Then: Should fail gracefully without allocating
        verify(waitingListRepository, never()).findByStatusOrderByCreatedAtAsc(any());
        verify(roomAllocationRepository, never()).save(any());
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

        // Then: No allocation should occur since all suitable rooms were filtered out
        verify(roomAllocationRepository, never()).save(any());
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    // ==================== Decline with Direct Reallocation Tests ====================

    @Test
    @DisplayName("Decline always adds student to waiting list")
    void declineOffer_shouldAlwaysAddToWaitingList() {
        // Given: An OFFERED allocation with student and application
        RoomAllocation offeredAllocation = new RoomAllocation();
        offeredAllocation.setId(10L);
        offeredAllocation.setStatus(AllocationStatus.OFFERED);
        offeredAllocation.setStudent(student);
        offeredAllocation.setApplication(application);
        offeredAllocation.setRoom(room);

        when(roomAllocationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(offeredAllocation));
        when(roomAllocationRepository.countDeclinedAllocationsByApplicationId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        // And: Student not already on waiting list
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);

        // And: FF4j disabled to avoid allocateNextFromWaitingList processing
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        DeclineOfferRequest declineRequest = new DeclineOfferRequest();
        declineRequest.setDeclineReason(DeclineReason.WRONG_LOCATION);

        // When
        roomAllocationService.declineOffer(10L, declineRequest);

        // Then: Application should be set to AWAITING_ALLOCATION
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(applicationRepository).save(application);
        // And: Student should be added to waiting list
        verify(waitingListRepository).save(any(WaitingListEntry.class));
    }

    @Test
    @DisplayName("Decline adds student to waiting list when declined")
    void declineOffer_shouldAddToWaitingList_whenDeclined() {
        // Given: An OFFERED allocation
        RoomAllocation offeredAllocation = new RoomAllocation();
        offeredAllocation.setId(10L);
        offeredAllocation.setStatus(AllocationStatus.OFFERED);
        offeredAllocation.setStudent(student);
        offeredAllocation.setApplication(application);
        offeredAllocation.setRoom(room);

        when(roomAllocationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(offeredAllocation));
        when(roomAllocationRepository.countDeclinedAllocationsByApplicationId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        // And: Student not already on waiting list
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);

        // And: FF4j disabled
        when(ff4j.check(FF4jConfig.FEATURE_AUTO_ALLOCATION)).thenReturn(false);

        DeclineOfferRequest declineRequest = new DeclineOfferRequest();
        declineRequest.setDeclineReason(DeclineReason.WRONG_LOCATION);

        // When
        roomAllocationService.declineOffer(10L, declineRequest);

        // Then: Application should be set to AWAITING_ALLOCATION
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(applicationRepository).save(application);
        // And: Student should be added to waiting list
        verify(waitingListRepository).save(any(WaitingListEntry.class));
    }

}
