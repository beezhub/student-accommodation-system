package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.ApplicationWindowClosedException;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** GH-495: which intakes of a company a student can apply for, and the submit checks per company. */
@ExtendWith(MockitoExtension.class)
class CompanyIntakePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);
    private static final Long COMPANY = 42L;
    private static final Long UNIVERSITY_X = 7L;
    private static final Long COLLEGE_Y = 8L;

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private IntakeRepository intakeRepository;
    @Mock private AcademicYearService academicYearService;
    @Mock private DocumentTypeService documentTypeService;

    @InjectMocks private CompanyIntakePolicy policy;

    private final Tenant company = Tenant.builder().id(COMPANY).name("Company B").build();
    private AcademicYear allInstitutions2026;
    private AcademicYear universityX2027;
    private AcademicYear collegeY2027;

    @BeforeEach
    void setUp() {
        lenient().when(academicYearService.today()).thenReturn(TODAY);
        lenient().when(academicYearService.isOpenToday(any(), any())).thenAnswer(inv -> {
            LocalDate open = inv.getArgument(0);
            LocalDate close = inv.getArgument(1);
            return !TODAY.isBefore(open) && !TODAY.isAfter(close);
        });
        allInstitutions2026 = year(1L, null, "2026", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
        universityX2027 = year(2L, UNIVERSITY_X, "2027", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        collegeY2027 = year(3L, COLLEGE_Y, "2027", LocalDate.of(2027, 1, 15), LocalDate.of(2027, 12, 10));
    }

    // ── Applicable years ───────────────────────────────────────────────────────

    @Test
    void applicableYears_keepsTheInstitutionsOwnAndAllInstitutionsYears() {
        stubYears(allInstitutions2026, universityX2027, collegeY2027);

        assertThat(policy.applicableYears(COMPANY, UNIVERSITY_X)).containsExactly(allInstitutions2026, universityX2027);
    }

    @Test
    void applicableYears_studentWithoutInstitution_seesOnlyAllInstitutionsYears() {
        stubYears(allInstitutions2026, universityX2027);

        assertThat(policy.applicableYears(COMPANY, null)).containsExactly(allInstitutions2026);
    }

    /** A year for the institution takes priority over an "All institutions" year it overlaps. */
    @Test
    void applicableYears_institutionsYearHidesAnOverlappingAllInstitutionsYear() {
        AcademicYear universityX2026 = year(4L, UNIVERSITY_X, "UX 2026", LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 12, 1));
        stubYears(allInstitutions2026, universityX2026, universityX2027);

        assertThat(policy.applicableYears(COMPANY, UNIVERSITY_X)).containsExactly(universityX2026, universityX2027);
    }

    // ── Open intakes ───────────────────────────────────────────────────────────

    @Test
    void openIntakes_onlyThoseWhoseWindowIsOpenToday() {
        Intake open = intake(10L, universityX2027, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        Intake notYet = intake(11L, universityX2027, "Semester 2", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 6, 30));
        stubYears(universityX2027);
        stubIntakes(open, notYet);

        assertThat(policy.openIntakes(COMPANY, UNIVERSITY_X)).containsExactly(open);
    }

    @Test
    void openIntakes_companyWithNoYearForTheInstitution_isEmpty() {
        stubYears(collegeY2027);

        assertThat(policy.openIntakes(COMPANY, UNIVERSITY_X)).isEmpty();
    }

    @Test
    void nextToOpen_earliestFutureWindowOutsideTheExcludedYears() {
        Intake later = intake(11L, universityX2027, "Semester 2", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 6, 30));
        Intake sooner = intake(12L, universityX2027, "Annual", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        Intake past = intake(13L, allInstitutions2026, "Annual", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 15));
        stubYears(allInstitutions2026, universityX2027);
        stubIntakes(past, sooner, later);

        assertThat(policy.nextToOpen(COMPANY, UNIVERSITY_X, Set.of())).contains(sooner);
        assertThat(policy.nextToOpen(COMPANY, UNIVERSITY_X, Set.of(universityX2027.getId()))).isEmpty();
    }

    // ── Resolving the intake on submit ─────────────────────────────────────────

    @Test
    void resolveIntake_requestedOpenIntakeOfTheCompany_isUsed() {
        Intake annual = intake(10L, universityX2027, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(annual));

        assertThat(policy.resolveIntake(company, 10L, UNIVERSITY_X)).isEqualTo(annual);
    }

    @Test
    void resolveIntake_intakeOfAnotherCompany_isRefusedNamingTheCompany() {
        AcademicYear otherCompanyYear = year(9L, null, "2027", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 1));
        otherCompanyYear.setTenantId(99L);
        Intake foreign = intake(10L, otherCompanyYear, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> policy.resolveIntake(company, 10L, UNIVERSITY_X))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company B");
    }

    @Test
    void resolveIntake_unknownIntake_isRefusedTheSameWay() {
        when(intakeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.resolveIntake(company, 10L, UNIVERSITY_X))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not one of theirs");
    }

    @Test
    void resolveIntake_intakeOfAnotherInstitution_isRefused() {
        Intake college = intake(10L, collegeY2027, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(college));

        assertThatThrownBy(() -> policy.resolveIntake(company, 10L, UNIVERSITY_X))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company B")
                .hasMessageContaining("another institution");
    }

    @Test
    void resolveIntake_allInstitutionsIntake_isOpenToEveryInstitution() {
        Intake annual = intake(10L, allInstitutions2026, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(annual));

        assertThat(policy.resolveIntake(company, 10L, COLLEGE_Y)).isEqualTo(annual);
    }

    @Test
    void resolveIntake_windowNotOpenYet_saysWhenItOpens() {
        Intake later = intake(10L, universityX2027, "Semester 2", LocalDate.of(2027, 5, 1), LocalDate.of(2027, 6, 30));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(later));

        assertThatThrownBy(() -> policy.resolveIntake(company, 10L, UNIVERSITY_X))
                .isInstanceOf(ApplicationWindowClosedException.class)
                .hasMessage("Company B opens applications for Semester 2, 2027 on 1 May 2027.");
    }

    @Test
    void resolveIntake_windowClosed_offersAnotherOpenIntake() {
        Intake closed = intake(10L, allInstitutions2026, "Annual", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 31));
        Intake open = intake(11L, universityX2027, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(closed));
        stubYears(allInstitutions2026, universityX2027);
        stubIntakes(closed, open);

        assertThatThrownBy(() -> policy.resolveIntake(company, 10L, UNIVERSITY_X))
                .isInstanceOf(ApplicationWindowClosedException.class)
                .hasMessage("Applications to Company B for Annual, 2026 closed on 31 January 2026."
                        + " You can apply for Annual, 2027 instead.");
    }

    @Test
    void resolveIntake_windowClosedAndNothingOpen_saysWhenTheNextOpens() {
        Intake closed = intake(10L, allInstitutions2026, "Annual", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 31));
        Intake next = intake(11L, universityX2027, "Annual", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        when(intakeRepository.findById(10L)).thenReturn(Optional.of(closed));
        stubYears(allInstitutions2026, universityX2027);
        stubIntakes(closed, next);

        assertThatThrownBy(() -> policy.resolveIntake(company, 10L, UNIVERSITY_X))
                .isInstanceOf(ApplicationWindowClosedException.class)
                .hasMessageEndingWith("Applications for Annual, 2027 open on 1 September 2026.");
    }

    @Test
    void resolveIntake_noneRequestedAndOneOpen_usesIt() {
        Intake open = intake(10L, universityX2027, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        stubYears(universityX2027);
        stubIntakes(open);

        assertThat(policy.resolveIntake(company, null, UNIVERSITY_X)).isEqualTo(open);
    }

    @Test
    void resolveIntake_noneRequestedAndNoneOpen_isRefusedNamingTheCompanyAndTheNextWindow() {
        Intake next = intake(10L, universityX2027, "Annual", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
        stubYears(universityX2027);
        stubIntakes(next);

        assertThatThrownBy(() -> policy.resolveIntake(company, null, UNIVERSITY_X))
                .isInstanceOf(ApplicationWindowClosedException.class)
                .hasMessage("Company B is not taking applications right now."
                        + " Applications for Annual, 2027 open on 1 September 2026.");
    }

    @Test
    void resolveIntake_noneRequestedAndNoYearAtAll_isRefused() {
        stubYears();

        assertThatThrownBy(() -> policy.resolveIntake(company, null, UNIVERSITY_X))
                .isInstanceOf(ApplicationWindowClosedException.class)
                .hasMessage("Company B is not taking applications right now.");
    }

    @Test
    void resolveIntake_noneRequestedAndSeveralOpen_asksForAChoice() {
        Intake annual = intake(10L, universityX2027, "Annual", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        Intake semester = intake(11L, universityX2027, "Semester 1", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        stubYears(universityX2027);
        stubIntakes(annual, semester);

        assertThatThrownBy(() -> policy.resolveIntake(company, null, UNIVERSITY_X))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Choose an intake for Company B: Annual, 2027 or Semester 1, 2027.");
    }

    // ── Documents ──────────────────────────────────────────────────────────────

    @Test
    void missingDocuments_requiredTypesNotUploaded_sorted() {
        when(documentTypeService.getEffectiveRequiredTypeNames(COMPANY))
                .thenReturn(Set.of("proof of address", "id document", "proof of registration"));

        assertThat(policy.missingDocuments(COMPANY, Set.of("id document")))
                .containsExactly("proof of address", "proof of registration");
    }

    // ── Fixtures ───────────────────────────────────────────────────────────────

    private void stubYears(AcademicYear... years) {
        when(academicYearRepository.findNotEndedByTenant(COMPANY, TODAY)).thenReturn(List.of(years));
    }

    private void stubIntakes(Intake... intakes) {
        when(intakeRepository.findByAcademicYear_IdInOrderByStayStartDateAscNameAsc(anyCollection()))
                .thenAnswer(inv -> {
                    Collection<Long> ids = inv.getArgument(0);
                    return List.of(intakes).stream().filter(i -> ids.contains(i.getAcademicYear().getId())).toList();
                });
    }

    private static AcademicYear year(Long id, Long institutionId, String name, LocalDate start, LocalDate end) {
        return AcademicYear.builder().id(id).tenantId(COMPANY).institutionId(institutionId).name(name)
                .startDate(start).endDate(end).build();
    }

    private static Intake intake(Long id, AcademicYear year, String name, LocalDate open, LocalDate close) {
        return Intake.builder().id(id).tenantId(year.getTenantId()).academicYear(year).intakeType(IntakeType.ANNUAL)
                .name(name).stayStartDate(year.getStartDate()).stayEndDate(year.getEndDate())
                .applicationOpenDate(open).applicationCloseDate(close).build();
    }
}
