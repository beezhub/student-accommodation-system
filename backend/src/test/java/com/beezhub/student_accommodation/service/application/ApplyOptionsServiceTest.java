package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.InstitutionNotFoundException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.CompanyApplyOptionsResponse;
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
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.BuildingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GH-495: what the application form offers a student, per building picked. */
@ExtendWith(MockitoExtension.class)
class ApplyOptionsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);

    @Mock private AuthService authService;
    @Mock private StudentRepository studentRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private CompanyIntakePolicy intakePolicy;
    @Mock private AcademicYearService academicYearService;
    @Mock private BuildingService buildingService;

    @InjectMocks private ApplyOptionsService service;

    private Student student;
    private Building residence;
    private final Tenant company = Tenant.builder().id(42L).name("Company B").build();
    private final AcademicYear year2026 = AcademicYear.builder().id(1L).tenantId(42L).name("2026").build();
    private final AcademicYear year2027 = AcademicYear.builder().id(2L).tenantId(42L).name("2027").build();

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        user.setId(5L);
        Institution institution = new Institution();
        institution.setId(7L);
        student = new Student();
        student.setId(1L);
        student.setAppUser(user);
        student.setInstitution(institution);
        residence = new Building();
        residence.setId(10L);
        residence.setTenantId(42L);
        lenient().when(authService.getCurrentUser()).thenReturn(user);
        lenient().when(studentRepository.findByAppUser_Id(5L)).thenReturn(Optional.of(student));
        lenient().when(academicYearService.today()).thenReturn(TODAY);
    }

    // ── Buildings ──────────────────────────────────────────────────────────────

    @Test
    void getBuildings_usesTheProfileInstitutionAndNamesEachCompany() {
        when(buildingRepository.findTakingInstitution(7L, TODAY)).thenReturn(List.of(residence));
        when(tenantRepository.findAllById(Set.of(42L))).thenReturn(List.of(company));
        BuildingResponse response = new BuildingResponse();
        when(buildingService.toApplicantResponses(List.of(residence), Map.of(42L, "Company B")))
                .thenReturn(List.of(response));

        assertThat(service.getBuildingsForInstitution(null)).containsExactly(response);
    }

    @Test
    void getBuildings_requestedInstitution_isChecked() {
        when(institutionRepository.existsById(8L)).thenReturn(true);
        when(buildingRepository.findTakingInstitution(8L, TODAY)).thenReturn(List.of());
        when(tenantRepository.findAllById(Set.of())).thenReturn(List.of());

        service.getBuildingsForInstitution(8L);

        verify(buildingRepository).findTakingInstitution(8L, TODAY);
    }

    @Test
    void getBuildings_unknownInstitution_isNotFound() {
        when(institutionRepository.existsById(8L)).thenReturn(false);

        assertThatThrownBy(() -> service.getBuildingsForInstitution(8L))
                .isInstanceOf(InstitutionNotFoundException.class);
    }

    /** A student without an institution matches only "All institutions" years and open buildings. */
    @Test
    void getBuildings_noInstitutionOnTheProfile_matchesNoInstitution() {
        student.setInstitution(null);
        when(buildingRepository.findTakingInstitution(-1L, TODAY)).thenReturn(List.of());
        when(tenantRepository.findAllById(Set.of())).thenReturn(List.of());

        service.getBuildingsForInstitution(null);

        verify(buildingRepository).findTakingInstitution(-1L, TODAY);
    }

    @Test
    void getBuildings_noStudentProfile_isNotFound() {
        when(studentRepository.findByAppUser_Id(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBuildingsForInstitution(null)).isInstanceOf(StudentNotFoundException.class);
    }

    // ── Options per building ───────────────────────────────────────────────────

    @Test
    void getOptions_leavesOutYearsAlreadyAppliedForAndLinksThatApplication() {
        Intake annual2026 = intake(20L, year2026, "Annual", LocalDate.of(2026, 1, 1));
        Intake annual2027 = intake(21L, year2027, "Annual", LocalDate.of(2026, 6, 1));
        Application existing = Application.builder().id(300L).applicationCode("APP-2026-001")
                .academicYear(year2026).intake(annual2026).status(ApplicationStatus.UNDER_REVIEW).build();
        stubBuildingAndCompany();
        when(applicationRepository.findByStudent_IdAndTenantIdAndStatusInOrderByIdDesc(eq(1L), eq(42L), any()))
                .thenReturn(List.of(existing));
        when(intakePolicy.openIntakes(42L, 7L)).thenReturn(List.of(annual2026, annual2027));
        when(documentRepository.findDocumentTypeNamesByStudentId(1L)).thenReturn(Set.of("ID Document"));
        when(intakePolicy.missingDocuments(42L, Set.of("id document"))).thenReturn(List.of("proof of address"));

        CompanyApplyOptionsResponse options = service.getOptionsForBuilding(10L, null);

        assertThat(options.getCompanyName()).isEqualTo("Company B");
        assertThat(options.getOpenIntakes()).extracting("intakeId").containsExactly(21L);
        assertThat(options.getOpenIntakes().get(0).getAcademicYearName()).isEqualTo("2027");
        assertThat(options.getExistingApplications()).singleElement().satisfies(link -> {
            assertThat(link.getApplicationId()).isEqualTo(300L);
            assertThat(link.getAcademicYearName()).isEqualTo("2026");
            assertThat(link.getIntakeName()).isEqualTo("Annual");
            assertThat(link.getStatus()).isEqualTo("UNDER_REVIEW");
        });
        assertThat(options.getNextIntake()).isNull();
        assertThat(options.getMissingDocuments()).containsExactly("proof of address");
    }

    @Test
    void getOptions_nothingOpen_givesTheNextWindow() {
        Intake semester2 = intake(22L, year2027, "Semester 2", LocalDate.of(2027, 5, 1));
        stubBuildingAndCompany();
        when(applicationRepository.findByStudent_IdAndTenantIdAndStatusInOrderByIdDesc(anyLong(), anyLong(), any()))
                .thenReturn(List.of());
        when(intakePolicy.openIntakes(42L, 7L)).thenReturn(List.of());
        when(intakePolicy.nextToOpen(42L, 7L, Set.of())).thenReturn(Optional.of(semester2));
        when(documentRepository.findDocumentTypeNamesByStudentId(1L)).thenReturn(Set.of());
        when(intakePolicy.missingDocuments(42L, Set.of())).thenReturn(List.of());

        CompanyApplyOptionsResponse options = service.getOptionsForBuilding(10L, null);

        assertThat(options.getOpenIntakes()).isEmpty();
        assertThat(options.getNextIntake().getIntakeName()).isEqualTo("Semester 2");
        assertThat(options.getNextIntake().getApplicationOpenDate()).isEqualTo(LocalDate.of(2027, 5, 1));
    }

    @Test
    void getOptions_unknownBuilding_isNotFound() {
        when(buildingRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOptionsForBuilding(10L, null)).isInstanceOf(BuildingNotFoundException.class);
    }

    // ── Open windows for the dashboard ─────────────────────────────────────────

    @Test
    void getOpenWindows_onlyCompaniesWithAnIntakeTheStudentCanStillApplyFor() {
        Tenant other = Tenant.builder().id(43L).name("Company A").build();
        Building otherResidence = new Building();
        otherResidence.setId(11L);
        otherResidence.setTenantId(43L);
        Intake annual2027 = intake(21L, year2027, "Annual", LocalDate.of(2026, 6, 1));
        when(buildingRepository.findTakingInstitution(7L, TODAY)).thenReturn(List.of(residence, otherResidence));
        when(tenantRepository.findAllById(Set.of(42L, 43L))).thenReturn(List.of(company, other));
        when(applicationRepository.findByStudent_IdAndTenantIdAndStatusInOrderByIdDesc(anyLong(), anyLong(), any()))
                .thenReturn(List.of());
        when(intakePolicy.openIntakes(42L, 7L)).thenReturn(List.of(annual2027));
        when(intakePolicy.openIntakes(43L, 7L)).thenReturn(List.of());
        when(intakePolicy.nextToOpen(43L, 7L, Set.of())).thenReturn(Optional.empty());

        List<CompanyApplyOptionsResponse> windows = service.getOpenWindows();

        assertThat(windows).singleElement().satisfies(w -> {
            assertThat(w.getCompanyName()).isEqualTo("Company B");
            assertThat(w.getOpenIntakes()).extracting("intakeId").containsExactly(21L);
        });
    }

    private void stubBuildingAndCompany() {
        when(buildingRepository.findById(10L)).thenReturn(Optional.of(residence));
        when(tenantRepository.findById(42L)).thenReturn(Optional.of(company));
    }

    private static Intake intake(Long id, AcademicYear year, String name, LocalDate open) {
        return Intake.builder().id(id).tenantId(42L).academicYear(year).intakeType(IntakeType.ANNUAL).name(name)
                .applicationOpenDate(open).applicationCloseDate(open.plusMonths(3))
                .stayStartDate(open.plusMonths(4)).stayEndDate(open.plusMonths(10)).build();
    }
}
