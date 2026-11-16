package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.MessageResponse;
import com.beezhub.student_accommodation.model.dto.WaitlistPositionDto;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import com.beezhub.student_accommodation.model.event.AllocationDeclinedEvent;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WaitingListServiceTest {

    @Mock
    private WaitingListRepository waitingListRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AuthService authService;
    @Mock
    private AcademicYearService academicYearService;

    @Captor
    private ArgumentCaptor<AllocationDeclinedEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<WaitingListEntry> entryCaptor;

    @InjectMocks
    private WaitingListService waitingListService;

    @Test
    void closeEntriesForApplication_waitingEntry_cancelled() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setStatus("WAITING");
        when(waitingListRepository.findByApplication_IdAndStatus(10L, "WAITING"))
                .thenReturn(java.util.List.of(entry));

        waitingListService.closeEntriesForApplication(10L);

        assertThat(entry.getStatus()).isEqualTo("CANCELLED");
        verify(waitingListRepository).save(entry);
    }

    private Student student;
    private AppUser appUser;
    private Application application;
    private Room room;
    private RoomAllocation allocation;
    private WaitingListEntry waitingEntry;
    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        // Setup student
        student = new Student();
        student.setId(1L);
        student.setStudentNumber("STU001");
        appUser = new AppUser();
        appUser.setId(1L);
        appUser.setFirstName("John");
        appUser.setLastName("Doe");
        student.setAppUser(appUser);

        academicYear = AcademicYear.builder().id(9L).name("2026/2027").build();

        // Setup application
        application = new Application();
        application.setId(1L);
        application.setStudent(student);
        application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
        application.setApplicationCode("APP-001");
        application.setAcademicYear(academicYear);

        // Setup room
        room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");

        // Setup allocation
        allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);

        // Setup waiting list entry
        waitingEntry = new WaitingListEntry();
        waitingEntry.setId(1L);
        waitingEntry.setStudent(student);
        waitingEntry.setApplication(application);
        waitingEntry.setStatus("WAITING");
        waitingEntry.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void enqueueWaitingList_newStudent_added() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        MessageResponse response = waitingListService.enqueueWaitingList(1L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("added to waiting list");
        verify(waitingListRepository).save(any(WaitingListEntry.class));
    }

    /**
     * GH-146: the entry was left unstamped, so TenantListener copied the acting context, which is
     * 0 for both a platform admin and a student. The owning tenant could not then see the row
     * through any filtered query, so confirming an allocation left the student WAITING and the
     * already-allocated guard did not fire.
     */
    @Test
    void enqueueWaitingList_stampsTheTenantThatOwnsTheApplication() {
        application.setTenantId(7L);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);

        waitingListService.enqueueWaitingList(1L, 1L);

        verify(waitingListRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTenantId()).isEqualTo(7L);
    }

    /**
     * An application created by a platform admin carries the platform tenant itself, so the
     * building it was made to is what still identifies the owner. Same rule the batch allocator
     * uses when grouping entries by tenant.
     */
    @Test
    void enqueueWaitingList_platformTenantApplication_fallsBackToThePreferredBuilding() {
        application.setTenantId(0L);
        Building preferred = new Building();
        preferred.setId(3L);
        preferred.setTenantId(7L);
        application.setPreferredBuilding(preferred);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);

        waitingListService.enqueueWaitingList(1L, 1L);

        verify(waitingListRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTenantId()).isEqualTo(7L);
    }

    @Test
    void enqueueWaitingList_duplicateStudent_skipped() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(true);

        // Act
        MessageResponse response = waitingListService.enqueueWaitingList(1L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("already on the waiting list");
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    @Test
    void enqueueWaitingList_studentNotFound_throwsException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> waitingListService.enqueueWaitingList(1L, 1L))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void enqueueWaitingList_applicationNotFound_throwsException() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> waitingListService.enqueueWaitingList(1L, 1L))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void enqueueWaitingList_withEntities_createsEntry() {
        // Arrange
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        waitingListService.enqueueWaitingList(student, application);

        // Assert
        verify(waitingListRepository).save(any(WaitingListEntry.class));
    }

    @Test
    void updateStatusToAllocated_waitingEntry_updatesStatus() {
        // Arrange
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waitingEntry));
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        waitingListService.updateStatusToAllocated(1L);

        // Assert
        assertThat(waitingEntry.getStatus()).isEqualTo("ALLOCATED");
        verify(waitingListRepository).save(waitingEntry);
    }

    @Test
    void updateStatusToAllocated_noEntry_noOp() {
        // Arrange
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.empty());

        // Act
        waitingListService.updateStatusToAllocated(1L);

        // Assert
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    @Test
    void handleDeclineOutcome_firstDecline_addsToWaitingList() {
        // Arrange
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        waitingListService.handleDeclineOutcome(allocation, 1);

        // Assert
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        assertThat(application.getRemarks()).contains("Declined offer 1");
        assertThat(application.getRemarks()).doesNotContain("1 of ");
        verify(waitingListRepository).save(any(WaitingListEntry.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AllocationDeclinedEvent event = eventCaptor.getValue();
        assertThat(event.totalDeclineCount()).isEqualTo(1);
    }

    @Test
    void handleDeclineOutcome_secondDecline_updatesApplicationStatus() {
        // Arrange
        application.setStatus(ApplicationStatus.APPROVED);
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        waitingListService.handleDeclineOutcome(allocation, 2);

        // Assert
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(applicationRepository).save(application);
    }

    @Test
    void handleDeclineOutcome_noApplication_logsWarning() {
        // Arrange
        allocation.setApplication(null);

        // Act
        waitingListService.handleDeclineOutcome(allocation, 1);

        // Assert
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    // ── The decline limit (GH-160 recorded the gap; this is the decision) ────

    /**
     * The decline dialog has always told students the third decline closes their application.
     * Nothing enforced it — every decline re-queued, whatever the count — so the warning promised
     * something the system did not do, and one student could cycle through offers other applicants
     * were waiting for.
     */
    @Test
    void handleDeclineOutcome_thirdDecline_closesTheApplicationInsteadOfReQueueing() {
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        waitingListService.handleDeclineOutcome(allocation, WaitingListService.MAX_DECLINES);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(application.getRemarks()).contains("application closed");
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    /**
     * A WAITING entry left behind would have the batch allocator keep offering rooms to a student
     * whose application is closed — the orphaned-row shape GH-137 fixed for rejected applications.
     */
    @Test
    void handleDeclineOutcome_thirdDecline_takesTheStudentOffTheWaitingList() {
        WaitingListEntry waiting = new WaitingListEntry();
        waiting.setId(9L);
        waiting.setStatus("WAITING");
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waiting));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        waitingListService.handleDeclineOutcome(allocation, WaitingListService.MAX_DECLINES);

        assertThat(waiting.getStatus()).isEqualTo("CANCELLED");
        verify(waitingListRepository).save(waiting);
    }

    /** The notification and audit are owed whether or not the student goes back on the list. */
    @Test
    void handleDeclineOutcome_thirdDecline_stillPublishesTheDeclinedEvent() {
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        waitingListService.handleDeclineOutcome(allocation, WaitingListService.MAX_DECLINES);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().totalDeclineCount())
                .isEqualTo(WaitingListService.MAX_DECLINES);
    }

    /** Two is still within the allowance the dialog offers, so it re-queues as before. */
    @Test
    void handleDeclineOutcome_secondDecline_stillReQueues() {
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        waitingListService.handleDeclineOutcome(allocation, 2);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(waitingListRepository).save(any(WaitingListEntry.class));
    }

    /** A fourth decline cannot happen once the third closed the application, but is safe if it does. */
    @Test
    void handleDeclineOutcome_pastTheLimit_isStillClosedNotReQueued() {
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        waitingListService.handleDeclineOutcome(allocation, WaitingListService.MAX_DECLINES + 1);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    // ── getMyWaitlistPosition ────────────────────────────────────────────────

    @Test
    void getMyWaitlistPosition_studentOnWaitlist_returnsPositionAndQueueDepth() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waitingEntry));
        // 2 entries strictly before this student's createdAt -> position 3
        when(waitingListRepository.countWaitingInYearBefore(
                eq(9L), eq(waitingEntry.getCreatedAt()), any())).thenReturn(2L);
        when(waitingListRepository.countWaitingInYear(eq(9L), any())).thenReturn(12L);
        // Fewer than 5 recent allocations -> estimate unavailable
        when(roomAllocationRepository.countByAcademicYear_IdAndCreatedAtAfter(eq(9L), any(LocalDateTime.class)))
                .thenReturn(2L);

        WaitlistPositionDto result = waitingListService.getMyWaitlistPosition();

        assertThat(result.getPosition()).isEqualTo(3);
        assertThat(result.getTotalInQueue()).isEqualTo(12);
        assertThat(result.getEstimatedWaitDays()).isNull();
    }

    @Test
    void getMyWaitlistPosition_sufficientRecentAllocations_returnsEstimatedWaitDays() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waitingEntry));
        // Position 1 (no one ahead)
        when(waitingListRepository.countWaitingInYearBefore(
                eq(9L), any(LocalDateTime.class), any())).thenReturn(0L);
        when(waitingListRepository.countWaitingInYear(eq(9L), any())).thenReturn(1L);
        // 10 allocations in the last 30 days -> avg interval = 3 days -> position(1) * 3 = 3
        when(roomAllocationRepository.countByAcademicYear_IdAndCreatedAtAfter(eq(9L), any(LocalDateTime.class)))
                .thenReturn(10L);

        WaitlistPositionDto result = waitingListService.getMyWaitlistPosition();

        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(result.getEstimatedWaitDays()).isEqualTo(3);
    }

    /** GH-494: the queue is the student's own application's year, not one platform-wide year. */
    @Test
    void getMyWaitlistPosition_usesTheApplicationsOwnYear() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waitingEntry));

        waitingListService.getMyWaitlistPosition();

        verify(waitingListRepository).countWaitingInYear(eq(9L), any());
        verify(academicYearService, never()).findCurrentAcademicYear(any(), any());
    }

    @Test
    void getMyWaitlistPosition_applicationWithoutYear_fallsBackToTheCompanysCurrentYear() {
        application.setAcademicYear(null);
        waitingEntry.setTenantId(42L);
        AcademicYear current = AcademicYear.builder().id(11L).name("2027").build();
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waitingEntry));
        when(academicYearService.findCurrentAcademicYear(42L, null)).thenReturn(Optional.of(current));

        waitingListService.getMyWaitlistPosition();

        verify(waitingListRepository).countWaitingInYear(eq(11L), any());
    }

    @Test
    void getMyWaitlistPosition_noYearAtAll_throwsAcademicYearNotFound() {
        application.setAcademicYear(null);
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waitingEntry));
        when(academicYearService.findCurrentAcademicYear(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingListService.getMyWaitlistPosition())
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.AcademicYearNotFoundException.class);
    }

    @Test
    void getMyWaitlistPosition_studentNotOnWaitlist_throwsResourceNotFoundException() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingListService.getMyWaitlistPosition())
                .isInstanceOf(ResourceNotFoundException.class);

        verify(academicYearService, never()).findCurrentAcademicYear(any(), any());
    }

    @Test
    void getMyWaitlistPosition_studentProfileMissing_throwsStudentNotFoundException() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingListService.getMyWaitlistPosition())
                .isInstanceOf(StudentNotFoundException.class);
    }

    // ------------------------------------------------- GH-138 restore after cancellation

    private RoomAllocation cancelledAllocation(com.beezhub.student_accommodation.model.enums.AllocationStatus previous) {
        RoomAllocation cancelled = new RoomAllocation();
        cancelled.setId(9L);
        cancelled.setStudent(student);
        cancelled.setApplication(application);
        cancelled.setStatus(previous);
        return cancelled;
    }

    /**
     * Cancelling wrote nothing to the application, so it stayed ALLOCATED. That counts as an
     * active application, which blocked the student from re-applying to that tenant while the
     * cancelled allocation no longer held them a room.
     */
    @Test
    void restoreAfterCancellation_cancelledOffer_returnsTheApplicationToTheQueue() {
        application.setStatus(ApplicationStatus.ALLOCATED);

        waitingListService.restoreAfterCancellation(
                cancelledAllocation(com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED),
                com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(applicationRepository).save(application);
    }

    /**
     * Cancelling an ACTIVE allocation is the termination of a sitting resident, not a re-route.
     * Quietly putting them back on the queue would be wrong; the end endpoint is for that.
     */
    @Test
    void restoreAfterCancellation_previousStatusActive_leavesApplicationAndQueueAlone() {
        application.setStatus(ApplicationStatus.CHECKED_IN);

        waitingListService.restoreAfterCancellation(
                cancelledAllocation(com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE),
                com.beezhub.student_accommodation.model.enums.AllocationStatus.ACTIVE);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CHECKED_IN);
        verify(applicationRepository, never()).save(any(Application.class));
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
    }

    /**
     * A fresh arrival time, so the student re-enters at the back. Leaving the original would let
     * the after-commit refill hand them straight back the room just taken from them.
     */
    @Test
    void restoreAfterCancellation_existingEntry_reQueuesItAtTheBack() {
        application.setStatus(ApplicationStatus.ALLOCATED);
        WaitingListEntry allocated = new WaitingListEntry();
        allocated.setId(3L);
        allocated.setStudent(student);
        allocated.setStatus("ALLOCATED");
        allocated.setCreatedAt(LocalDateTime.now().minusDays(30));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "ALLOCATED"))
                .thenReturn(Optional.of(allocated));

        waitingListService.restoreAfterCancellation(
                cancelledAllocation(com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED),
                com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED);

        assertThat(allocated.getStatus()).isEqualTo("WAITING");
        assertThat(allocated.getCreatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    /** An allocation with no application has nothing to restore. */
    @Test
    void restoreAfterCancellation_noApplication_doesNothing() {
        RoomAllocation orphan = new RoomAllocation();
        orphan.setId(9L);
        orphan.setStudent(student);

        waitingListService.restoreAfterCancellation(orphan,
                com.beezhub.student_accommodation.model.enums.AllocationStatus.OFFERED);

        verify(applicationRepository, never()).save(any(Application.class));
    }
}
