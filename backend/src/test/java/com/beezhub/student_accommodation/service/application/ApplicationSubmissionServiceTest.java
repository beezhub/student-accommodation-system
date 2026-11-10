package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.config.ClockConfig;
import com.beezhub.student_accommodation.exceptions.ApplicationWindowClosedException;
import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.exceptions.EmailNotVerifiedException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** GH-495: one form, buildings from any companies, one application per company, all or nothing. */
@ExtendWith(MockitoExtension.class)
class ApplicationSubmissionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    @Mock private ApplicationRepository applicationRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private AuthService authService;
    @Mock private CompanyIntakePolicy intakePolicy;
    @Mock private ApplicationPreferenceWriter preferenceWriter;

    private ApplicationSubmissionService service;

    private AppUser studentUser;
    private Student student;
    private Tenant companyA;
    private Tenant companyB;
    private Intake intakeA;
    private Intake intakeB;
    private Building residence1;
    private Building residence2;
    private Building residence3;
    private ApplicationRequest request;
    private long nextId = 500L;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(TODAY.atTime(9, 0), ClockConfig.CALENDAR_ZONE).toInstant(),
                ClockConfig.CALENDAR_ZONE);
        service = new ApplicationSubmissionService(applicationRepository, studentRepository, buildingRepository,
                tenantRepository, documentRepository, applicationMapper, authService, intakePolicy,
                preferenceWriter, clock);

        studentUser = new AppUser();
        studentUser.setId(1L);
        studentUser.setUserRole(UserRole.STUDENT);
        studentUser.setEmailVerified(true);

        Institution institution = new Institution();
        institution.setId(7L);
        institution.setName("University X");

        student = new Student();
        student.setId(1L);
        student.setStudentNumber("STU001");
        student.setPhoneNumber("0123456789");
        student.setDateOfBirth(LocalDate.of(2000, 5, 14));
        student.setGender("Female");
        student.setEmergencyContactName("Thandi Mokoena");
        student.setEmergencyContactPhone("+27 82 123 4567");
        student.setAppUser(studentUser);
        student.setInstitution(institution);

        companyA = Tenant.builder().id(42L).name("Company A").build();
        companyB = Tenant.builder().id(43L).name("Company B").build();
        intakeA = intake(100L, companyA, "2027");
        intakeB = intake(200L, companyB, "2027");
        residence1 = building(10L, "Residence 1", companyA);
        residence2 = building(11L, "Residence 2", companyB);
        residence3 = building(12L, "Residence 3", companyA);

        request = new ApplicationRequest();
        request.setStudentId(1L);
        request.setCourseName("Computer Science");
        request.setPreferredBuildingIds(List.of(10L, 11L, 12L));
    }

    // ── One application per company ───────────────────────────────────────────

    @Test
    void submit_buildingsAtTwoCompanies_createsOneApplicationEachWithOnlyItsOwnBuildingsInOrder() {
        stubHappyPath();

        List<ApplicationResponse> created = service.submitApplication(request);

        assertThat(created).hasSize(2);
        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository, times(2)).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues()).extracting(Application::getTenantId).containsExactly(42L, 43L);

        ArgumentCaptor<Application> forApp = ArgumentCaptor.forClass(Application.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Building>> buildings = ArgumentCaptor.forClass(List.class);
        verify(preferenceWriter, times(2)).recordSubmission(anyLong(), forApp.capture(), buildings.capture());
        assertThat(buildings.getAllValues().get(0)).containsExactly(residence1, residence3);
        assertThat(buildings.getAllValues().get(1)).containsExactly(residence2);
        assertThat(forApp.getAllValues().get(0).getPreferredBuilding()).isEqualTo(residence1);
        assertThat(forApp.getAllValues().get(1).getPreferredBuilding()).isEqualTo(residence2);
    }

    @Test
    void submit_recordsTheIntakeItsYearAndTheProfileInstitution() {
        request.setPreferredBuildingIds(List.of(10L));
        stubHappyPath();

        service.submitApplication(request);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).saveAndFlush(saved.capture());
        Application application = saved.getValue();
        assertThat(application.getIntake()).isEqualTo(intakeA);
        assertThat(application.getAcademicYear()).isEqualTo(intakeA.getAcademicYear());
        assertThat(application.getInstitution()).isEqualTo(student.getInstitution());
        assertThat(application.getInstitutionName()).isEqualTo("University X");
        assertThat(application.getApplicationDate()).isEqualTo(TODAY);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(application.getTenant()).isEqualTo(companyA);
    }

    @Test
    void submit_passesTheIntakeChosenForEachCompany() {
        request.setIntakeIds(Map.of(42L, 100L));
        stubHappyPath();

        service.submitApplication(request);

        verify(intakePolicy).resolveIntake(companyA, 100L, 7L);
        verify(intakePolicy).resolveIntake(eq(companyB), isNull(), eq(7L));
    }

    /** An older client sends no intakes; the company's one open intake is used. */
    @Test
    void submit_missingIntakeIds_letsThePolicyPickTheOneOpenIntake() {
        request.setPreferredBuildingIds(List.of(10L));
        request.setIntakeIds(null);
        stubHappyPath();

        service.submitApplication(request);

        verify(intakePolicy).resolveIntake(eq(companyA), isNull(), eq(7L));
    }

    @Test
    void submit_studentWithoutInstitution_checksIntakesWithNoInstitution() {
        student.setInstitution(null);
        request.setPreferredBuildingIds(List.of(10L));
        request.setInstitutionName("Typed in");
        stubHappyPath();

        service.submitApplication(request);

        verify(intakePolicy).resolveIntake(eq(companyA), isNull(), isNull());
        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getInstitution()).isNull();
        assertThat(saved.getValue().getInstitutionName()).isEqualTo("Typed in");
    }

    // ── All or nothing ─────────────────────────────────────────────────────────

    @Test
    void submit_closedWindowAtOneCompany_savesNothingAndNamesIt() {
        stubLookups();
        when(intakePolicy.resolveIntake(eq(companyA), any(), any())).thenReturn(intakeA);
        when(intakePolicy.resolveIntake(eq(companyB), any(), any()))
                .thenThrow(new ApplicationWindowClosedException("Company B opens applications for Annual, 2027 on 1 May 2027."));
        when(intakePolicy.missingDocuments(anyLong(), anySet())).thenReturn(List.of());

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(ApplicationWindowClosedException.class)
                .hasMessageContaining("Company B");
        verify(applicationRepository, never()).saveAndFlush(any());
        verifyNoInteractions(preferenceWriter);
    }

    @Test
    void submit_missingDocumentAtOneCompany_savesNothingAndNamesIt() {
        stubLookups();
        when(intakePolicy.resolveIntake(any(), any(), any())).thenReturn(intakeA, intakeB);
        when(intakePolicy.missingDocuments(42L, Set.of())).thenReturn(List.of());
        when(intakePolicy.missingDocuments(43L, Set.of())).thenReturn(List.of("proof of address"));

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company B needs")
                .hasMessageContaining("proof of address");
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void submit_activeApplicationAtOneCompany_savesNothingAndNamesIt() {
        stubLookups();
        when(intakePolicy.resolveIntake(any(), any(), any())).thenReturn(intakeA, intakeB);
        when(intakePolicy.missingDocuments(anyLong(), anySet())).thenReturn(List.of());
        when(applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(eq(1L), eq(42L), any())).thenReturn(false);
        when(applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(eq(1L), eq(43L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("Company B");
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    /**
     * Two concurrent submits both pass the pre-check, so the unique index refuses the second. The
     * exception leaves the method, which rolls back every application of the submission.
     */
    @Test
    void submit_uniqueIndexViolationOnTheSecondCompany_isADuplicateNamingIt() {
        stubHappyPath();
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(inv -> withId(inv.getArgument(0)))
                .thenThrow(new DataIntegrityViolationException("could not execute statement",
                        new RuntimeException("duplicate key value violates unique constraint \""
                                + ApplicationSubmissionService.ACTIVE_APPLICATION_INDEX + "\"")));

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("Company B");
    }

    @Test
    void submit_unrelatedConstraintViolation_isNotDisguisedAsADuplicate() {
        stubHappyPath();
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenThrow(new DataIntegrityViolationException("null value in column \"student_id\""));

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Buildings ──────────────────────────────────────────────────────────────

    @Test
    void submit_moreThanThreeBuildings_isRefused() {
        stubApplicant();
        request.setPreferredBuildingIds(List.of(10L, 11L, 12L, 13L));

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 3");
    }

    @Test
    void submit_noBuildings_isRefused() {
        stubApplicant();
        request.setPreferredBuildingIds(null);

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("building preference is required");
    }

    @Test
    void submit_unknownBuilding_isNotFound() {
        stubApplicant();
        when(buildingRepository.findById(10L)).thenReturn(Optional.empty());
        request.setPreferredBuildingIds(List.of(10L));

        assertThatThrownBy(() -> service.submitApplication(request)).isInstanceOf(BuildingNotFoundException.class);
    }

    @Test
    void submit_sameBuildingTwice_countsOnce() {
        request.setPreferredBuildingIds(List.of(10L, 10L, 12L));
        stubHappyPath();

        service.submitApplication(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Building>> buildings = ArgumentCaptor.forClass(List.class);
        verify(preferenceWriter).recordSubmission(anyLong(), any(), buildings.capture());
        assertThat(buildings.getValue()).containsExactly(residence1, residence3);
    }

    /** An older client sends only the single preferred building, and no preference rows. */
    @Test
    void submit_legacySingleBuilding_setsItAsPreferredAndWritesNoPreferenceRows() {
        request.setPreferredBuildingIds(null);
        request.setPreferredBuildingId(10L);
        stubHappyPath();

        service.submitApplication(request);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPreferredBuilding()).isEqualTo(residence1);
        verify(preferenceWriter).recordSubmission(anyLong(), any(), eq(List.of()));
    }

    @Test
    void submit_codeComesFromTheSequence() {
        request.setPreferredBuildingIds(List.of(10L));
        stubHappyPath();
        when(applicationRepository.nextApplicationCodeSequence()).thenReturn(12345L);

        service.submitApplication(request);

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getApplicationCode()).isEqualTo("APP-2026-12345");
    }

    // ── The applicant ──────────────────────────────────────────────────────────

    @Test
    void submit_unknownStudent_isNotFound() {
        when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitApplication(request)).isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void submit_forAnotherStudent_isRefused() {
        AppUser other = new AppUser();
        other.setId(2L);
        other.setUserRole(UserRole.STUDENT);
        when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
        when(authService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> service.submitApplication(request)).isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void submit_unverifiedEmail_isRefusedEvenForAnAdminCaller() {
        studentUser.setEmailVerified(false);
        AppUser admin = new AppUser();
        admin.setId(99L);
        admin.setUserRole(UserRole.PLATFORM_ADMIN);
        when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
        when(authService.getCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("Confirm your email address");
    }

    @Test
    void submit_incompleteProfile_listsEveryMissingField() {
        student.setStudentNumber(" ");
        student.setPhoneNumber(null);
        student.setDateOfBirth(null);
        student.setGender(null);
        student.setEmergencyContactName(null);
        student.setEmergencyContactPhone("");
        stubApplicant();

        assertThatThrownBy(() -> service.submitApplication(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("profile is incomplete")
                .hasMessageContaining("student number")
                .hasMessageContaining("phone number")
                .hasMessageContaining("date of birth")
                .hasMessageContaining("gender")
                .hasMessageContaining("emergency contact name")
                .hasMessageContaining("emergency contact phone");
    }

    // ── Fixtures ───────────────────────────────────────────────────────────────

    private void stubApplicant() {
        when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
        when(authService.getCurrentUser()).thenReturn(studentUser);
    }

    private void stubLookups() {
        stubApplicant();
        lenient().when(buildingRepository.findById(10L)).thenReturn(Optional.of(residence1));
        lenient().when(buildingRepository.findById(11L)).thenReturn(Optional.of(residence2));
        lenient().when(buildingRepository.findById(12L)).thenReturn(Optional.of(residence3));
        when(tenantRepository.findAllById(any())).thenReturn(new ArrayList<>(List.of(companyA, companyB)));
        when(documentRepository.findDocumentTypeNamesByStudentId(1L)).thenReturn(Set.of());
    }

    private void stubHappyPath() {
        stubLookups();
        lenient().when(intakePolicy.resolveIntake(eq(companyA), any(), any())).thenReturn(intakeA);
        lenient().when(intakePolicy.resolveIntake(eq(companyB), any(), any())).thenReturn(intakeB);
        lenient().when(intakePolicy.missingDocuments(anyLong(), anySet())).thenReturn(List.of());
        lenient().when(applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(anyLong(), anyLong(), any()))
                .thenReturn(false);
        lenient().when(applicationRepository.nextApplicationCodeSequence()).thenReturn(1L);
        lenient().when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(applicationMapper.toResponse(any(Application.class))).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            return ApplicationResponse.builder().id(a.getId()).tenantId(a.getTenantId()).build();
        });
    }

    private Application withId(Application application) {
        application.setId(nextId++);
        return application;
    }

    private static Intake intake(Long id, Tenant company, String yearName) {
        AcademicYear year = AcademicYear.builder().id(id + 1).tenantId(company.getId()).name(yearName)
                .startDate(LocalDate.of(2027, 2, 1)).endDate(LocalDate.of(2027, 11, 30)).build();
        return Intake.builder().id(id).tenantId(company.getId()).academicYear(year).intakeType(IntakeType.ANNUAL)
                .name("Annual").stayStartDate(year.getStartDate()).stayEndDate(year.getEndDate())
                .applicationOpenDate(LocalDate.of(2026, 10, 1)).applicationCloseDate(LocalDate.of(2027, 1, 15))
                .build();
    }

    private static Building building(Long id, String name, Tenant company) {
        Building building = new Building();
        building.setId(id);
        building.setName(name);
        building.setTenantId(company.getId());
        return building;
    }
}
