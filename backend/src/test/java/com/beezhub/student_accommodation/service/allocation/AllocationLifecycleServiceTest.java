package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.model.event.AllocationCreatedEvent;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.beezhub.student_accommodation.model.event.AllocationAcceptedEvent;
import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private com.beezhub.student_accommodation.service.AuthService authService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RoomOccupancyManager roomOccupancyManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;
    @Mock
    private AllocationAccessGuard allocationAccessGuard;

    @InjectMocks
    private AllocationLifecycleService allocationLifecycleService;

    private Student student;
    private Room room;
    private Application application;
    private RoomAllocationRequest allocationRequest;

    @BeforeEach
    void setUp() {
        // @Value fields are not populated in plain Mockito tests; mirror the
        // application.yml defaults so check-in window validation behaves as in prod.
        org.springframework.test.util.ReflectionTestUtils.setField(
                allocationLifecycleService, "offerExpiryDays", 7);
        org.springframework.test.util.ReflectionTestUtils.setField(
                allocationLifecycleService, "checkInExpiryDays", 7);

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
        verify(eventPublisher).publishEvent(any(AllocationCreatedEvent.class));
        verify(roomOccupancyManager).syncOccupancy(room);
    }

    /**
     * GH-140: neither construction site stamped the academic year, so every row was null and the
     * waiting-list wait estimate, which counts allocations for the year, always counted zero and
     * returned null. The existing WaitingListServiceTest cases stayed green throughout, because
     * they stub that count directly.
     */
    @Test
    void createRoomAllocation_stampsTheAcademicYearFromTheApplication() {
        AcademicYear year = new AcademicYear();
        year.setId(5L);
        year.setName("2026");
        application.setAcademicYear(year);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(false);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(roomAllocationRepository.existsByApplicationId(1L)).thenReturn(false);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        allocationLifecycleService.createRoomAllocation(allocationRequest);

        ArgumentCaptor<RoomAllocation> saved = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(roomAllocationRepository).save(saved.capture());
        assertThat(saved.getValue().getAcademicYear()).isEqualTo(year);
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
    void acceptOffer_callerNotOwner_throwsAndNothingChanges() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStudent(student);
        allocation.setStatus(AllocationStatus.OFFERED);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        doThrow(new UnauthorizedAccessException("not yours"))
                .when(allocationAccessGuard).assertCanAccessAllocation(allocation);

        assertThatThrownBy(() -> allocationLifecycleService.acceptOffer(1L, new AcceptOfferRequest()))
                .isInstanceOf(UnauthorizedAccessException.class);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.OFFERED);
        verify(roomAllocationRepository, never()).save(any());
        verify(waitingListService, never()).updateStatusToAllocated(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void declineOffer_callerNotOwner_throwsAndRoomNotFreed() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setStatus(AllocationStatus.OFFERED);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        doThrow(new UnauthorizedAccessException("not yours"))
                .when(allocationAccessGuard).assertCanAccessAllocation(allocation);

        assertThatThrownBy(() -> allocationLifecycleService.declineOffer(1L, new DeclineOfferRequest()))
                .isInstanceOf(UnauthorizedAccessException.class);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.OFFERED);
        verify(roomOccupancyManager, never()).syncOccupancy(any());
        verify(roomAllocationRepository, never()).save(any());
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

    /** Builds an OFFERED allocation wired to the shared fixtures, ready for the accept path. */
    private RoomAllocation offeredAllocation() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);
        return allocation;
    }

    /**
     * GH-139: the offer deadline emailed to the student was enforced only by the hourly sweep, so a
     * lapsed offer stayed acceptable for up to an hour, and accepting it extended the hold by a
     * further seven days. Rejecting is all this path does - expiring the row, releasing the bed and
     * refilling the waiting list stay with OfferExpiryService, hence the occupancy assertion.
     */
    @Test
    void acceptOffer_offerDeadlinePassed_rejectedAndNothingChanges() {
        RoomAllocation allocation = offeredAllocation();
        allocation.setOfferExpiryDate(LocalDateTime.now().minusMinutes(1));
        application.setStatus(ApplicationStatus.APPROVED);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        // Stubbed so that before the guard existed this test failed on the missing rejection, not on
        // an NPE from an unstubbed save further down acceptOffer.
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        AcceptOfferRequest request = new AcceptOfferRequest();
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> allocationLifecycleService.acceptOffer(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.OFFERED);
        assertThat(allocation.getCheckInExpiryDate()).isNull();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        verify(roomAllocationRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
        verify(waitingListService, never()).updateStatusToAllocated(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(roomOccupancyManager, never()).syncOccupancy(any());
    }

    /**
     * Pins the strict {@code isBefore} comparison: a deadline that has not yet passed is still
     * acceptable. It is the same predicate the expiry sweep queries with, so the two never disagree
     * about a given row.
     */
    @Test
    void acceptOffer_offerDeadlineNotYetReached_accepted() {
        RoomAllocation allocation = offeredAllocation();
        allocation.setOfferExpiryDate(LocalDateTime.now().plusSeconds(2));

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        AcceptOfferRequest request = new AcceptOfferRequest();
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(1));

        allocationLifecycleService.acceptOffer(1L, request);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
    }

    /**
     * The column is nullable, and a row with no deadline is invisible to the sweep query, so accept
     * has to admit it for the two to stay consistent. Unreachable in practice: both paths that create
     * an OFFERED allocation stamp a deadline.
     */
    @Test
    void acceptOffer_nullOfferExpiryDate_accepted() {
        RoomAllocation allocation = offeredAllocation();

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        AcceptOfferRequest request = new AcceptOfferRequest();
        request.setExpectedCheckInDate(LocalDateTime.now().plusDays(1));

        allocationLifecycleService.acceptOffer(1L, request);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
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
        verify(roomOccupancyManager).syncOccupancy(room);
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
        verify(roomOccupancyManager).syncOccupancy(room);
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
        verify(roomOccupancyManager).syncOccupancy(room);
        verify(waitingListService).reQueueStudent(any(), eq(3));
        // Refill is delegated to the AFTER_COMMIT listener, never run inside this transaction
        verify(eventPublisher).publishEvent(new RoomVacatedEvent(1L, room.getId(), AllocationStatus.DECLINED));
    }

    // ── GH-129: updateRoomAllocation had no status or ownership validation ───

    @Test
    void updateRoomAllocation_declinedAllocation_rejectedWithoutTouchingOccupancy() {
        // The room was already released when the offer was declined; re-pointing it would
        // decrement the old room a second time.
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.DECLINED);
        allocation.setStudent(student);
        allocation.setRoom(room);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationLifecycleService.updateRoomAllocation(1L, allocationRequest))
                .isInstanceOf(InvalidAllocationStatusTransitionException.class);

        verify(roomOccupancyManager, never()).syncOccupancy(any());
        verify(roomAllocationRepository, never()).save(any(RoomAllocation.class));
    }

    @Test
    void updateRoomAllocation_completedAllocation_rejected() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.COMPLETED);
        allocation.setStudent(student);
        allocation.setRoom(room);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationLifecycleService.updateRoomAllocation(1L, allocationRequest))
                .isInstanceOf(InvalidAllocationStatusTransitionException.class);
    }

    @Test
    void updateRoomAllocation_newStudentAlreadyHoused_rejected() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setRoom(room);
        Student existingOccupant = new Student();
        existingOccupant.setId(99L);
        allocation.setStudent(existingOccupant);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.existsActiveAllocationByStudentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> allocationLifecycleService.updateRoomAllocation(1L, allocationRequest))
                .isInstanceOf(StudentAlreadyAllocatedException.class);

        verify(roomAllocationRepository, never()).save(any(RoomAllocation.class));
    }

    @Test
    void updateRoomAllocation_offeredAllocationSameStudent_succeeds() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setStudent(student);
        allocation.setRoom(room);

        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(roomAllocationMapper.toResponse(any(RoomAllocation.class))).thenReturn(new RoomAllocationResponse());

        assertThat(allocationLifecycleService.updateRoomAllocation(1L, allocationRequest)).isNotNull();

        // Same student and same room: no occupancy movement at all.
        verify(roomOccupancyManager, never()).syncOccupancy(any());
        verify(roomAllocationRepository).save(allocation);
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
        verify(roomOccupancyManager).syncOccupancy(room);
        verify(roomAllocationRepository).save(allocation);
        verify(eventPublisher).publishEvent(new RoomVacatedEvent(1L, room.getId(), AllocationStatus.CANCELLED));
    }

    @Test
    void cancelAllocation_activeAllocation_statusCancelled() {
        // Alias for deleteRoomAllocation
        deleteRoomAllocation_activeAllocation_statusCancelled();
    }

    // ── GH-223: an application that ends must not leave an offer standing ────────────────────

    private RoomAllocation outstandingAllocation(Long id, AllocationStatus status) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(status);
        allocation.setRoom(room);
        allocation.setStudent(student);
        allocation.setApplication(application);
        return allocation;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = AllocationStatus.class, names = {"OFFERED", "ACCEPTED"})
    void releaseOutstandingAllocations_cancelsTheAllocationAndFreesTheRoom(AllocationStatus held) {
        RoomAllocation allocation = outstandingAllocation(50L, held);
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(eq(7L), any()))
                .thenReturn(java.util.List.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int released = allocationLifecycleService.releaseOutstandingAllocations(7L, "application REJECTED");

        assertThat(released).isEqualTo(1);
        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.CANCELLED);
        assertThat(allocation.getEndDate()).isNotNull();
        // Without this the room stays counted as occupied until the hourly expiry sweep runs.
        verify(roomOccupancyManager).syncOccupancy(room);
        verify(allocationAuditService).logStatusChange(allocation, held, AllocationStatus.CANCELLED);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = AllocationStatus.class, names = {"OFFERED", "ACCEPTED"})
    void releaseOutstandingAllocations_publishesVacatedSoTheWaitingListRefills(AllocationStatus held) {
        RoomAllocation allocation = outstandingAllocation(50L, held);
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(eq(7L), any()))
                .thenReturn(java.util.List.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        allocationLifecycleService.releaseOutstandingAllocations(7L, "application REJECTED");

        ArgumentCaptor<RoomVacatedEvent> event = ArgumentCaptor.forClass(RoomVacatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().allocationId()).isEqualTo(50L);
        assertThat(event.getValue().roomId()).isEqualTo(room.getId());
        assertThat(event.getValue().newStatus()).isEqualTo(AllocationStatus.CANCELLED);
    }

    /**
     * The student is not put back on the waiting list, unlike a plain allocation cancellation.
     * The application itself is over, so re-queuing them would undo the admin's decision.
     */
    @Test
    void releaseOutstandingAllocations_doesNotRequeueTheStudent() {
        RoomAllocation allocation = outstandingAllocation(50L, AllocationStatus.OFFERED);
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(eq(7L), any()))
                .thenReturn(java.util.List.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        allocationLifecycleService.releaseOutstandingAllocations(7L, "application REJECTED");

        verify(waitingListService, never()).restoreAfterCancellation(any(), any());
    }

    @Test
    void releaseOutstandingAllocations_nothingOutstanding_doesNothing() {
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(eq(7L), any()))
                .thenReturn(java.util.List.of());

        assertThat(allocationLifecycleService.releaseOutstandingAllocations(7L, "application REJECTED"))
                .isZero();
        verify(roomOccupancyManager, never()).syncOccupancy(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * A checked-in student is living in the room. Rejecting the application is not grounds for
     * ending an occupancy behind their back; that needs a deliberate move-out.
     */
    @Test
    void releaseOutstandingAllocations_leavesACheckedInOccupancyAlone() {
        allocationLifecycleService.releaseOutstandingAllocations(7L, "application REJECTED");

        ArgumentCaptor<java.util.Collection<AllocationStatus>> statuses =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(roomAllocationRepository).findByApplicationIdAndStatusIn(eq(7L), statuses.capture());
        assertThat(statuses.getValue())
                .containsExactlyInAnyOrder(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED)
                .doesNotContain(AllocationStatus.ACTIVE);
    }

    // ── GH-198: a lapsed offer had no recovery route for anyone ──────────────────────────────

    private RoomAllocation offeredAllocation(java.time.LocalDateTime deadline) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(60L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setRoom(room);
        allocation.setStudent(student);
        allocation.setApplication(application);
        allocation.setOfferExpiryDate(deadline);
        allocation.setReminder24hSent(true);
        return allocation;
    }

    private void stubExtendable(RoomAllocation allocation) {
        when(roomAllocationRepository.findByIdWithDetails(60L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());
    }

    @Test
    void extendOfferDeadline_movesTheDeadlineAndRearmsTheReminder() {
        RoomAllocation allocation = offeredAllocation(java.time.LocalDateTime.now().minusDays(1));
        stubExtendable(allocation);
        java.time.LocalDateTime newDeadline = java.time.LocalDateTime.now().plusDays(3);

        allocationLifecycleService.extendOfferDeadline(60L, newDeadline, "Student was in hospital");

        assertThat(allocation.getOfferExpiryDate()).isEqualTo(newDeadline);
        // The 24h reminder already fired against the old deadline; without re-arming, the student
        // would get no warning before the new one.
        assertThat(allocation.isReminder24hSent()).isFalse();
    }

    /** An extension changes a decision the student was told about, so it is never anonymous. */
    @Test
    void extendOfferDeadline_isRecordedWithTheReasonAndBothDates() {
        java.time.LocalDateTime oldDeadline = java.time.LocalDateTime.now().minusDays(1);
        RoomAllocation allocation = offeredAllocation(oldDeadline);
        stubExtendable(allocation);

        allocationLifecycleService.extendOfferDeadline(
                60L, java.time.LocalDateTime.now().plusDays(3), "Student was in hospital");

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(allocationAuditService).logAuditAction(any(RoomAllocation.class),
                eq(com.beezhub.student_accommodation.model.enums.AllocationAuditActionType.OVERRIDE),
                any(), reason.capture(), any(), any());
        assertThat(reason.getValue())
                .contains("Student was in hospital")
                .contains("Offer deadline moved from");
    }

    @Test
    void extendOfferDeadline_tellsTheStudentTheDateMoved() {
        RoomAllocation allocation = offeredAllocation(java.time.LocalDateTime.now().minusDays(1));
        stubExtendable(allocation);

        allocationLifecycleService.extendOfferDeadline(
                60L, java.time.LocalDateTime.now().plusDays(3), "Student was in hospital");

        // Otherwise they keep acting on the old date, which is the opposite of the point.
        verify(userNotificationService).createOfferExtendedNotification(anyLong(), any(), any());
    }

    @Test
    void extendOfferDeadline_intoThePast_isRefused() {
        RoomAllocation allocation = offeredAllocation(java.time.LocalDateTime.now().minusDays(1));
        when(roomAllocationRepository.findByIdWithDetails(60L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationLifecycleService.extendOfferDeadline(
                60L, java.time.LocalDateTime.now().minusHours(1), "typo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    /**
     * Without a ceiling a mistyped year leaves an offer open for decades, holding a room out of
     * the pool the whole time.
     */
    @Test
    void extendOfferDeadline_beyondWhatANewOfferWouldGive_isRefused() {
        RoomAllocation allocation = offeredAllocation(java.time.LocalDateTime.now().minusDays(1));
        when(roomAllocationRepository.findByIdWithDetails(60L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationLifecycleService.extendOfferDeadline(
                60L, java.time.LocalDateTime.now().plusYears(1000), "fat finger"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days from now");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = AllocationStatus.class, names = {"ACCEPTED", "DECLINED", "ACTIVE", "CANCELLED"})
    void extendOfferDeadline_whenThereIsNoOutstandingOffer_isRefused(AllocationStatus status) {
        RoomAllocation allocation = offeredAllocation(java.time.LocalDateTime.now().minusDays(1));
        allocation.setStatus(status);
        when(roomAllocationRepository.findByIdWithDetails(60L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationLifecycleService.extendOfferDeadline(
                60L, java.time.LocalDateTime.now().plusDays(3), "too late"))
                .isInstanceOf(IllegalStateException.class);
    }

    /** The sweep has already released the room, and EXPIRED is terminal. Say so. */
    @Test
    void extendOfferDeadline_afterTheSweepExpiredIt_saysToAllocateAgain() {
        RoomAllocation allocation = offeredAllocation(java.time.LocalDateTime.now().minusDays(1));
        allocation.setStatus(AllocationStatus.EXPIRED);
        when(roomAllocationRepository.findByIdWithDetails(60L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationLifecycleService.extendOfferDeadline(
                60L, java.time.LocalDateTime.now().plusDays(3), "too late"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("allocate the student a room again");
    }
}
