package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateApplicationException;
import com.beezhub.student_accommodation.exceptions.EmailNotVerifiedException;
import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.beezhub.student_accommodation.model.enums.ApplicationStatus.*;

/**
 * Submitting an application (GH-495).
 *
 * <p>One form takes up to three buildings from any companies, in the student's order of
 * preference. They are grouped by company, and one application is created per company with only
 * that company's buildings, renumbered from 1, so each company reviews only its own application.
 *
 * <p>All or nothing: every company is checked before anything is written, and every application
 * is written in the one transaction, so a refusal for any company, including a unique-index
 * violation on the last insert, saves nothing. The refusal names the company and the reason.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationSubmissionService {

    static final int MAX_BUILDINGS = 3;

    /**
     * Statuses that count as an active application. Keep this list and the predicate of
     * {@link #ACTIVE_APPLICATION_INDEX} identical; CHECKED_IN is deliberately absent from both.
     */
    static final List<ApplicationStatus> ACTIVE_STATUSES = List.of(
            PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED, AWAITING_ALLOCATION, ALLOCATED);

    /** Defined in V2026_11_10_1400__one_active_application_per_company_year.sql. */
    static final String ACTIVE_APPLICATION_INDEX = "ux_application_student_tenant_year_active";

    private final ApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final BuildingRepository buildingRepository;
    private final TenantRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationMapper applicationMapper;
    private final AuthService authService;
    private final CompanyIntakePolicy intakePolicy;
    private final ApplicationPreferenceWriter preferenceWriter;
    private final ReturningResidentPolicy returningResidentPolicy;
    private final Clock clock;

    /** One company's part of a submission, checked and ready to write. */
    private record CompanyPlan(Tenant company, Intake intake, List<Building> buildings) {
    }

    /** Submits the form, returning the applications created, one per company, in the student's order. */
    @Transactional
    public List<ApplicationResponse> submitApplication(@Valid ApplicationRequest request) {
        Student student = loadApplicant(request.getStudentId());
        List<Building> buildings = resolveBuildings(request);
        Map<Long, List<Building>> byCompany = buildings.stream().collect(Collectors.groupingBy(
                Building::getTenantId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Tenant> companies = tenantRepository.findAllById(byCompany.keySet()).stream()
                .collect(Collectors.toMap(Tenant::getId, Function.identity()));

        Long institutionId = student.getInstitution() != null ? student.getInstitution().getId() : null;
        Set<String> uploaded = documentRepository.findDocumentTypeNamesByStudentId(student.getId()).stream()
                .map(String::toLowerCase).collect(Collectors.toSet());
        Map<Long, Long> requestedIntakes = request.getIntakeIds() != null ? request.getIntakeIds() : Map.of();

        List<CompanyPlan> plans = new ArrayList<>();
        byCompany.forEach((companyId, companyBuildings) -> {
            Tenant company = companies.get(companyId);
            if (company == null) {
                throw new BuildingNotFoundException("Building not found with ID: " + companyBuildings.get(0).getId());
            }
            plans.add(check(student, company, companyBuildings, requestedIntakes.get(companyId),
                    institutionId, uploaded));
        });

        List<ApplicationResponse> created = new ArrayList<>();
        for (CompanyPlan plan : plans) {
            created.add(create(student, plan, request));
        }
        log.info("action=applicationsSubmitted studentId={} count={}", student.getId(), created.size());
        return created;
    }

    /** Every check for one company, before anything is written. */
    private CompanyPlan check(Student student, Tenant company, List<Building> buildings, Long requestedIntakeId,
                              Long institutionId, Set<String> uploaded) {
        Intake intake = intakePolicy.resolveIntake(company, requestedIntakeId, institutionId);
        List<String> missing = intakePolicy.missingDocuments(company.getId(), uploaded);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(company.getName() + " needs these documents before you apply. "
                    + "Missing required documents: " + String.join(", ", missing));
        }
        // One active application per company per year. This pre-check gives the message; the
        // unique index is what holds the rule when two submits race (GH-201).
        if (applicationRepository.existsByStudent_IdAndTenantIdAndAcademicYear_IdAndStatusIn(
                student.getId(), company.getId(), intake.getAcademicYear().getId(), ACTIVE_STATUSES)) {
            throw new DuplicateApplicationException(duplicateMessage(company, intake));
        }
        return new CompanyPlan(company, intake, buildings);
    }

    private ApplicationResponse create(Student student, CompanyPlan plan, ApplicationRequest request) {
        Tenant company = plan.company();
        Intake intake = plan.intake();
        boolean multiPreference = request.getPreferredBuildingIds() != null
                && !request.getPreferredBuildingIds().isEmpty();
        Application application = Application.builder()
                .applicationCode(generateApplicationCode())
                .applicationDate(LocalDate.now(clock))
                .status(PENDING)
                .student(student)
                .tenantId(company.getId())
                .tenant(company)
                .requiresAccessibility(Boolean.TRUE.equals(request.getRequiresAccessibility()))
                .specialRequirements(request.getSpecialRequirements())
                .institutionName(institutionName(request, student))
                .courseName(request.getCourseName())
                .preferredBuilding(plan.buildings().get(0))
                .academicYear(intake.getAcademicYear())
                .intake(intake)
                .institution(student.getInstitution())
                .isReturning(returningResidentPolicy.isReturning(student.getId(), company.getId()))
                .build();

        // saveAndFlush, so a unique-index violation surfaces here, inside the transaction, where it
        // is reported as a duplicate and rolls back every application of this submission.
        Application saved;
        try {
            saved = applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateActiveApplication(e)) {
                throw e;
            }
            log.info("action=duplicateApplicationRejected reason=uniqueIndex student={} tenant={}",
                    student.getId(), company.getId());
            throw new DuplicateApplicationException(duplicateMessage(company, intake));
        }
        preferenceWriter.recordSubmission(saved.getId(), saved, multiPreference ? plan.buildings() : List.of());
        log.info("action=applicationSubmitted applicationId={} tenantId={} intakeId={}",
                saved.getId(), company.getId(), intake.getId());
        return applicationMapper.toResponse(saved);
    }

    private Student loadApplicant(Long studentId) {
        Student student = studentRepository.findStudentWithDetailsById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with ID: " + studentId));
        AppUser currentUser = authService.getCurrentUser();
        if (!student.getAppUser().getId().equals(currentUser.getId()) && !currentUser.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("You can only submit applications for yourself");
        }
        // GH-153: checked against the applicant's own account, not the caller's, so an admin
        // submitting on a student's behalf cannot launder an unverified address.
        if (!Boolean.TRUE.equals(student.getAppUser().getEmailVerified())) {
            throw new EmailNotVerifiedException("Confirm your email address before submitting an application. "
                    + "Check your inbox for the verification link, or request a new one.");
        }
        List<String> missingFields = ApplicantProfileCheck.missingFields(student);
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Student profile is incomplete. Missing: "
                    + String.join(", ", missingFields)
                    + ". Please update your profile before submitting an application.");
        }
        return student;
    }

    /**
     * The buildings picked, in order, without repeats. A client that sends only the single legacy
     * preferred building still works: it is the one building of the submission.
     */
    private List<Building> resolveBuildings(ApplicationRequest request) {
        Set<Long> ids = new LinkedHashSet<>();
        if (request.getPreferredBuildingIds() != null) {
            ids.addAll(request.getPreferredBuildingIds());
        }
        if (ids.isEmpty() && request.getPreferredBuildingId() != null) {
            ids.add(request.getPreferredBuildingId());
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one building preference is required to identify the property company");
        }
        if (ids.size() > MAX_BUILDINGS) {
            throw new IllegalArgumentException("You can select at most 3 preferred buildings");
        }
        return ids.stream()
                .map(id -> buildingRepository.findById(id)
                        .orElseThrow(() -> new BuildingNotFoundException("Building not found with ID: " + id)))
                .toList();
    }

    private static String institutionName(ApplicationRequest request, Student student) {
        if (request.getInstitutionName() != null && !request.getInstitutionName().isBlank()) {
            return request.getInstitutionName();
        }
        return student.getInstitution() != null ? student.getInstitution().getName() : null;
    }

    private static String duplicateMessage(Tenant company, Intake intake) {
        return "You already applied to " + company.getName() + " for " + intake.getAcademicYear().getName()
                + ". You can apply there again for another year.";
    }

    /**
     * Whether this violation is the one-active-application index rather than any other constraint.
     * Matched on the index name in the PostgreSQL message, so renaming the index without renaming
     * {@link #ACTIVE_APPLICATION_INDEX} turns a 409 back into a 500.
     */
    private static boolean isDuplicateActiveApplication(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(ACTIVE_APPLICATION_INDEX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uniqueness comes from the database sequence alone (GH-143). The year in the code is
     * decorative; the intake and year are shown separately.
     */
    private String generateApplicationCode() {
        long sequence = applicationRepository.nextApplicationCodeSequence();
        return String.format("APP-%d-%03d", LocalDate.now(clock).getYear(), sequence);
    }
}
