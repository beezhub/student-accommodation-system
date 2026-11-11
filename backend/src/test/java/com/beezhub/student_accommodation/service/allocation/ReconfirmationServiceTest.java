package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.PlaceReleasedEvent;
import com.beezhub.student_accommodation.model.event.ReconfirmationRequestedEvent;
import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GH-496: "Still coming?" before move-in, reminders, and release of places not reconfirmed. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconfirmationServiceTest {

    /** 10 November 2026 in South Africa. */
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final ReconfirmationPolicy.LeadTimes DEFAULTS = new ReconfirmationPolicy.LeadTimes(42, 28, 14);

    @Mock private RoomAllocationRepository roomAllocationRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ReconfirmationPolicy reconfirmationPolicy;
    @Mock private RoomOccupancyManager roomOccupancyManager;
    @Mock private AllocationAuditService allocationAuditService;
    @Mock private WaitingListService waitingListService;
    @Mock private AllocationAccessGuard allocationAccessGuard;
    @Mock private RoomAllocationMapper roomAllocationMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TransactionTemplate transactionTemplate;

    private ReconfirmationService service;
    private final List<Long> tenantsSeen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T08:00:00Z"), ZoneId.of("Africa/Johannesburg"));
        service = new ReconfirmationService(roomAllocationRepository, applicationRepository, reconfirmationPolicy,
                roomOccupancyManager, allocationAuditService, waitingListService, allocationAccessGuard,
                roomAllocationMapper, eventPublisher, transactionTemplate, clock);
        when(transactionTemplate.execute(any())).thenAnswer(i ->
                ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null));
        when(reconfirmationPolicy.leadTimesFor(any())).thenAnswer(i -> {
            tenantsSeen.add(TenantContext.getTenantId());
            return DEFAULTS;
        });
        when(roomAllocationRepository.save(any(RoomAllocation.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private RoomAllocation accepted(long id, LocalDate moveIn, LocalDate acceptedOn) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        allocation.setTenantId(7L);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setStayStartDate(moveIn);
        allocation.setStayEndDate(moveIn.plusMonths(10));
        allocation.setAcceptedAt(acceptedOn.atTime(10, 0));
        Room room = new Room();
        room.setId(50L);
        allocation.setRoom(room);
        Application application = new Application();
        application.setId(90L);
        application.setStatus(ApplicationStatus.ALLOCATED);
        allocation.setApplication(application);
        when(roomAllocationRepository.findByIdWithDetails(id)).thenReturn(Optional.of(allocation));
        return allocation;
    }

    private ReconfirmationService.Step handle(long id) {
        return service.handle(id, DEFAULTS, TODAY);
    }

    @Test
    void handle_acceptedWellAhead_askedAtTheAskPoint() {
        // Move-in 1 December: asked from 3 November, released from 17 November.
        RoomAllocation allocation = accepted(1L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));

        assertThat(handle(1L)).isEqualTo(ReconfirmationService.Step.ASKED);

        assertThat(allocation.getReconfirmRequestedAt()).isNotNull();
        ArgumentCaptor<ReconfirmationRequestedEvent> event = ArgumentCaptor.forClass(ReconfirmationRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().reminder()).isFalse();
        assertThat(event.getValue().releaseDate()).isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test
    void handle_acceptedWithinTheMinimumLeadTime_neverAsked() {
        // Accepted 30 days before move-in, below the 42-day minimum.
        RoomAllocation allocation = accepted(2L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1));

        assertThat(handle(2L)).isEqualTo(ReconfirmationService.Step.NONE);

        assertThat(allocation.getReconfirmRequestedAt()).isNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_beforeTheAskPoint_nothingYet() {
        accepted(3L, LocalDate.of(2027, 2, 1), LocalDate.of(2026, 6, 15));

        assertThat(handle(3L)).isEqualTo(ReconfirmationService.Step.NONE);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_askedAWeekAgo_reminded() {
        RoomAllocation allocation = accepted(4L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));
        allocation.setReconfirmRequestedAt(LocalDateTime.of(2026, 11, 3, 8, 0));

        assertThat(handle(4L)).isEqualTo(ReconfirmationService.Step.REMINDED);

        assertThat(allocation.getReconfirmRemindedAt()).isNotNull();
        ArgumentCaptor<ReconfirmationRequestedEvent> event = ArgumentCaptor.forClass(ReconfirmationRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().reminder()).isTrue();
    }

    @Test
    void handle_askedYesterday_noReminderYet() {
        RoomAllocation allocation = accepted(5L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));
        allocation.setReconfirmRequestedAt(LocalDateTime.of(2026, 11, 9, 8, 0));

        assertThat(handle(5L)).isEqualTo(ReconfirmationService.Step.NONE);
    }

    @Test
    void handle_unconfirmedAtTheReleasePoint_releasedAndWaitingListRefilled() {
        // Move-in 20 November: released from 6 November.
        RoomAllocation allocation = accepted(6L, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 6, 15));
        allocation.setReconfirmRequestedAt(LocalDateTime.of(2026, 10, 23, 8, 0));

        assertThat(handle(6L)).isEqualTo(ReconfirmationService.Step.RELEASED);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.CANCELLED);
        assertThat(allocation.getEndDate()).isNotNull();
        assertThat(allocation.getApplication().getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        verify(roomOccupancyManager).syncOccupancy(allocation.getRoom());
        verify(allocationAuditService).logStatusChange(allocation, AllocationStatus.ACCEPTED, AllocationStatus.CANCELLED);
        verify(waitingListService).closeEntriesForApplication(90L);
        verify(eventPublisher).publishEvent(any(PlaceReleasedEvent.class));
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues()).anySatisfy(e -> {
            assertThat(e).isInstanceOf(RoomVacatedEvent.class);
            assertThat(((RoomVacatedEvent) e).roomId()).isEqualTo(50L);
            assertThat(((RoomVacatedEvent) e).newStatus()).isEqualTo(AllocationStatus.CANCELLED);
        });
    }

    @Test
    void handle_releasePointButNeverAsked_askedInsteadOfReleased() {
        RoomAllocation allocation = accepted(7L, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 6, 15));

        assertThat(handle(7L)).isEqualTo(ReconfirmationService.Step.ASKED);

        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
    }

    @Test
    void handle_askedToday_atTheReleasePoint_notReleasedTheSameDay() {
        RoomAllocation allocation = accepted(8L, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 6, 15));
        allocation.setReconfirmRequestedAt(LocalDateTime.of(2026, 11, 10, 8, 0));

        assertThat(handle(8L)).isEqualTo(ReconfirmationService.Step.NONE);
        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
    }

    @Test
    void handle_confirmed_keepsTheBed() {
        RoomAllocation allocation = accepted(9L, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 6, 15));
        allocation.setReconfirmRequestedAt(LocalDateTime.of(2026, 10, 23, 8, 0));
        allocation.setReconfirmedAt(LocalDateTime.of(2026, 10, 24, 8, 0));

        assertThat(handle(9L)).isEqualTo(ReconfirmationService.Step.NONE);
        assertThat(allocation.getStatus()).isEqualTo(AllocationStatus.ACCEPTED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_noLongerAccepted_skipped() {
        RoomAllocation allocation = accepted(10L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));
        allocation.setStatus(AllocationStatus.ACTIVE);

        assertThat(handle(10L)).isEqualTo(ReconfirmationService.Step.NONE);
    }

    @Test
    void runDaily_setsTheTenantContextPerCompanyAndRestoresIt() {
        when(roomAllocationRepository.findTenantIdsAwaitingReconfirmation(TODAY)).thenReturn(List.of(7L, 8L));
        when(roomAllocationRepository.findIdsAwaitingReconfirmation(7L, TODAY)).thenReturn(List.of(1L));
        when(roomAllocationRepository.findIdsAwaitingReconfirmation(8L, TODAY)).thenReturn(List.of());
        accepted(1L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));

        int processed = service.runDaily();

        assertThat(processed).isEqualTo(1);
        assertThat(tenantsSeen).containsExactly(7L, 8L);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void runDaily_oneFailure_othersStillProcessed() {
        when(roomAllocationRepository.findTenantIdsAwaitingReconfirmation(TODAY)).thenReturn(List.of(7L));
        when(roomAllocationRepository.findIdsAwaitingReconfirmation(7L, TODAY)).thenReturn(List.of(1L, 2L));
        when(roomAllocationRepository.findByIdWithDetails(1L)).thenThrow(new IllegalStateException("boom"));
        accepted(2L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));

        assertThat(service.runDaily()).isEqualTo(1);
    }

    @Test
    void reconfirm_ownAcceptedPlace_recorded() {
        RoomAllocation allocation = accepted(11L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));
        when(roomAllocationMapper.toResponse(allocation)).thenReturn(new RoomAllocationResponse());

        service.reconfirm(11L);

        assertThat(allocation.getReconfirmedAt()).isNotNull();
        verify(allocationAccessGuard).assertCanAccessAllocation(allocation);
    }

    @Test
    void reconfirm_anotherStudentsPlace_refusedAndNothingChanges() {
        RoomAllocation allocation = accepted(12L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));
        doThrow(new UnauthorizedAccessException("not yours")).when(allocationAccessGuard).assertCanAccessAllocation(allocation);

        assertThatThrownBy(() -> service.reconfirm(12L)).isInstanceOf(UnauthorizedAccessException.class);

        assertThat(allocation.getReconfirmedAt()).isNull();
        verify(roomAllocationRepository, never()).save(any());
    }

    @Test
    void reconfirm_notAccepted_refused() {
        RoomAllocation allocation = accepted(13L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));
        allocation.setStatus(AllocationStatus.OFFERED);

        assertThatThrownBy(() -> service.reconfirm(13L)).isInstanceOf(InvalidAllocationStatusTransitionException.class);
    }

    @Test
    void reconfirm_alreadyConfirmed_keepsTheFirstTime() {
        RoomAllocation allocation = accepted(14L, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 15));
        LocalDateTime first = LocalDateTime.of(2026, 11, 4, 9, 0);
        allocation.setReconfirmedAt(first);

        service.reconfirm(14L);

        assertThat(allocation.getReconfirmedAt()).isEqualTo(first);
        verify(roomAllocationRepository, never()).save(any());
    }
}
