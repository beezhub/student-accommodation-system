package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException;
import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.UserNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationCheckInServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RoomAllocationMapper roomAllocationMapper;
    @Mock
    private UserNotificationService userNotificationService;
    @Mock
    private AllocationBatchService allocationBatchService;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private RoomOccupancyManager roomOccupancyManager;
    @Mock
    private AllocationAuditService allocationAuditService;
    @Mock
    private com.beezhub.student_accommodation.service.AuthService authService;
    @Mock
    private com.beezhub.student_accommodation.service.document.DocumentReadinessGuard documentReadinessGuard;

    @InjectMocks
    private AllocationCheckInService allocationCheckInService;

    @org.junit.jupiter.api.BeforeEach
    void useRealStayGuard() {
        org.springframework.test.util.ReflectionTestUtils.setField(allocationCheckInService, "stayCapacityGuard",
                new StayCapacityGuard(roomAllocationRepository, java.time.Clock.fixed(
                        java.time.Instant.parse("2026-11-10T08:00:00Z"), java.time.ZoneId.of("Africa/Johannesburg"))));
    }

    /** GH-496: the offer stands on the planned dates, but nobody moves into a bed still slept in. */
    @Test
    void checkIn_bedStillPhysicallyOccupied_refused() {
        com.beezhub.student_accommodation.model.entity.Room room = new com.beezhub.student_accommodation.model.entity.Room();
        room.setId(5L);
        room.setRoomNumber("12");
        room.setCapacity(1);
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(2L);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setRoom(room);
        allocation.setApplication(new Application());
        when(roomAllocationRepository.findByIdWithDetails(2L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.countByRoom_IdAndStatus(5L, AllocationStatus.ACTIVE)).thenReturn(1L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> allocationCheckInService.confirmCheckIn(2L, false))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.RoomStillOccupiedException.class)
                .hasMessageContaining("still occupied");

        org.mockito.Mockito.verify(roomAllocationRepository, org.mockito.Mockito.never()).save(any());
    }

    /** GH-498: an expired or pending required expiring document refuses the check-in. */
    @Test
    void checkIn_documentsNotReady_refusedAndNothingSaved() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(4L);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setApplication(new Application());
        when(roomAllocationRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(allocation));
        org.mockito.Mockito.doThrow(new com.beezhub.student_accommodation.exceptions.DocumentsNotReadyException(
                "The student cannot check in")).when(documentReadinessGuard).assertReadyForCheckIn(allocation);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> allocationCheckInService.confirmCheckIn(4L, true))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.DocumentsNotReadyException.class);

        org.mockito.Mockito.verify(roomAllocationRepository, org.mockito.Mockito.never()).save(any());
        org.assertj.core.api.Assertions.assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
    }

    @Test
    void checkIn_bedFreedByCheckOut_succeeds() {
        com.beezhub.student_accommodation.model.entity.Room room = new com.beezhub.student_accommodation.model.entity.Room();
        room.setId(5L);
        room.setRoomNumber("12");
        room.setCapacity(1);
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(3L);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setRoom(room);
        allocation.setApplication(new Application());
        when(roomAllocationRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.countByRoom_IdAndStatus(5L, AllocationStatus.ACTIVE)).thenReturn(0L);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);

        allocationCheckInService.confirmCheckIn(3L, false);

        org.assertj.core.api.Assertions.assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACTIVE);
    }

    @Test
    void checkIn_acceptedAllocation_statusActive() {
        // Arrange
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setApplication(new Application());
        
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        // Act
        allocationCheckInService.confirmCheckIn(id, false);

        // Assert
        assertEquals(AllocationStatus.ACTIVE, allocation.getStatus());
        assertNotNull(allocation.getCheckInDate());
        verify(applicationRepository).save(any(Application.class));
    }

    /**
     * GH-160: checking in on a status other than ACCEPTED had no test.
     *
     * <p>OFFERED is the case that matters: the student has not answered the offer yet, so checking
     * them in would put someone in a room they never agreed to and skip the accept step that
     * reserves it.
     */
    @ParameterizedTest
    @EnumSource(value = AllocationStatus.class, names = {"ACCEPTED"}, mode = EnumSource.Mode.EXCLUDE)
    void checkIn_fromAnyStatusButAccepted_isRefusedAndNothingChanges(AllocationStatus status) {
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(status);
        allocation.setApplication(new Application());
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));

        assertThrows(InvalidAllocationStatusTransitionException.class,
                () -> allocationCheckInService.confirmCheckIn(id, false));

        assertEquals(status, allocation.getStatus());
        assertNull(allocation.getCheckInDate());
        verify(roomAllocationRepository, never()).save(any());
    }

    /**
     * A second check-in is now refused (GH-160 recorded the gap; this is the decision).
     *
     * <p>{@code canTransitionTo} permits a self-transition, so ACTIVE -> ACTIVE used to pass and
     * the second attempt re-stamped {@code checkInDate} — losing the record of when the student
     * actually moved in, which is the one fact a check-in exists to record.
     */
    @Test
    void checkIn_onAnAlreadyActiveAllocation_isRefusedAndKeepsTheOriginalDate() {
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setApplication(new Application());
        java.time.LocalDateTime originalCheckIn = java.time.LocalDateTime.now().minusDays(30);
        allocation.setCheckInDate(originalCheckIn);
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));

        assertThrows(InvalidAllocationStatusTransitionException.class,
                () -> allocationCheckInService.confirmCheckIn(id, false));

        assertEquals(originalCheckIn, allocation.getCheckInDate());
        verify(roomAllocationRepository, never()).save(any());
    }

    /** The message says when, so staff can tell a double-click from a genuine mistake. */
    @Test
    void checkIn_onAnAlreadyActiveAllocation_saysWhenItHappened() {
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setApplication(new Application());
        allocation.setCheckInDate(java.time.LocalDateTime.of(2026, 2, 1, 9, 30));
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));

        InvalidAllocationStatusTransitionException thrown = assertThrows(
                InvalidAllocationStatusTransitionException.class,
                () -> allocationCheckInService.confirmCheckIn(id, false));

        assertTrue(thrown.getMessage().contains("already checked in"));
        assertTrue(thrown.getMessage().contains("2026-02-01"));
    }

    /** The override is for a late check-in, not for one that is not legal at all. */
    @Test
    void checkIn_onAnAlreadyActiveAllocation_isRefusedEvenWithTheOverride() {
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setApplication(new Application());
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));

        assertThrows(InvalidAllocationStatusTransitionException.class,
                () -> allocationCheckInService.confirmCheckIn(id, true));

        verify(roomAllocationRepository, never()).save(any());
    }

    /** The override is for a late check-in, not for a check-in that was never legal. */
    @Test
    void checkIn_onAnOfferedAllocation_isRefusedEvenWithTheOverride() {
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setApplication(new Application());
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));

        assertThrows(InvalidAllocationStatusTransitionException.class,
                () -> allocationCheckInService.confirmCheckIn(id, true));

        assertEquals(AllocationStatus.OFFERED, allocation.getStatus());
        verify(roomAllocationRepository, never()).save(any());
    }

    // ── GH-197: the check-in deadline is enforced here, not only by the hourly sweep ─────────

    private RoomAllocation acceptedAllocation(Long id, java.time.LocalDateTime checkInDeadline) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setApplication(new Application());
        allocation.setRoom(new Room());
        allocation.setCheckInExpiryDate(checkInDeadline);
        return allocation;
    }

    @Test
    void checkIn_pastTheDeadline_isRefusedAndSaysWhen() {
        // confirmCheckIn never read checkInExpiryDate, so the only thing enforcing the deadline was
        // the hourly sweep and an ACCEPTED allocation stayed checkable-in past it.
        java.time.LocalDateTime deadline = java.time.LocalDateTime.now().minusDays(2);
        RoomAllocation allocation = acceptedAllocation(1L, deadline);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> allocationCheckInService.confirmCheckIn(1L, false));

        assertTrue(thrown.getMessage().contains("check-in deadline"),
                "the message the frontend matches on to offer the override");
        assertTrue(thrown.getMessage().contains(String.valueOf(deadline.getYear())),
                "an admin should be told which deadline they are past");
        assertEquals(AllocationStatus.ACCEPTED, allocation.getStatus());
        verify(roomAllocationRepository, never()).save(any(RoomAllocation.class));
    }

    @Test
    void checkIn_pastTheDeadline_withOverride_succeeds() {
        // Admin-triggered, not student-triggered: checking someone in slightly late is a judgement
        // an admin may legitimately want to make. It must be deliberate, not silent.
        RoomAllocation allocation = acceptedAllocation(1L, java.time.LocalDateTime.now().minusDays(2));
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        allocationCheckInService.confirmCheckIn(1L, true);

        assertEquals(AllocationStatus.ACTIVE, allocation.getStatus());
    }

    @Test
    void checkIn_pastTheDeadline_withOverride_isRecordedInTheAuditTrail() {
        RoomAllocation allocation = acceptedAllocation(1L, java.time.LocalDateTime.now().minusDays(2));
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        allocationCheckInService.confirmCheckIn(1L, true);

        verify(allocationAuditService).logAuditActionWithConstraint(
                any(RoomAllocation.class),
                org.mockito.ArgumentMatchers.eq(
                        com.beezhub.student_accommodation.model.enums.AllocationAuditActionType.OVERRIDE),
                any(),
                org.mockito.ArgumentMatchers.contains("after the deadline"),
                any(),
                any(),
                org.mockito.ArgumentMatchers.eq(AllocationCheckInService.CHECK_IN_DEADLINE_CONSTRAINT));
    }

    @Test
    void checkIn_beforeTheDeadline_needsNoOverrideAndRecordsNothing() {
        RoomAllocation allocation = acceptedAllocation(1L, java.time.LocalDateTime.now().plusDays(2));
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        allocationCheckInService.confirmCheckIn(1L, false);

        assertEquals(AllocationStatus.ACTIVE, allocation.getStatus());
        verifyNoInteractions(allocationAuditService);
    }

    @Test
    void checkIn_withNoDeadlineRecorded_isTreatedAsHavingNone() {
        // A null deadline is equally invisible to the sweep, so the two agree on it.
        RoomAllocation allocation = acceptedAllocation(1L, null);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        allocationCheckInService.confirmCheckIn(1L, false);

        assertEquals(AllocationStatus.ACTIVE, allocation.getStatus());
    }

    @Test
    void moveOut_activeAllocation_statusCompleted() {
        // Arrange
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACTIVE);
        Room room = new Room();
        room.setCapacity(1);
        room.setCurrentOccupancy(1);
        allocation.setRoom(room);
        
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        // Act
        allocationCheckInService.confirmCheckOut(id);

        // Assert
        assertEquals(AllocationStatus.COMPLETED, allocation.getStatus());
        assertNotNull(allocation.getCheckOutDate());
        verify(roomOccupancyManager).syncOccupancy(room);
        // The event something actually listens for. AllocationFreedEvent had no listener, so
        // every check-out published into the void and the room was never refilled (GH-138).
        verify(eventPublisher).publishEvent(any(RoomVacatedEvent.class));
    }

    @Test
    void terminate_activeAllocation_statusTerminated() {
        // Arrange
        Long id = 1L;
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setStatus(AllocationStatus.ACTIVE);
        Room room = new Room();
        room.setCapacity(1);
        room.setCurrentOccupancy(1);
        allocation.setRoom(room);
        
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());

        // Act
        allocationCheckInService.endAllocation(id);

        // Assert
        // TERMINATED ("Lease cancelled early"), not COMPLETED ("Student moved out naturally").
        // Both methods wrote COMPLETED, which made TERMINATED unreachable despite the transition
        // being legal, and lost the distinction between the two (GH-138).
        assertEquals(AllocationStatus.TERMINATED, allocation.getStatus());
        assertNotNull(allocation.getEndDate());
        verify(roomOccupancyManager).syncOccupancy(room);
        verify(eventPublisher).publishEvent(any(RoomVacatedEvent.class));
    }

    /**
     * The status machine permits a self-transition, so a second close passed validation and ran
     * the whole method again, re-stamping the dates and publishing a second vacated event.
     */
    @Test
    void confirmCheckOut_alreadyCompleted_isRefused() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.COMPLETED);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        assertThrows(InvalidAllocationStatusTransitionException.class,
                () -> allocationCheckInService.confirmCheckOut(1L));

        verify(roomAllocationRepository, never()).save(any(RoomAllocation.class));
        verify(eventPublisher, never()).publishEvent(any(RoomVacatedEvent.class));
    }

    @Test
    void endAllocation_alreadyTerminated_isRefused() {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.TERMINATED);
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(allocation));

        assertThrows(InvalidAllocationStatusTransitionException.class,
                () -> allocationCheckInService.endAllocation(1L));

        verify(roomAllocationRepository, never()).save(any(RoomAllocation.class));
    }
}
