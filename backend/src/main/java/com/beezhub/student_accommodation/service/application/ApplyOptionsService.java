package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.InstitutionNotFoundException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.CompanyApplyOptionsResponse;
import com.beezhub.student_accommodation.model.dto.ExistingApplicationLink;
import com.beezhub.student_accommodation.model.dto.IntakeOptionResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.BuildingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What the application form offers a student (GH-495): the buildings that can take their
 * institution, and, per building picked, its company's open intakes, the student's own active
 * applications there, and the documents that company still needs.
 *
 * <p>Students are platform tenant 0, so these reads cross companies. Each one names the company or
 * the student explicitly and returns only what the form needs: building listings that are already
 * public, intake dates, document type names, and the calling student's own applications. Nothing
 * here reads another student's data or a company's applications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyOptionsService {

    /** Matches no institution, for a student whose profile has none. */
    private static final long NO_INSTITUTION = -1L;

    private final AuthService authService;
    private final StudentRepository studentRepository;
    private final InstitutionRepository institutionRepository;
    private final BuildingRepository buildingRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final CompanyIntakePolicy intakePolicy;
    private final AcademicYearService academicYearService;
    private final BuildingService buildingService;

    /**
     * Buildings of any company that can take a student of the institution: the company has a year
     * for it, or an "All institutions" year, and the building lists it or lists none.
     *
     * @param institutionId the institution to check, or empty for the one on the caller's profile
     */
    @Transactional(readOnly = true)
    public List<BuildingResponse> getBuildingsForInstitution(Long institutionId) {
        Long institution = resolveInstitution(institutionId, currentStudent());
        List<Building> buildings = buildingRepository.findTakingInstitution(
                institution != null ? institution : NO_INSTITUTION, academicYearService.today());
        Map<Long, String> companyNames = tenantRepository
                .findAllById(buildings.stream().map(Building::getTenantId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
        log.info("action=applyBuildingsListed institutionId={} count={}", institution, buildings.size());
        return buildingService.toApplicantResponses(buildings, companyNames);
    }

    /**
     * The company owning {@code buildingId}, as the form checks it when the building is picked:
     * open intakes for the institution, leaving out years the student already has an active
     * application for there (linked instead), the next window when none is open, and the
     * company's required documents the student has not uploaded.
     */
    @Transactional(readOnly = true)
    public CompanyApplyOptionsResponse getOptionsForBuilding(Long buildingId, Long institutionId) {
        Student student = currentStudent();
        Long institution = resolveInstitution(institutionId, student);
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with ID: " + buildingId));
        Long companyId = building.getTenantId();
        Tenant company = tenantRepository.findById(companyId)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with ID: " + buildingId));

        List<Application> active = applicationRepository.findByStudent_IdAndTenantIdAndStatusInOrderByIdDesc(
                student.getId(), companyId, ApplicationSubmissionService.ACTIVE_STATUSES);
        Set<Long> appliedYearIds = active.stream()
                .map(a -> a.getAcademicYear() != null ? a.getAcademicYear().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<IntakeOptionResponse> open = intakePolicy.openIntakes(companyId, institution).stream()
                .filter(i -> !appliedYearIds.contains(i.getAcademicYear().getId()))
                .map(ApplyOptionsService::toOption)
                .toList();
        IntakeOptionResponse next = open.isEmpty()
                ? intakePolicy.nextToOpen(companyId, institution, appliedYearIds).map(ApplyOptionsService::toOption)
                        .orElse(null)
                : null;
        Set<String> uploaded = documentRepository.findDocumentTypeNamesByStudentId(student.getId()).stream()
                .map(String::toLowerCase).collect(Collectors.toSet());

        return CompanyApplyOptionsResponse.builder()
                .buildingId(buildingId)
                .companyId(companyId)
                .companyName(company.getName())
                .openIntakes(open)
                .existingApplications(active.stream().map(ApplyOptionsService::toLink).toList())
                .nextIntake(next)
                .missingDocuments(intakePolicy.missingDocuments(companyId, uploaded))
                .build();
    }

    private Student currentStudent() {
        AppUser user = authService.getCurrentUser();
        return studentRepository.findByAppUser_Id(user.getId())
                .orElseThrow(() -> new StudentNotFoundException("Student profile not found for user"));
    }

    private Long resolveInstitution(Long requested, Student student) {
        if (requested == null) {
            return student.getInstitution() != null ? student.getInstitution().getId() : null;
        }
        if (!institutionRepository.existsById(requested)) {
            throw new InstitutionNotFoundException("Institution not found with ID: " + requested);
        }
        return requested;
    }

    static IntakeOptionResponse toOption(Intake intake) {
        return IntakeOptionResponse.builder()
                .intakeId(intake.getId())
                .intakeName(intake.getName())
                .intakeType(intake.getIntakeType() != null ? intake.getIntakeType().name() : null)
                .academicYearId(intake.getAcademicYear().getId())
                .academicYearName(intake.getAcademicYear().getName())
                .stayStartDate(intake.getStayStartDate())
                .stayEndDate(intake.getStayEndDate())
                .applicationOpenDate(intake.getApplicationOpenDate())
                .applicationCloseDate(intake.getApplicationCloseDate())
                .build();
    }

    private static ExistingApplicationLink toLink(Application application) {
        return ExistingApplicationLink.builder()
                .applicationId(application.getId())
                .applicationCode(application.getApplicationCode())
                .academicYearId(application.getAcademicYear() != null ? application.getAcademicYear().getId() : null)
                .academicYearName(application.getAcademicYear() != null ? application.getAcademicYear().getName() : null)
                .intakeName(application.getIntake() != null ? application.getIntake().getName() : null)
                .status(application.getStatus() != null ? application.getStatus().name() : null)
                .build();
    }
}
