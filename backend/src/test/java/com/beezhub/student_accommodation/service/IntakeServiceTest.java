package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.InvalidAcademicCalendarException;
import com.beezhub.student_accommodation.exceptions.ResourceInUseException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.AcademicYearCopyRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearPatternRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.dto.IntakeRequest;
import com.beezhub.student_accommodation.model.dto.IntakeResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntakeServiceTest {

    private static final Long COMPANY = 42L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    @Mock private IntakeRepository intakeRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private RoomAllocationRepository roomAllocationRepository;
    @Mock private AcademicYearService academicYearService;
    @Mock private IntakeWindowPolicy intakeWindowPolicy;

    private IntakeService service;
    private AcademicYear year2027;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(COMPANY);
        service = new IntakeService(intakeRepository, academicYearRepository, applicationRepository,
                roomAllocationRepository, academicYearService, intakeWindowPolicy, new TenantOwnershipGuard());
        year2027 = AcademicYear.builder().id(5L).tenantId(COMPANY).name("2027")
                .startDate(LocalDate.of(2027, 2, 1)).endDate(LocalDate.of(2027, 11, 30))
                .applicationOpenDate(LocalDate.of(2026, 10, 4)).applicationCloseDate(LocalDate.of(2027, 1, 18))
                .build();
        lenient().when(intakeWindowPolicy.windowFor(anyLong(), any())).thenAnswer(inv -> {
            LocalDate start = inv.getArgument(1);
            return new IntakeWindowPolicy.Window(start.minusDays(120), start.minusDays(14));
        });
        lenient().when(academicYearService.toIntakeResponse(any())).thenAnswer(inv -> {
            Intake i = inv.getArgument(0);
            return IntakeResponse.builder().id(i.getId()).name(i.getName())
                    .applicationCloseDate(i.getApplicationCloseDate()).build();
        });
        lenient().when(academicYearService.toResponse(any())).thenAnswer(inv ->
                AcademicYearResponse.builder().name(((AcademicYear) inv.getArgument(0)).getName()).build());
        lenient().when(academicYearService.prepareNewYear(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(academicYearService.today()).thenReturn(TODAY);
        lenient().when(academicYearRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(intakeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Create from a pattern ───────────────────────────────────────────────────────────────

    @Test
    void createFromPattern_annualAndSemesters_fillsInTheIntakesAndTheirWindows() {
        AcademicYearPatternRequest request = AcademicYearPatternRequest.builder()
                .name("UX 2027").institutionId(7L)
                .startDate(LocalDate.of(2027, 2, 1)).endDate(LocalDate.of(2027, 11, 30))
                .intakeTypes(Set.of(IntakeType.SEMESTER, IntakeType.ANNUAL)).build();

        service.createFromPattern(request);

        List<Intake> intakes = savedIntakes();
        assertThat(intakes).extracting(Intake::getName).containsExactly("Annual", "Semester 1", "Semester 2");
        Intake annual = intakes.get(0);
        assertThat(annual.getStayStartDate()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(annual.getStayEndDate()).isEqualTo(LocalDate.of(2027, 11, 30));
        assertThat(annual.getApplicationOpenDate()).isEqualTo(LocalDate.of(2027, 2, 1).minusDays(120));
        assertThat(annual.getApplicationCloseDate()).isEqualTo(LocalDate.of(2027, 2, 1).minusDays(14));
        Intake s1 = intakes.get(1);
        Intake s2 = intakes.get(2);
        assertThat(s1.getStayEndDate().plusDays(1)).isEqualTo(s2.getStayStartDate());
        assertThat(s2.getStayEndDate()).isEqualTo(LocalDate.of(2027, 11, 30));
        assertThat(s2.getApplicationCloseDate()).isEqualTo(s2.getStayStartDate().minusDays(14));
        assertThat(intakes).allSatisfy(i -> assertThat(i.getTenantId()).isEqualTo(COMPANY));
        verify(academicYearService).prepareNewYear(any(), eq7());
        verify(academicYearService).validate(any());
    }

    @Test
    void createFromPattern_trimesters_coverTheYearInThreeParts() {
        AcademicYearPatternRequest request = AcademicYearPatternRequest.builder()
                .name("CY 2027").startDate(LocalDate.of(2027, 1, 15)).endDate(LocalDate.of(2027, 12, 10))
                .intakeTypes(Set.of(IntakeType.TRIMESTER)).build();

        service.createFromPattern(request);

        List<Intake> intakes = savedIntakes();
        assertThat(intakes).extracting(Intake::getName).containsExactly("Trimester 1", "Trimester 2", "Trimester 3");
        assertThat(intakes.get(0).getStayStartDate()).isEqualTo(LocalDate.of(2027, 1, 15));
        assertThat(intakes.get(2).getStayEndDate()).isEqualTo(LocalDate.of(2027, 12, 10));
    }

    @Test
    void createFromPattern_yearWindowCoversItsIntakes() {
        AcademicYearPatternRequest request = AcademicYearPatternRequest.builder()
                .name("2027").startDate(LocalDate.of(2027, 2, 1)).endDate(LocalDate.of(2027, 11, 30))
                .intakeTypes(Set.of(IntakeType.SEMESTER)).build();

        service.createFromPattern(request);

        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(captor.capture());
        List<Intake> intakes = savedIntakes();
        assertThat(captor.getValue().getApplicationOpenDate()).isEqualTo(intakes.get(0).getApplicationOpenDate());
        assertThat(captor.getValue().getApplicationCloseDate()).isEqualTo(intakes.get(1).getApplicationCloseDate());
    }

    @Test
    void createFromPattern_tooShortToSplit_isRefused() {
        AcademicYearPatternRequest request = AcademicYearPatternRequest.builder()
                .name("2027").startDate(LocalDate.of(2027, 2, 1)).endDate(LocalDate.of(2027, 2, 3))
                .intakeTypes(Set.of(IntakeType.TRIMESTER)).build();

        assertThatThrownBy(() -> service.createFromPattern(request))
                .isInstanceOf(InvalidAcademicCalendarException.class);
    }

    // ── Copy ────────────────────────────────────────────────────────────────────────────────

    @Test
    void copyYear_movesEveryDateOnByAYearAndNamesItTheNextYear() {
        when(academicYearService.loadOwned(5L)).thenReturn(year2027);
        when(intakeRepository.findByAcademicYear_IdOrderByStayStartDateAscNameAsc(5L)).thenReturn(List.of(
                intake(9L, "Semester 2", LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30))));

        AcademicYearResponse response = service.copyYear(5L, null);

        assertThat(response.getName()).isEqualTo("2028");
        Intake copied = savedIntakes().get(0);
        assertThat(copied.getId()).isNull();
        assertThat(copied.getStayStartDate()).isEqualTo(LocalDate.of(2028, 7, 15));
        assertThat(copied.getApplicationCloseDate()).isEqualTo(LocalDate.of(2028, 7, 1));
        ArgumentCaptor<AcademicYear> captor = ArgumentCaptor.forClass(AcademicYear.class);
        verify(academicYearRepository).save(captor.capture());
        assertThat(captor.getValue().getStartDate()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(captor.getValue().getTenantId()).isEqualTo(COMPANY);
    }

    @Test
    void copyYear_withAName_usesIt() {
        when(academicYearService.loadOwned(5L)).thenReturn(year2027);

        AcademicYearResponse response = service.copyYear(5L, new AcademicYearCopyRequest("Next one"));

        assertThat(response.getName()).isEqualTo("Next one");
    }

    @Test
    void nextName_movesEveryYearInTheNameOn() {
        assertThat(IntakeService.nextName("University X 2027")).isEqualTo("University X 2028");
        assertThat(IntakeService.nextName("2026/2027")).isEqualTo("2027/2028");
        assertThatThrownBy(() -> IntakeService.nextName("Main year"))
                .isInstanceOf(InvalidAcademicCalendarException.class);
    }

    // ── Create, edit, delete ────────────────────────────────────────────────────────────────

    @Test
    void createIntake_noWindowGiven_fillsItInAndWidensTheYearWindow() {
        when(academicYearService.loadOwned(5L)).thenReturn(year2027);
        IntakeRequest request = IntakeRequest.builder().intakeType(IntakeType.SEMESTER).name("Semester 2")
                .stayStartDate(LocalDate.of(2027, 7, 15)).stayEndDate(LocalDate.of(2027, 11, 30)).build();
        Intake existing = intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate());
        Intake added = intake(9L, "Semester 2", LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30));
        when(intakeRepository.findByAcademicYear_IdOrderByStayStartDateAscNameAsc(5L)).thenReturn(List.of(existing, added));

        service.createIntake(5L, request);

        ArgumentCaptor<Intake> captor = ArgumentCaptor.forClass(Intake.class);
        verify(intakeRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationOpenDate()).isEqualTo(LocalDate.of(2027, 3, 17));
        assertThat(captor.getValue().getApplicationCloseDate()).isEqualTo(LocalDate.of(2027, 7, 1));
        assertThat(captor.getValue().getTenantId()).isEqualTo(COMPANY);
        assertThat(year2027.getApplicationCloseDate()).isEqualTo(LocalDate.of(2027, 7, 1));
    }

    @Test
    void createIntake_outsideTheYear_isRefused() {
        when(academicYearService.loadOwned(5L)).thenReturn(year2027);
        IntakeRequest request = IntakeRequest.builder().intakeType(IntakeType.SEMESTER).name("Semester 2")
                .stayStartDate(LocalDate.of(2027, 7, 15)).stayEndDate(LocalDate.of(2027, 12, 31)).build();

        assertThatThrownBy(() -> service.createIntake(5L, request))
                .isInstanceOf(InvalidAcademicCalendarException.class)
                .hasMessageContaining("inside the year");
        verify(intakeRepository, never()).save(any());
    }

    @Test
    void createIntake_nameTaken_isRefused() {
        when(academicYearService.loadOwned(5L)).thenReturn(year2027);
        when(intakeRepository.existsByAcademicYear_IdAndName(5L, "Annual")).thenReturn(true);
        IntakeRequest request = IntakeRequest.builder().intakeType(IntakeType.ANNUAL).name("Annual")
                .stayStartDate(year2027.getStartDate()).stayEndDate(year2027.getEndDate()).build();

        assertThatThrownBy(() -> service.createIntake(5L, request)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createIntake_endsBeforeItStarts_isRefused() {
        when(academicYearService.loadOwned(5L)).thenReturn(year2027);
        IntakeRequest request = IntakeRequest.builder().intakeType(IntakeType.ANNUAL).name("Annual")
                .stayStartDate(LocalDate.of(2027, 5, 1)).stayEndDate(LocalDate.of(2027, 4, 1)).build();

        assertThatThrownBy(() -> service.createIntake(5L, request))
                .isInstanceOf(InvalidAcademicCalendarException.class);
    }

    @Test
    void updateIntake_returnsHowManyApplicationsAndAllocationsItHas() {
        Intake annual = intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate());
        when(intakeRepository.findById(8L)).thenReturn(Optional.of(annual));
        when(applicationRepository.countByAcademicYear_Id(5L)).thenReturn(3L);
        when(roomAllocationRepository.countByAcademicYear_Id(5L)).thenReturn(1L);
        IntakeRequest request = IntakeRequest.builder().intakeType(IntakeType.ANNUAL).name("Annual")
                .stayStartDate(year2027.getStartDate()).stayEndDate(year2027.getEndDate())
                .applicationOpenDate(LocalDate.of(2026, 9, 1)).applicationCloseDate(LocalDate.of(2027, 1, 31)).build();

        IntakeResponse response = service.updateIntake(8L, request);

        assertThat(response.getApplicationCount()).isEqualTo(3L);
        assertThat(response.getAllocationCount()).isEqualTo(1L);
        assertThat(annual.getApplicationOpenDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        verify(intakeRepository).existsByAcademicYear_IdAndNameAndIdNot(5L, "Annual", 8L);
    }

    @Test
    void updateIntake_anotherCompanysIntake_isNotFound() {
        Intake other = intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate());
        other.setTenantId(99L);
        when(intakeRepository.findById(8L)).thenReturn(Optional.of(other));
        IntakeRequest request = IntakeRequest.builder().intakeType(IntakeType.ANNUAL).name("Annual")
                .stayStartDate(year2027.getStartDate()).stayEndDate(year2027.getEndDate()).build();

        assertThatThrownBy(() -> service.updateIntake(8L, request)).isInstanceOf(ResourceNotFoundException.class);
        verify(intakeRepository, never()).save(any());
    }

    @Test
    void updateIntake_missing_isNotFound() {
        when(intakeRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteIntake(8L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteIntake_withApplications_isRefusedPointingAtCloseNow() {
        when(intakeRepository.findById(8L)).thenReturn(Optional.of(
                intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate())));
        when(applicationRepository.existsByAcademicYear_Id(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteIntake(8L))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("Close its applications now");
        verify(intakeRepository, never()).delete(any());
    }

    @Test
    void deleteIntake_withoutApplications_deletes() {
        Intake annual = intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate());
        when(intakeRepository.findById(8L)).thenReturn(Optional.of(annual));

        service.deleteIntake(8L);

        verify(intakeRepository).delete(annual);
    }

    // ── Close applications now ──────────────────────────────────────────────────────────────

    @Test
    void closeApplicationsNow_setsTheCloseDateToToday() {
        Intake annual = intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate());
        annual.setApplicationOpenDate(LocalDate.of(2026, 10, 1));
        annual.setApplicationCloseDate(LocalDate.of(2027, 1, 18));
        when(intakeRepository.findById(8L)).thenReturn(Optional.of(annual));
        when(intakeRepository.findByAcademicYear_IdOrderByStayStartDateAscNameAsc(5L)).thenReturn(List.of(annual));

        IntakeResponse response = service.closeApplicationsNow(8L);

        assertThat(response.getApplicationCloseDate()).isEqualTo(TODAY);
        assertThat(year2027.getApplicationCloseDate()).isEqualTo(TODAY);
    }

    @Test
    void closeApplicationsNow_windowNotOpenYet_isRefused() {
        Intake annual = intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate());
        annual.setApplicationOpenDate(TODAY.plusDays(5));
        annual.setApplicationCloseDate(TODAY.plusDays(50));
        when(intakeRepository.findById(8L)).thenReturn(Optional.of(annual));

        assertThatThrownBy(() -> service.closeApplicationsNow(8L))
                .isInstanceOf(InvalidAcademicCalendarException.class);
    }

    @Test
    void closeApplicationsNow_alreadyClosed_keepsTheEarlierDate() {
        Intake annual = intake(8L, "Annual", year2027.getStartDate(), year2027.getEndDate());
        annual.setApplicationOpenDate(TODAY.minusDays(50));
        annual.setApplicationCloseDate(TODAY.minusDays(5));
        when(intakeRepository.findById(8L)).thenReturn(Optional.of(annual));

        assertThat(service.closeApplicationsNow(8L).getApplicationCloseDate()).isEqualTo(TODAY.minusDays(5));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Intake> savedIntakes() {
        ArgumentCaptor<Iterable<Intake>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(intakeRepository).saveAll(captor.capture());
        List<Intake> intakes = new ArrayList<>();
        captor.getValue().forEach(intakes::add);
        return intakes;
    }

    private Intake intake(Long id, String name, LocalDate start, LocalDate end) {
        return Intake.builder().id(id).tenantId(COMPANY).academicYear(year2027).name(name)
                .intakeType(name.startsWith("Semester") ? IntakeType.SEMESTER : IntakeType.ANNUAL)
                .stayStartDate(start).stayEndDate(end)
                .applicationOpenDate(start.minusDays(120)).applicationCloseDate(start.minusDays(14)).build();
    }

    private static Long eq7() {
        return org.mockito.ArgumentMatchers.eq(7L);
    }
}
