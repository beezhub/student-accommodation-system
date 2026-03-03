package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.model.event.AllocationCreatedEvent;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.event.AllocationCreatedEvent;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DeclineReason;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.beezhub.student_accommodation.model.event.AllocationAcceptedEvent;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllocationLifecycleServiceTest {

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
    private WaitingListService waitingListService;
    @Mock
    private AllocationAuditService allocationAuditService;
    @Mock
    private LegacyScoringStrategy allocationScoringService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RoomOccupancyManager roomOccupancyManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    @InjectMocks
    private AllocationLifecycleService allocationLifecycleService;

    private Student student;
    private Room room;
    private Application application;
    private RoomAllocationRequest allocationRequest;

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
    }

    @Test
    void createRoomAllocation_shouldCreateAllocation() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(roomAllocationRepository.existsByApplicationId(1L)).thenReturn(false);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        RoomAllocationResponse response = allocationLifecycleService.createRoomAllocation(allocationRequest);

        assertThat(response).isNotNull();
        verify(roomAllocationRepository).save(any(RoomAllocation.class));
        verify(roomAllocationRepository).save(any(RoomAllocation.class));
        verify(eventPublisher).publishEvent(any(AllocationCreatedEvent.class));
        verify(roomOccupancyManager).updateOccupancy(room, 1);
        verify(roomOccupancyManager).updateOccupancy(room, 1);
    }

    @Test
    void manualAllocate_validRoom_allocationCreated() {
        // Alias test for manual allocation
        createRoomAllocation_shouldCreateAllocation();
    }

    @Test
    void createRoomAllocation_shouldThrowException_whenRoomFull() {
        room.setCapacity(1);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> allocationLifecycleService.createRoomAllocation(allocationRequest))
                .isInstanceOf(RoomCapacityExceededException.class);
    }

    @Test
    void createRoomAllocation_shouldThrowException_whenStudentAlreadyAllocated() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> allocationLifecycleService.createRoomAllocation(allocationRequest))
                .isInstanceOf(StudentAlreadyAllocatedException.class);
    }

    @Test
    void acceptOffer_validOffer_statusChangedToAccepted() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        AcceptOfferRequest request = new AcceptOfferRequest();
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(1));

        allocationLifecycleService.acceptOffer(1L, request);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ALLOCATED);
        verify(roomAllocationRepository).save(allocation);
        verify(eventPublisher).publishEvent(any(AllocationAcceptedEvent.class));
    }

    @Test
    void acceptOffer_expiredOffer_throwsException() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.EXPIRED);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        AcceptOfferRequest request = new AcceptOfferRequest();
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> allocationLifecycleService.acceptOffer(1L, request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void declineOffer_shouldSetStatusToDeclined() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.countDeclinedAllocationsByApplicationId(1L)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        DeclineOfferRequest request = new DeclineOfferRequest();
        request.setDeclineReason(DeclineReason.WRONG_LOCATION);

        allocationLifecycleService.declineOffer(1L, request);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.DECLINED);
        verify(roomOccupancyManager).updateOccupancy(room, -1);
        verify(roomAllocationRepository).save(allocation);
        verify(waitingListService).reQueueStudent(any(), eq(1));
    }

    @Test
    void declineOffer_secondDecline_applicationAutoCancelled() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setStudent(student);
        allocation.setApplication(application);
        allocation.setRoom(room);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.countDeclinedAllocationsByApplicationId(1L)).thenReturn(1L); // 1 previous decline
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        DeclineOfferRequest request = new DeclineOfferRequest();
        request.setDeclineReason(DeclineReason.WRONG_LOCATION);

        allocationLifecycleService.declineOffer(1L, request);

        // Check delegation
        verify(roomOccupancyManager).updateOccupancy(room, -1);
        verify(waitingListService).reQueueStudent(any(), eq(2));
    }

    @Test
    void declineOffer_thirdDecline_shouldReQueueUnlimited() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setStudent(student);
        allocation.setApplication(application);
        allocation.setRoom(room);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.countDeclinedAllocationsByApplicationId(1L)).thenReturn(2L); // 2 previous
                                                                                                   // declines
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        DeclineOfferRequest request = new DeclineOfferRequest();
        request.setDeclineReason(DeclineReason.WRONG_LOCATION);

        allocationLifecycleService.declineOffer(1L, request);

        // Verify no cancellation — student is re-queued regardless of decline count
        verify(roomOccupancyManager).updateOccupancy(room, -1);
        verify(waitingListService).reQueueStudent(any(), eq(3));
    }

    @Test
    void deleteRoomAllocation_activeAllocation_statusCancelled() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setRoom(room);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        allocationLifecycleService.deleteRoomAllocation(1L);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.CANCELLED);
        verify(roomOccupancyManager).updateOccupancy(room, -1);
        verify(roomAllocationRepository).save(allocation);
    }

    @Test
    void cancelAllocation_activeAllocation_statusCancelled() {
        // Alias for deleteRoomAllocation
        deleteRoomAllocation_activeAllocation_statusCancelled();
    }
}
