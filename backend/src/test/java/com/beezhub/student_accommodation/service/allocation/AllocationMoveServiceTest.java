package com.beezhub.student_accommodation.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beezhub.student_accommodation.exceptions.AllocationConstraintViolationException;
import com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException;
import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import com.beezhub.student_accommodation.service.UserNotificationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

/**
 * GH-160: {@code AllocationMoveService} had no test at all, and moving a student is the one
 * allocation operation that touches two rooms' occupancy at once.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllocationMoveServiceTest {

    private static final Long ADMIN_ID = 99L;

    @Mock private RoomAllocationRepository roomAllocationRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomAllocationMapper roomAllocationMapper;
    @Mock private LegacyScoringStrategy allocationScoringService;
    @Mock private AppUserRepository appUserRepository;
    @Mock private AllocationAuditService allocationAuditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserNotificationService userNotificationService;
    @Mock private RoomOccupancyManager roomOccupancyManager;
    @Mock private com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    @InjectMocks private AllocationMoveService allocationMoveService;

    private Room oldRoom;
    private Room newRoom;
    private RoomAllocation allocation;

    @BeforeEach
    void setUp() {
        oldRoom = room(1L, "A101", 2);
        newRoom = room(2L, "B202", 2);

        AppUser appUser = new AppUser();
        appUser.setId(7L);
        Student student = new Student();
        student.setId(5L);
        student.setAppUser(appUser);

        allocation = new RoomAllocation();
        allocation.setId(10L);
        allocation.setRoom(oldRoom);
        allocation.setStudent(student);
        allocation.setStatus(AllocationStatus.ACTIVE);

        when(roomAllocationRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(allocation));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(newRoom));
        when(roomAllocationRepository.countActiveAllocationsByRoomId(2L)).thenReturn(0L);
        when(allocationScoringService.validateHardConstraints(any(), any(), anyBoolean()))
                .thenReturn(true);
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArgument(0));
        when(roomAllocationMapper.toResponse(any())).thenReturn(new RoomAllocationResponse());
        when(appUserRepository.findById(ADMIN_ID)).thenReturn(Optional.of(new AppUser()));
    }

    // ── The status rule ─────────────────────────────────────────────────────────────────────

    @Test
    void moveStudent_activeAllocation_movesToTheNewRoom() {
        allocationMoveService.moveStudent(10L, 2L, "Maintenance", ADMIN_ID);

        assertThat(allocation.getRoom()).isEqualTo(newRoom);
        verify(roomAllocationRepository).save(allocation);
    }

    /**
     * Only an ACTIVE allocation may be moved: anything earlier has no one in the room yet, and
     * anything later is finished. An OFFERED allocation moved here would silently re-point an
     * offer the student has not answered.
     */
    @ParameterizedTest
    @EnumSource(value = AllocationStatus.class, names = {"ACTIVE"}, mode = EnumSource.Mode.EXCLUDE)
    void moveStudent_nonActiveAllocation_isRefusedAndNothingChanges(AllocationStatus status) {
        allocation.setStatus(status);

        assertThatThrownBy(() -> allocationMoveService.moveStudent(10L, 2L, "Maintenance", ADMIN_ID))
                .isInstanceOf(InvalidAllocationStatusTransitionException.class)
                .hasMessageContaining("ACTIVE");

        assertThat(allocation.getRoom()).isEqualTo(oldRoom);
        verify(roomAllocationRepository, never()).save(any());
        verify(roomOccupancyManager, never()).syncOccupancy(any());
    }

    // ── The room rules ──────────────────────────────────────────────────────────────────────

    @Test
    void moveStudent_destinationAtCapacity_isRefused() {
        when(roomAllocationRepository.countActiveAllocationsByRoomId(2L)).thenReturn(2L);

        assertThatThrownBy(() -> allocationMoveService.moveStudent(10L, 2L, "Maintenance", ADMIN_ID))
                .isInstanceOf(RoomCapacityExceededException.class);

        assertThat(allocation.getRoom()).isEqualTo(oldRoom);
        verify(roomAllocationRepository, never()).save(any());
    }

    /** Accessibility and gender rules are hard constraints; a move must not be a way around them. */
    @Test
    void moveStudent_destinationFailsAHardConstraint_isRefused() {
        when(allocationScoringService.validateHardConstraints(any(), any(), anyBoolean()))
                .thenReturn(false);

        assertThatThrownBy(() -> allocationMoveService.moveStudent(10L, 2L, "Maintenance", ADMIN_ID))
                .isInstanceOf(AllocationConstraintViolationException.class);

        assertThat(allocation.getRoom()).isEqualTo(oldRoom);
    }

    @Test
    void moveStudent_unknownAllocation_isNotFound() {
        when(roomAllocationRepository.findByIdWithDetails(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allocationMoveService.moveStudent(404L, 2L, "x", ADMIN_ID))
                .isInstanceOf(RoomAllocationNotFoundException.class);
    }

    @Test
    void moveStudent_unknownDestinationRoom_isNotFound() {
        when(roomRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allocationMoveService.moveStudent(10L, 404L, "x", ADMIN_ID))
                .isInstanceOf(RoomNotFoundException.class);
    }

    // ── Occupancy ───────────────────────────────────────────────────────────────────────────

    /**
     * Both rooms are re-counted, and only after the allocation has been saved onto the new room.
     * Syncing before the save counts the allocation against the room it is leaving and not yet
     * against the one it is joining, which is the double-count GH-136 fixed.
     */
    @Test
    void moveStudent_recountsBothRoomsAfterTheSave() {
        allocationMoveService.moveStudent(10L, 2L, "Maintenance", ADMIN_ID);

        InOrder order = inOrder(roomAllocationRepository, roomOccupancyManager);
        order.verify(roomAllocationRepository).save(any(RoomAllocation.class));
        order.verify(roomOccupancyManager).syncOccupancy(oldRoom);
        order.verify(roomOccupancyManager).syncOccupancy(newRoom);
    }

    @Test
    void moveStudent_recordsTheMoveInTheAuditTrailWithBothRooms() {
        allocationMoveService.moveStudent(10L, 2L, "Roof leak", ADMIN_ID);

        verify(allocationAuditService).logAuditAction(
                any(), any(), any(), org.mockito.ArgumentMatchers.eq("Roof leak"),
                org.mockito.ArgumentMatchers.eq(oldRoom), org.mockito.ArgumentMatchers.eq(newRoom));
    }

    /** A notification failure must not undo a move that has already happened. */
    @Test
    void moveStudent_notificationFails_theMoveStillStands() {
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(userNotificationService)
                .createRoomMoveNotification(any(), any(), any(), any());

        allocationMoveService.moveStudent(10L, 2L, "Maintenance", ADMIN_ID);

        assertThat(allocation.getRoom()).isEqualTo(newRoom);
        verify(roomAllocationRepository).save(allocation);
    }

    private static Room room(Long id, String number, int capacity) {
        Room room = new Room();
        room.setId(id);
        room.setRoomNumber(number);
        room.setCapacity(capacity);
        return room;
    }
}
