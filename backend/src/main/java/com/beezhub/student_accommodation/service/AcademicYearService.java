package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.AcademicYearNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.InstitutionNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidAcademicCalendarException;
import com.beezhub.student_accommodation.exceptions.ResourceInUseException;
import com.beezhub.student_accommodation.mapper.AcademicYearMapper;
import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.dto.IntakeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Academic years belong to one company and, optionally, one institution (GH-494, GH-439).
 *
 * <p>Nobody marks a year as current: the current year is the one whose dates contain today, or,
 * between two years, the next one. A year for a specific institution takes priority over an
 * "All institutions" year for that institution's students. Every date is read from the calendar
 * {@link Clock}, which is pinned to {@code Africa/Johannesburg}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicYearService {

    private static final String ENTITY_LABEL = "Academic year";

    private final AcademicYearRepository academicYearRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final IntakeRepository intakeRepository;
    private final IntakeWindowPolicy intakeWindowPolicy;
    private final InstitutionRepository institutionRepository;
    private final AcademicYearMapper academicYearMapper;
    private final TenantOwnershipGuard tenantOwnershipGuard;
    private final Clock clock;

    @Transactional
    @LogActivity(action = "CREATE", entityType = "ACADEMIC_YEAR", description = "Created an academic year",
            entityIdParamIndex = -1)
    public AcademicYearResponse createAcademicYear(AcademicYearRequest request) {
        AcademicYear academicYear = academicYearMapper.toEntity(request);
        academicYear.setTenantId(callerTenantId());
        applyInstitution(academicYear, request.getInstitutionId());
        if (academicYear.getApplicationOpenDate() == null || academicYear.getApplicationCloseDate() == null) {
            IntakeWindowPolicy.Window window =
                    intakeWindowPolicy.windowFor(academicYear.getTenantId(), academicYear.getStartDate());
            academicYear.setApplicationOpenDate(window.open());
            academicYear.setApplicationCloseDate(window.close());
        }
        validate(academicYear);

        AcademicYear saved = academicYearRepository.save(academicYear);
        log.info("action=academicYearCreated academicYearId={} tenantId={} institutionId={}",
                saved.getId(), saved.getTenantId(), saved.getInstitutionId());
        return toResponse(saved);
    }

    @Transactional
    @LogActivity(action = "UPDATE", entityType = "ACADEMIC_YEAR", description = "Updated academic year {0}")
    public AcademicYearResponse updateAcademicYear(Long id, AcademicYearRequest request) {
        AcademicYear academicYear = loadOwned(id);
        academicYearMapper.updateEntity(request, academicYear);
        applyInstitution(academicYear, request.getInstitutionId());
        validate(academicYear);
        intakeRepository.findByAcademicYear_IdOrderByStayStartDateAscNameAsc(id).stream()
                .filter(intake -> intake.getStayStartDate().isBefore(academicYear.getStartDate())
                        || intake.getStayEndDate().isAfter(academicYear.getEndDate()))
                .findFirst()
                .ifPresent(intake -> {
                    throw new InvalidAcademicCalendarException(intake.getName() + " (" + intake.getStayStartDate()
                            + " to " + intake.getStayEndDate() + ") would fall outside the year. "
                            + "Move or delete it first.");
                });

        AcademicYear saved = academicYearRepository.save(academicYear);
        log.info("action=academicYearUpdated academicYearId={}", saved.getId());
        AcademicYearResponse response = toResponse(saved);
        response.setApplicationCount(applicationRepository.countByAcademicYear_Id(id));
        response.setAllocationCount(roomAllocationRepository.countByAcademicYear_Id(id));
        return response;
    }

    @Transactional(readOnly = true)
    public AcademicYearResponse getAcademicYearById(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        tenantOwnershipGuard.assertReadable(academicYear, ENTITY_LABEL, id);
        return toResponse(academicYear);
    }

    /**
     * The caller's company's years. Platform roles see every company's years, since they manage
     * them all; a company admin sees only their own (GH-439).
     */
    @Transactional(readOnly = true)
    public PageResponse<AcademicYearResponse> getAllAcademicYears(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Page<AcademicYear> years = isPlatformCaller(tenantId)
                ? academicYearRepository.findAll(pageable)
                : academicYearRepository.findByTenantId(tenantId, pageable);
        Map<Long, List<IntakeResponse>> intakesByYear = years.isEmpty() ? Map.of()
                : intakeRepository.findByAcademicYear_IdInOrderByStayStartDateAscNameAsc(
                                years.map(AcademicYear::getId).getContent()).stream()
                        .collect(Collectors.groupingBy(i -> i.getAcademicYear().getId(),
                                Collectors.mapping(this::toIntakeResponse, Collectors.toList())));
        return PageMapper.toPageResponse(years.map(year -> {
            AcademicYearResponse response = toResponseWithoutIntakes(year);
            response.setIntakes(intakesByYear.getOrDefault(year.getId(), List.of()));
            return response;
        }));
    }

    /**
     * The caller's current year by date, for {@code institutionId} or "All institutions" when it is
     * empty, falling back to the platform's years when the company has none.
     */
    @Transactional(readOnly = true)
    public AcademicYearResponse getCurrentAcademicYear(Long institutionId) {
        return findCurrentAcademicYear(callerTenantId(), institutionId)
                .map(this::toResponse)
                .orElseThrow(() -> new AcademicYearNotFoundException("No current academic year"));
    }

    /**
     * The current year of a company for an institution: the year containing today, or the next
     * one when today falls between two years. Platform (tenant 0) years are used when the company
     * has none, so a company that has not set up its own calendar keeps working as before.
     */
    @Transactional(readOnly = true)
    public Optional<AcademicYear> findCurrentAcademicYear(Long tenantId, Long institutionId) {
        LocalDate today = today();
        return pickCurrent(candidates(tenantId, institutionId, today), today);
    }

    @Transactional
    @LogActivity(action = "DELETE", entityType = "ACADEMIC_YEAR", description = "Deleted academic year {0}")
    public void deleteAcademicYear(Long id) {
        AcademicYear academicYear = loadOwned(id);
        if (applicationRepository.existsByAcademicYear_Id(id)) {
            throw new ResourceInUseException("Academic year " + academicYear.getName()
                    + " has applications and cannot be deleted. Close its applications instead.");
        }
        academicYearRepository.delete(academicYear);
        log.info("action=academicYearDeleted academicYearId={}", id);
    }

    /** Whether today, in South African time, is inside the year's application window. */
    public boolean isApplicationWindowOpen(AcademicYear academicYear) {
        return academicYear != null
                && isOpenToday(academicYear.getApplicationOpenDate(), academicYear.getApplicationCloseDate());
    }

    /** Whether today, in South African time, is inside a window. Both dates are inclusive. */
    public boolean isOpenToday(LocalDate open, LocalDate close) {
        if (open == null || close == null) {
            return false;
        }
        LocalDate today = today();
        return !today.isBefore(open) && !today.isAfter(close);
    }

    /** Today in the calendar zone. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Loads a year by id for a write, refusing another company's year as not found. */
    @Transactional(readOnly = true)
    public AcademicYear loadOwned(Long id) {
        AcademicYear academicYear = academicYearRepository.findById(id).orElseThrow(() -> notFound(id));
        tenantOwnershipGuard.assertOwned(academicYear, ENTITY_LABEL, id);
        return academicYear;
    }

    /**
     * Refuses a year whose dates are reversed, whose name is taken, or which overlaps another year
     * of the same company for the same institution, or another "All institutions" year. A year for
     * a specific institution may overlap an "All institutions" year.
     */
    public void validate(AcademicYear year) {
        if (!year.getStartDate().isBefore(year.getEndDate())) {
            throw new InvalidAcademicCalendarException("The year must start before it ends");
        }
        if (year.getApplicationOpenDate() != null && year.getApplicationCloseDate() != null
                && year.getApplicationOpenDate().isAfter(year.getApplicationCloseDate())) {
            throw new InvalidAcademicCalendarException("Applications must open before they close");
        }
        boolean nameTaken = year.getId() == null
                ? academicYearRepository.existsByTenantIdAndInstitutionIdAndName(
                        year.getTenantId(), year.getInstitutionId(), year.getName())
                : academicYearRepository.existsByTenantIdAndInstitutionIdAndNameAndIdNot(
                        year.getTenantId(), year.getInstitutionId(), year.getName(), year.getId());
        if (nameTaken) {
            throw new DuplicateResourceException("There is already a year called '" + year.getName()
                    + "' for " + institutionLabel(year));
        }
        academicYearRepository.findOverlapping(year.getTenantId(), year.getStartDate(), year.getEndDate())
                .stream()
                .filter(other -> !Objects.equals(other.getId(), year.getId()))
                .filter(other -> Objects.equals(other.getInstitutionId(), year.getInstitutionId()))
                .findFirst()
                .ifPresent(other -> {
                    throw new DuplicateResourceException("These dates overlap " + other.getName() + " ("
                            + other.getStartDate() + " to " + other.getEndDate() + ") for "
                            + institutionLabel(year) + ". Change the dates so the years follow each other.");
                });
    }

    /** Maps a year and its intakes, with window states read from the calendar clock. */
    public AcademicYearResponse toResponse(AcademicYear academicYear) {
        AcademicYearResponse response = toResponseWithoutIntakes(academicYear);
        response.setIntakes(academicYear.getId() == null ? List.of()
                : intakeRepository.findByAcademicYear_IdOrderByStayStartDateAscNameAsc(academicYear.getId())
                        .stream().map(this::toIntakeResponse).toList());
        return response;
    }

    public IntakeResponse toIntakeResponse(Intake intake) {
        return IntakeResponse.builder()
                .id(intake.getId())
                .academicYearId(intake.getAcademicYear() != null ? intake.getAcademicYear().getId() : null)
                .intakeType(intake.getIntakeType())
                .name(intake.getName())
                .stayStartDate(intake.getStayStartDate())
                .stayEndDate(intake.getStayEndDate())
                .applicationOpenDate(intake.getApplicationOpenDate())
                .applicationCloseDate(intake.getApplicationCloseDate())
                .isApplicationWindowOpen(isOpenToday(intake.getApplicationOpenDate(), intake.getApplicationCloseDate()))
                .build();
    }

    private AcademicYearResponse toResponseWithoutIntakes(AcademicYear academicYear) {
        AcademicYearResponse response = academicYearMapper.toResponse(academicYear);
        response.setIsApplicationWindowOpen(isApplicationWindowOpen(academicYear));
        return response;
    }

    private List<AcademicYear> candidates(Long tenantId, Long institutionId, LocalDate today) {
        Long tenant = tenantId == null ? TenantContext.PLATFORM_TENANT_ID : tenantId;
        List<AcademicYear> own = applicable(academicYearRepository.findNotEndedByTenant(tenant, today), institutionId);
        if (!own.isEmpty() || TenantContext.PLATFORM_TENANT_ID.equals(tenant)) {
            return own;
        }
        return applicable(academicYearRepository.findNotEndedByTenant(TenantContext.PLATFORM_TENANT_ID, today),
                institutionId);
    }

    /** Years that apply to an institution: its own, and "All institutions" years. */
    private static List<AcademicYear> applicable(List<AcademicYear> years, Long institutionId) {
        return years.stream()
                .filter(y -> y.getInstitutionId() == null || y.getInstitutionId().equals(institutionId))
                .toList();
    }

    private static Optional<AcademicYear> pickCurrent(List<AcademicYear> candidates, LocalDate today) {
        return candidates.stream()
                .filter(y -> !y.getEndDate().isBefore(today))
                .min(priority(today));
    }

    /** A year containing today first, then a specific institution's year, then the earliest. */
    private static Comparator<AcademicYear> priority(LocalDate today) {
        return Comparator.<AcademicYear, Boolean>comparing(y -> !contains(y, today))
                .thenComparing(y -> y.getInstitutionId() == null)
                .thenComparing(AcademicYear::getStartDate);
    }

    private static boolean contains(AcademicYear year, LocalDate day) {
        return !day.isBefore(year.getStartDate()) && !day.isAfter(year.getEndDate());
    }

    /**
     * Readies a year built elsewhere (a pattern or a copy) for {@link #validate}: links the
     * institution, refusing an unknown one.
     */
    public AcademicYear prepareNewYear(AcademicYear year, Long institutionId) {
        applyInstitution(year, institutionId);
        return year;
    }

    private void applyInstitution(AcademicYear year, Long institutionId) {
        Institution institution = institutionId == null ? null
                : institutionRepository.findById(institutionId).orElseThrow(() ->
                        new InstitutionNotFoundException("Institution not found with ID: " + institutionId));
        year.setInstitutionId(institutionId);
        year.setInstitution(institution);
    }

    private static String institutionLabel(AcademicYear year) {
        if (year.getInstitutionId() == null) {
            return "All institutions";
        }
        return year.getInstitution() != null ? year.getInstitution().getName() : "this institution";
    }

    private static boolean isPlatformCaller(Long tenantId) {
        return tenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(tenantId);
    }

    private static Long callerTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? TenantContext.PLATFORM_TENANT_ID : tenantId;
    }

    private static AcademicYearNotFoundException notFound(Long id) {
        return new AcademicYearNotFoundException("Academic year not found with ID: " + id);
    }
}
