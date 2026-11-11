package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.ApplicationsWithdrawnEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.RoomAllocationService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GH-497: accepting a place withdraws the student's other applications for overlapping dates. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OverlappingApplicationWithdrawalServiceTest {

    private static final long STUDENT = 1L;
    private static final long ACCEPTED_ALLOCATION = 50L;

    @Mock private ApplicationRepository applicationRepository;
    @Mock private RoomAllocationRepository roomAllocationRepository;
    @Mock private RoomAllocationService roomAllocationService;
    @Mock private WaitingListService waitingListService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private TransactionTemplate transactionTemplate;

    private OverlappingApplicationWithdrawalService service;
    private final List<Application> studentApplications = new ArrayList<>();
    private final List<Long> tenantsSeen = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new OverlappingApplicationWithdrawalService(applicationRepository, roomAllocationRepository,
                roomAllocationService, waitingListService, eventPublisher, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            tenantsSeen.add(TenantContext.getTenantId());
            return ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null);
        });
        doAnswer(i -> {
            ((Consumer<Object>) i.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        when(applicationRepository.findByStudent_IdAndStatusInOrderByIdDesc(eq(STUDENT), any()))
                .thenReturn(studentApplications);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static Student student(long id) {
        Student student = new Student();
        student.setId(id);
        return student;
    }

    private static Intake intake(String name, LocalDate start, LocalDate end) {
        AcademicYear year = AcademicYear.builder().id(5L).name("2027").build();
        return Intake.builder().id(6L).name(name).academicYear(year).stayStartDate(start).stayEndDate(end).build();
    }

    private static Intake annual2027() {
        return intake("Annual", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
    }

    private Application application(long id, long tenantId, long studentId, ApplicationStatus status, Intake intake) {
        Application application = Application.builder().id(id).tenantId(tenantId).student(student(studentId))
                .status(status).applicationCode("APP-" + id).intake(intake)
                .tenant(Tenant.builder().id(tenantId).name("Company " + tenantId).build()).build();
        when(applicationRepository.findWithDetailsById(id)).thenReturn(Optional.of(application));
        if (studentId == STUDENT) {
            studentApplications.add(application);
        }
        return application;
    }

    private RoomAllocation accepted(Application application) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(ACCEPTED_ALLOCATION);
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setStudent(student(STUDENT));
        allocation.setApplication(application);
        allocation.setStayStartDate(LocalDate.of(2027, 2, 1));
        allocation.setStayEndDate(LocalDate.of(2027, 11, 30));
        when(roomAllocationRepository.findByIdWithDetails(ACCEPTED_ALLOCATION)).thenReturn(Optional.of(allocation));
        return allocation;
    }

    @Test
    void withdrawOverlapping_overlappingAtTwoOtherCompanies_bothCancelledWithTheirOffers() {
        Application own = application(10L, 7L, STUDENT, ApplicationStatus.ALLOCATED, annual2027());
        accepted(own);
        Application atB = application(11L, 8L, STUDENT, ApplicationStatus.UNDER_REVIEW,
                intake("Semester 1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30)));
        Application atC = application(12L, 9L, STUDENT, ApplicationStatus.APPROVED, annual2027());
        when(roomAllocationService.releaseOutstandingAllocations(eq(12L), any())).thenReturn(1);
        TenantContext.setTenantId(3L);

        int withdrawn = service.withdrawOverlapping(ACCEPTED_ALLOCATION);

        assertThat(withdrawn).isEqualTo(2);
        assertThat(atB.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(atC.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(atC.getRemarks()).isEqualTo(OverlappingApplicationWithdrawalService.REMARK);
        assertThat(own.getStatus()).isEqualTo(ApplicationStatus.ALLOCATED);
        verify(roomAllocationService).releaseOutstandingAllocations(eq(11L), any());
        verify(roomAllocationService).releaseOutstandingAllocations(eq(12L), any());
        verify(waitingListService).closeEntriesForApplication(11L);
        verify(waitingListService).closeEntriesForApplication(12L);
        // Found as the platform, then each application under its own company.
        assertThat(tenantsSeen).containsExactly(0L, 8L, 9L);
        assertThat(TenantContext.getTenantId()).isEqualTo(3L);

        ArgumentCaptor<ApplicationsWithdrawnEvent> event = ArgumentCaptor.forClass(ApplicationsWithdrawnEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().studentId()).isEqualTo(STUDENT);
        assertThat(event.getValue().acceptedAllocationId()).isEqualTo(ACCEPTED_ALLOCATION);
        assertThat(event.getValue().withdrawn())
                .extracting(ApplicationsWithdrawnEvent.Withdrawn::applicationCode,
                        ApplicationsWithdrawnEvent.Withdrawn::companyName,
                        ApplicationsWithdrawnEvent.Withdrawn::intakeName,
                        ApplicationsWithdrawnEvent.Withdrawn::offersCancelled)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("APP-11", "Company 8", "Semester 1, 2027", 0),
                        org.assertj.core.groups.Tuple.tuple("APP-12", "Company 9", "Annual, 2027", 1));
    }

    @Test
    void withdrawOverlapping_nonOverlappingApplication_isKept() {
        accepted(application(10L, 7L, STUDENT, ApplicationStatus.ALLOCATED, annual2027()));
        Application nextYear = application(13L, 8L, STUDENT, ApplicationStatus.PENDING,
                intake("Annual", LocalDate.of(2028, 2, 1), LocalDate.of(2028, 11, 30)));
        Application backToBack = application(14L, 9L, STUDENT, ApplicationStatus.PENDING,
                intake("Summer", LocalDate.of(2027, 11, 30), LocalDate.of(2028, 1, 31)));

        assertThat(service.withdrawOverlapping(ACCEPTED_ALLOCATION)).isZero();

        assertThat(nextYear.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(backToBack.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void withdraw_anotherStudentsApplication_isNeverTouched() {
        Application other = application(20L, 8L, 2L, ApplicationStatus.PENDING, annual2027());

        assertThat(service.withdraw(20L, STUDENT)).isNull();

        assertThat(other.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(roomAllocationService, never()).releaseOutstandingAllocations(anyLong(), any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void withdrawOverlapping_selectsByTheStudentsOwnIdOnly() {
        accepted(application(10L, 7L, STUDENT, ApplicationStatus.ALLOCATED, annual2027()));

        service.withdrawOverlapping(ACCEPTED_ALLOCATION);

        verify(applicationRepository).findByStudent_IdAndStatusInOrderByIdDesc(
                STUDENT, OverlappingApplicationWithdrawalService.WITHDRAWABLE);
    }

    @Test
    void withdraw_applicationHoldingAPlace_isKept() {
        Application held = application(15L, 8L, STUDENT, ApplicationStatus.APPROVED, annual2027());
        when(roomAllocationRepository.findByApplicationIdAndStatusIn(eq(15L), any()))
                .thenReturn(List.of(new RoomAllocation()));

        assertThat(service.withdraw(15L, STUDENT)).isNull();

        assertThat(held.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void withdraw_statusChangedSinceItWasFound_isKept() {
        Application rejected = application(16L, 8L, STUDENT, ApplicationStatus.REJECTED, annual2027());

        assertThat(service.withdraw(16L, STUDENT)).isNull();
        assertThat(service.withdraw(99L, STUDENT)).isNull();

        assertThat(rejected.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void withdraw_eachWithdrawableStatus_isCancelled() {
        for (ApplicationStatus status : OverlappingApplicationWithdrawalService.WITHDRAWABLE) {
            Application application = application(17L, 8L, STUDENT, status, annual2027());

            assertThat(service.withdraw(17L, STUDENT)).as(status.name()).isNotNull();
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }
    }

    @Test
    void withdrawOverlapping_oneFails_theOthersAreStillWithdrawn() {
        accepted(application(10L, 7L, STUDENT, ApplicationStatus.ALLOCATED, annual2027()));
        application(11L, 8L, STUDENT, ApplicationStatus.PENDING, annual2027());
        Application atC = application(12L, 9L, STUDENT, ApplicationStatus.PENDING, annual2027());
        when(roomAllocationService.releaseOutstandingAllocations(eq(11L), any()))
                .thenThrow(new IllegalStateException("lock"));

        assertThat(service.withdrawOverlapping(ACCEPTED_ALLOCATION)).isEqualTo(1);

        assertThat(atC.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void withdrawOverlapping_allocationNoLongerAccepted_withdrawsNothing() {
        RoomAllocation allocation = accepted(application(10L, 7L, STUDENT, ApplicationStatus.ALLOCATED, annual2027()));
        allocation.setStatus(AllocationStatus.CANCELLED);
        Application atB = application(11L, 8L, STUDENT, ApplicationStatus.PENDING, annual2027());

        assertThat(service.withdrawOverlapping(ACCEPTED_ALLOCATION)).isZero();
        when(roomAllocationRepository.findByIdWithDetails(ACCEPTED_ALLOCATION)).thenReturn(Optional.empty());
        assertThat(service.withdrawOverlapping(ACCEPTED_ALLOCATION)).isZero();

        assertThat(atB.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void withdrawOverlapping_acceptedPlaceWithNoDates_withdrawsNothing() {
        RoomAllocation allocation = accepted(application(10L, 7L, STUDENT, ApplicationStatus.ALLOCATED, null));
        allocation.setStayStartDate(null);
        allocation.setStayEndDate(null);
        Application atB = application(11L, 8L, STUDENT, ApplicationStatus.PENDING, annual2027());

        assertThat(service.withdrawOverlapping(ACCEPTED_ALLOCATION)).isZero();

        assertThat(atB.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void withdrawOverlapping_otherApplicationWithNoDates_isKept() {
        accepted(application(10L, 7L, STUDENT, ApplicationStatus.ALLOCATED, annual2027()));
        Application undated = application(11L, 8L, STUDENT, ApplicationStatus.PENDING, null);

        assertThat(service.withdrawOverlapping(ACCEPTED_ALLOCATION)).isZero();

        assertThat(undated.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }
}
