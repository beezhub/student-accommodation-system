package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.StayExtensionRefusedException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.StayExtensionResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.model.event.StayExtendedEvent;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.DocumentIssue;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.Problem;
import com.beezhub.student_accommodation.service.document.DocumentReadinessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StayExtensionServiceTest {

    private static final LocalDate S1_START = LocalDate.of(2027, 2, 1);
    private static final LocalDate S1_END = LocalDate.of(2027, 6, 30);
    private static final LocalDate S2_START = LocalDate.of(2027, 7, 15);
    private static final LocalDate S2_END = LocalDate.of(2027, 11, 30);

    @Mock RoomAllocationRepository roomAllocationRepository;
    @Mock RoomRepository roomRepository;
    @Mock IntakeRepository intakeRepository;
    @Mock AllocationAccessGuard allocationAccessGuard;
    @Mock DocumentReadinessGuard documentReadinessGuard;
    @Mock RoomAllocationMapper roomAllocationMapper;
    @Mock ApplicationEventPublisher eventPublisher;

    private StayExtensionService service;
    private RoomAllocation allocation;
    private Intake semester1;
    private Intake semester2;
    private Room room;

    @BeforeEach
    void setUp() {
        StayCapacityGuard stayCapacityGuard = new StayCapacityGuard(roomAllocationRepository,
                Clock.fixed(Instant.parse("2027-05-10T08:00:00Z"), ZoneId.of("Africa/Johannesburg")));
        service = new StayExtensionService(roomAllocationRepository, roomRepository, intakeRepository,
                allocationAccessGuard, stayCapacityGuard, documentReadinessGuard, roomAllocationMapper, eventPublisher);

        AcademicYear year = AcademicYear.builder().id(5L).build();
        semester1 = intake(1L, "Semester 1", IntakeType.SEMESTER, S1_START, S1_END, year);
        semester2 = intake(2L, "Semester 2", IntakeType.SEMESTER, S2_START, S2_END, year);
        room = new Room();
        room.setId(9L);
        room.setRoomNumber("12");
        room.setCapacity(1);
        Student student = new Student();
        student.setId(10L);
        allocation = new RoomAllocation();
        allocation.setId(40L);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setRoom(room);
        allocation.setStudent(student);
        allocation.setApplication(Application.builder().id(30L).intake(semester1).build());
        allocation.setStayStartDate(S1_START);
        allocation.setStayEndDate(S1_END);

        lenient().when(roomAllocationRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(allocation));
        lenient().when(intakeRepository
                .findByAcademicYear_IdAndIntakeTypeAndStayStartDateGreaterThanEqualOrderByStayStartDateAsc(
                        5L, IntakeType.SEMESTER, S1_END)).thenReturn(List.of(semester2));
        lenient().when(documentReadinessGuard.issuesForStay(allocation)).thenReturn(List.of());
    }

    @Test
    void getOption_bedFreeAndDocumentsInOrder_allowedForTheNextIntake() {
        StayExtensionResponse option = service.getOption(40L);

        assertThat(option.getAllowed()).isTrue();
        assertThat(option.getNextIntakeName()).isEqualTo("Semester 2");
        assertThat(option.getNextStayEndDate()).isEqualTo(S2_END);
        assertThat(option.getCurrentStayEndDate()).isEqualTo(S1_END);
        // The bed is counted through the gap between the semesters, not from Semester 2's start.
        verify(roomAllocationRepository).countHoldingBeds(9L, S1_END, S2_END);
    }

    @Test
    void getOption_bedTaken_refusedButOffersTheNextIntakeToApplyFor() {
        when(roomAllocationRepository.countHoldingBeds(9L, S1_END, S2_END)).thenReturn(1L);

        StayExtensionResponse option = service.getOption(40L);

        assertThat(option.getAllowed()).isFalse();
        assertThat(option.getReason()).contains("not free for Semester 2");
        assertThat(option.getNextIntakeName()).isEqualTo("Semester 2");
    }

    @Test
    void getOption_expiredDocument_refused() {
        when(documentReadinessGuard.issuesForStay(allocation)).thenReturn(List.of(
                new DocumentIssue("Proof of Registration", Problem.EXPIRED, LocalDate.of(2026, 11, 30))));

        StayExtensionResponse option = service.getOption(40L);

        assertThat(option.getAllowed()).isFalse();
        assertThat(option.getReason()).contains("Proof of Registration (expired on 30 November 2026)");
    }

    @Test
    void getOption_annualStay_cannotBeExtended() {
        Intake annual = intake(3L, "Annual", IntakeType.ANNUAL, S1_START, S2_END, semester1.getAcademicYear());
        allocation.getApplication().setIntake(annual);

        StayExtensionResponse option = service.getOption(40L);

        assertThat(option.getAllowed()).isFalse();
        assertThat(option.getReason()).contains("Annual stay cannot be extended");
        assertThat(option.getNextIntakeName()).isNull();
    }

    @Test
    void getOption_lastIntakeOfTheYear_hasNothingToStayOnFor() {
        allocation.setStayEndDate(S2_END);

        StayExtensionResponse option = service.getOption(40L);

        assertThat(option.getAllowed()).isFalse();
        assertThat(option.getNextIntakeName()).isNull();
    }

    @Test
    void getOption_offerNotYetAccepted_refused() {
        allocation.setStatus(AllocationStatus.OFFERED);

        assertThat(service.getOption(40L).getAllowed()).isFalse();
    }

    @Test
    void getOption_alreadyHoldsAnotherBedForTheDates_refused() {
        when(roomAllocationRepository.existsLiveAllocationForStudentDuring(10L, S1_END, S2_END)).thenReturn(true);

        assertThat(service.getOption(40L).getReason()).contains("already have a place for Semester 2");
    }

    @Test
    void getOption_noIntakeOnThePlace_refused() {
        allocation.setApplication(null);

        assertThat(service.getOption(40L).getAllowed()).isFalse();
    }

    @Test
    void getOption_anotherStudent_isRefusedByTheGuard() {
        doThrow(new UnauthorizedAccessException("no")).when(allocationAccessGuard).assertCanAccessAllocation(allocation);

        assertThatThrownBy(() -> service.getOption(40L)).isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void extend_allowed_movesTheStayEndAtOnceThroughTheGap() {
        when(roomAllocationRepository.save(allocation)).thenReturn(allocation);
        when(roomAllocationMapper.toResponse(allocation)).thenReturn(new RoomAllocationResponse());

        service.extend(40L);

        assertThat(allocation.getStayEndDate()).isEqualTo(S2_END);
        assertThat(allocation.getStayStartDate()).isEqualTo(S1_START);
        assertThat(allocation.getRoom()).isSameAs(room);
        ArgumentCaptor<StayExtendedEvent> event = ArgumentCaptor.forClass(StayExtendedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new StayExtendedEvent(40L, "Semester 2", S1_END, S2_END));
    }

    @Test
    void extend_locksTheRoomBeforeCheckingAgain() {
        when(roomAllocationRepository.save(allocation)).thenReturn(allocation);

        service.extend(40L);

        var order = inOrder(roomRepository, roomAllocationRepository);
        order.verify(roomRepository).findByIdForUpdate(9L);
        order.verify(roomAllocationRepository).countHoldingBeds(eq(9L), any(), any());
    }

    @Test
    void extend_bedTakenSincePageLoaded_refusedAndNothingSaved() {
        when(roomAllocationRepository.countHoldingBeds(9L, S1_END, S2_END)).thenReturn(1L);

        assertThatThrownBy(() -> service.extend(40L))
                .isInstanceOf(StayExtensionRefusedException.class)
                .hasMessageContaining("not free for Semester 2");
        assertThat(allocation.getStayEndDate()).isEqualTo(S1_END);
        verify(roomAllocationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void extend_nextIntakeAlreadyOver_refused() {
        service = new StayExtensionService(roomAllocationRepository, roomRepository, intakeRepository,
                allocationAccessGuard, new StayCapacityGuard(roomAllocationRepository,
                        Clock.fixed(Instant.parse("2027-12-10T08:00:00Z"), ZoneId.of("Africa/Johannesburg"))),
                documentReadinessGuard, roomAllocationMapper, eventPublisher);

        assertThatThrownBy(() -> service.extend(40L)).hasMessageContaining("has already ended");
    }

    private static Intake intake(Long id, String name, IntakeType type, LocalDate start, LocalDate end,
            AcademicYear year) {
        return Intake.builder().id(id).name(name).intakeType(type).stayStartDate(start).stayEndDate(end)
                .academicYear(year).build();
    }
}
