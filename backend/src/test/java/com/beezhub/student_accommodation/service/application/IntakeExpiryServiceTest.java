package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.model.event.ApplicationStatusChangedEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GH-497: unfinished applications expire the day after their intake's stay ends. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IntakeExpiryServiceTest {

    /** 1 July 2027 in South Africa: Semester 1 ended on 30 June. */
    private static final LocalDate TODAY = LocalDate.of(2027, 7, 1);

    @Mock private ApplicationRepository applicationRepository;
    @Mock private RoomAllocationRepository roomAllocationRepository;
    @Mock private WaitingListService waitingListService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TransactionTemplate transactionTemplate;

    private IntakeExpiryService service;
    private final List<Long> tenantsSeen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 08:00 SAST on 1 July; still 30 June in some zones west of UTC, which must not matter.
        Clock clock = Clock.fixed(Instant.parse("2027-07-01T06:00:00Z"), ZoneId.of("Africa/Johannesburg"));
        service = new IntakeExpiryService(applicationRepository, roomAllocationRepository, waitingListService,
                eventPublisher, transactionTemplate, clock);
        when(transactionTemplate.execute(any())).thenAnswer(i ->
                ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static AcademicYear year() {
        return AcademicYear.builder().id(5L).name("2027")
                .startDate(LocalDate.of(2027, 2, 1)).endDate(LocalDate.of(2027, 11, 30)).build();
    }

    private Application application(long id, ApplicationStatus status, IntakeType type, String name,
            LocalDate stayStart, LocalDate stayEnd) {
        Intake intake = Intake.builder().id(id + 100).academicYear(year()).intakeType(type).name(name)
                .stayStartDate(stayStart).stayEndDate(stayEnd).build();
        Application application = Application.builder().id(id).tenantId(7L).status(status)
                .applicationCode("APP-" + id).academicYear(year()).intake(intake).build();
        when(applicationRepository.findWithDetailsById(id)).thenReturn(Optional.of(application));
        return application;
    }

    private Application semester1(long id, ApplicationStatus status) {
        return application(id, status, IntakeType.SEMESTER, "Semester 1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30));
    }

    @Test
    void expire_endedSemester1_eachUnfinishedStatusExpires() {
        for (ApplicationStatus status : IntakeExpiryService.UNFINISHED) {
            Application application = semester1(1L, status);

            assertThat(service.expire(1L, TODAY)).as(status.name()).isTrue();
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
            assertThat(application.getRemarks()).contains("30 June 2027");
        }
        verify(waitingListService, org.mockito.Mockito.times(IntakeExpiryService.UNFINISHED.size()))
                .closeEntriesForApplication(1L);
    }

    @Test
    void expire_endedSemester1_publishesTheStatusChangeForTheEmail() {
        semester1(1L, ApplicationStatus.UNDER_REVIEW);

        service.expire(1L, TODAY);

        ArgumentCaptor<ApplicationStatusChangedEvent> event = ArgumentCaptor.forClass(ApplicationStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().oldStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(event.getValue().newStatus()).isEqualTo(ApplicationStatus.EXPIRED);
        assertThat(event.getValue().application().getId()).isEqualTo(1L);
    }

    @Test
    void expire_onTheLastDayOfTheStay_isLeftAlone() {
        Application application = semester1(1L, ApplicationStatus.PENDING);

        assertThat(service.expire(1L, LocalDate.of(2027, 6, 30))).isFalse();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void expire_runningIntake_isLeftAlone() {
        Application application = application(2L, ApplicationStatus.PENDING, IntakeType.SEMESTER, "Semester 2",
                LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30));

        assertThat(service.expire(2L, TODAY)).isFalse();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(waitingListService, never()).closeEntriesForApplication(anyLong());
    }

    @Test
    void expire_annualIntake_expiresTheDayAfterTheYearEnds() {
        Application application = application(3L, ApplicationStatus.AWAITING_ALLOCATION, IntakeType.ANNUAL, "Annual",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));

        assertThat(service.expire(3L, LocalDate.of(2027, 11, 30))).isFalse();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.AWAITING_ALLOCATION);

        assertThat(service.expire(3L, LocalDate.of(2027, 12, 1))).isTrue();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
        verify(waitingListService).closeEntriesForApplication(3L);
    }

    @Test
    void expire_noIntake_usesTheYearEnd() {
        Application application = Application.builder().id(4L).tenantId(7L).status(ApplicationStatus.PENDING)
                .academicYear(year()).build();
        when(applicationRepository.findWithDetailsById(4L)).thenReturn(Optional.of(application));

        assertThat(service.expire(4L, LocalDate.of(2027, 12, 1))).isTrue();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
    }

    @Test
    void expire_noDatesAtAll_isLeftAlone() {
        Application application = Application.builder().id(4L).tenantId(7L).status(ApplicationStatus.PENDING).build();
        when(applicationRepository.findWithDetailsById(4L)).thenReturn(Optional.of(application));

        assertThat(service.expire(4L, TODAY)).isFalse();
    }

    @Test
    void expire_finishedOrAllocatedApplication_isLeftAlone() {
        for (ApplicationStatus status : List.of(ApplicationStatus.ALLOCATED, ApplicationStatus.CHECKED_IN,
                ApplicationStatus.REJECTED, ApplicationStatus.CANCELLED, ApplicationStatus.EXPIRED)) {
            Application application = semester1(1L, status);

            assertThat(service.expire(1L, TODAY)).as(status.name()).isFalse();
            assertThat(application.getStatus()).isEqualTo(status);
        }
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void expire_offerOutstanding_isLeftToOfferExpiry() {
        Application application = semester1(1L, ApplicationStatus.APPROVED);
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(eq(1L), any()))
                .thenReturn(List.of(new RoomAllocation()));

        assertThat(service.expire(1L, TODAY)).isFalse();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void expire_missingApplication_isLeftAlone() {
        when(applicationRepository.findWithDetailsById(9L)).thenReturn(Optional.empty());

        assertThat(service.expire(9L, TODAY)).isFalse();
    }

    @Test
    void runDaily_setsTheTenantPerCompanyAndRestoresTheCaller() {
        when(applicationRepository.findTenantIdsWithStayEndedBefore(IntakeExpiryService.UNFINISHED, TODAY))
                .thenReturn(Arrays.asList(7L, null, 8L));
        when(applicationRepository.findIdsWithStayEndedBefore(any(), eq(IntakeExpiryService.UNFINISHED), eq(TODAY)))
                .thenAnswer(i -> {
                    tenantsSeen.add(TenantContext.getTenantId());
                    return i.getArgument(0).equals(7L) ? List.of(1L) : List.of(2L);
                });
        semester1(1L, ApplicationStatus.PENDING);
        semester1(2L, ApplicationStatus.CHANGES_REQUESTED);
        TenantContext.setTenantId(3L);

        int expired = service.runDaily();

        assertThat(expired).isEqualTo(2);
        assertThat(tenantsSeen).containsExactly(7L, 8L);
        assertThat(TenantContext.getTenantId()).isEqualTo(3L);
    }

    @Test
    void runDaily_noCallerTenant_clearsTheContextAfterwards() {
        when(applicationRepository.findTenantIdsWithStayEndedBefore(any(), any())).thenReturn(List.of(7L));
        when(applicationRepository.findIdsWithStayEndedBefore(any(), any(), any())).thenReturn(List.of());

        assertThat(service.runDaily()).isZero();

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void runDaily_oneApplicationFails_theOthersStillExpire() {
        when(applicationRepository.findTenantIdsWithStayEndedBefore(any(), any())).thenReturn(List.of(7L));
        when(applicationRepository.findIdsWithStayEndedBefore(any(), any(), any())).thenReturn(List.of(1L, 2L));
        when(applicationRepository.findWithDetailsById(1L)).thenThrow(new IllegalStateException("db"));
        Application second = semester1(2L, ApplicationStatus.PENDING);

        assertThat(service.runDaily()).isEqualTo(1);

        assertThat(second.getStatus()).isEqualTo(ApplicationStatus.EXPIRED);
    }

    @Test
    void runDaily_queriesFailing_propagateForTheJobRunnerToCount() {
        when(applicationRepository.findTenantIdsWithStayEndedBefore(any(), any()))
                .thenThrow(new IllegalStateException("db down"));
        TenantContext.setTenantId(3L);

        assertThatThrownBy(() -> service.runDaily()).isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.getTenantId()).isEqualTo(3L);
    }
}
