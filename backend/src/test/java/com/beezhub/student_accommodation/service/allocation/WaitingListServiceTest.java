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
import com.beezhub.student_accommodation.service.NotificationService;
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

    @InjectMocks
    private WaitingListService waitingListService;

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

        academicYear = AcademicYear.builder().id(9L).name("2026/2027").isCurrent(true).build();

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
    void reQueueStudent_firstDecline_addsToWaitingList() {
        // Arrange
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(false);
        when(waitingListRepository.save(any(WaitingListEntry.class))).thenAnswer(i -> i.getArguments()[0]);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        waitingListService.reQueueStudent(allocation, 1);

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
    void reQueueStudent_updatesApplicationStatus() {
        // Arrange
        application.setStatus(ApplicationStatus.APPROVED);
        when(waitingListRepository.existsByStudent_IdAndStatus(1L, "WAITING")).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        waitingListService.reQueueStudent(allocation, 2);

        // Assert
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);
        verify(applicationRepository).save(application);
    }

    @Test
    void reQueueStudent_noApplication_logsWarning() {
        // Arrange
        allocation.setApplication(null);

        // Act
        waitingListService.reQueueStudent(allocation, 1);

        // Assert
        verify(waitingListRepository, never()).save(any(WaitingListEntry.class));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    // ── getMyWaitlistPosition ────────────────────────────────────────────────

    @Test
    void getMyWaitlistPosition_studentOnWaitlist_returnsPositionAndQueueDepth() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING"))
                .thenReturn(Optional.of(waitingEntry));
        when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
        // 2 entries strictly before this student's createdAt -> position 3
        when(waitingListRepository.countByStatusAndApplication_AcademicYear_IdAndCreatedAtLessThan(
                eq("WAITING"), eq(9L), eq(waitingEntry.getCreatedAt()))).thenReturn(2L);
        when(waitingListRepository.countByStatusAndApplication_AcademicYear_Id("WAITING", 9L)).thenReturn(12L);
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
        when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
        // Position 1 (no one ahead)
        when(waitingListRepository.countByStatusAndApplication_AcademicYear_IdAndCreatedAtLessThan(
                eq("WAITING"), eq(9L), any(LocalDateTime.class))).thenReturn(0L);
        when(waitingListRepository.countByStatusAndApplication_AcademicYear_Id("WAITING", 9L)).thenReturn(1L);
        // 10 allocations in the last 30 days -> avg interval = 3 days -> position(1) * 3 = 3
        when(roomAllocationRepository.countByAcademicYear_IdAndCreatedAtAfter(eq(9L), any(LocalDateTime.class)))
                .thenReturn(10L);

        WaitlistPositionDto result = waitingListService.getMyWaitlistPosition();

        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(result.getEstimatedWaitDays()).isEqualTo(3);
    }

    @Test
    void getMyWaitlistPosition_studentNotOnWaitlist_throwsResourceNotFoundException() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(waitingListRepository.findByStudent_IdAndStatus(1L, "WAITING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingListService.getMyWaitlistPosition())
                .isInstanceOf(ResourceNotFoundException.class);

        verify(academicYearService, never()).getCurrentAcademicYearEntity();
    }

    @Test
    void getMyWaitlistPosition_studentProfileMissing_throwsStudentNotFoundException() {
        when(authService.getCurrentUser()).thenReturn(appUser);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingListService.getMyWaitlistPosition())
                .isInstanceOf(StudentNotFoundException.class);
    }
}
