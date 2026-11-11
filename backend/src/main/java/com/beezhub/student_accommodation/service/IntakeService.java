package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intakes inside academic years (GH-494): creating a year from a pattern, copying last year, and
 * editing the intakes in place. Kept apart from {@link AcademicYearService} so each stays small.
 *
 * <p>Applications do not carry an intake yet (group B of #493), so the counts and the delete
 * check below use the intake's year. Every existing year has exactly one Annual intake, so for
 * existing data the two are the same.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntakeService {

    private static final String ENTITY_LABEL = "Intake";
    private static final Pattern YEAR_NUMBER = Pattern.compile("\\b(19|20)\\d{2}\\b");

    private final IntakeRepository intakeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final AcademicYearService academicYearService;
    private final IntakeWindowPolicy intakeWindowPolicy;
    private final TenantOwnershipGuard tenantOwnershipGuard;

    /**
     * Creates a year with its intakes filled in: Annual covers the whole year, Semesters split it
     * in two and Trimesters in three. Each window comes from the intake's move-in date.
     */
    @Transactional
    @LogActivity(action = "CREATE", entityType = "ACADEMIC_YEAR", description = "Created an academic year from a pattern",
            entityIdParamIndex = -1)
    public AcademicYearResponse createFromPattern(AcademicYearPatternRequest request) {
        AcademicYear year = AcademicYear.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        year.setTenantId(callerTenantId());
        List<Intake> intakes = new ArrayList<>();
        request.getIntakeTypes().stream().sorted().forEach(type -> intakes.addAll(split(year, type)));
        AcademicYear saved = saveYearWithIntakes(year, request.getInstitutionId(), intakes);
        log.info("action=academicYearCreatedFromPattern academicYearId={} intakes={}", saved.getId(), intakes.size());
        return academicYearService.toResponse(saved);
    }

    /** Copies a year and its intakes with every date moved on by a year. */
    @Transactional
    @LogActivity(action = "COPY", entityType = "ACADEMIC_YEAR", description = "Copied academic year {0}")
    public AcademicYearResponse copyYear(Long academicYearId, AcademicYearCopyRequest request) {
        AcademicYear source = academicYearService.loadOwned(academicYearId);
        String name = request != null && request.getName() != null && !request.getName().isBlank()
                ? request.getName().trim() : nextName(source.getName());
        AcademicYear copy = AcademicYear.builder()
                .name(name)
                .startDate(source.getStartDate().plusYears(1))
                .endDate(source.getEndDate().plusYears(1))
                .build();
        copy.setTenantId(source.getTenantId());
        List<Intake> intakes = intakeRepository.findByAcademicYear_IdOrderByStayStartDateAscNameAsc(academicYearId)
                .stream()
                .map(i -> Intake.builder()
                        .academicYear(copy)
                        .tenantId(copy.getTenantId())
                        .intakeType(i.getIntakeType())
                        .name(i.getName())
                        .stayStartDate(i.getStayStartDate().plusYears(1))
                        .stayEndDate(i.getStayEndDate().plusYears(1))
                        .applicationOpenDate(i.getApplicationOpenDate().plusYears(1))
                        .applicationCloseDate(i.getApplicationCloseDate().plusYears(1))
                        .build())
                .toList();
        AcademicYear saved = saveYearWithIntakes(copy, source.getInstitutionId(), new ArrayList<>(intakes));
        log.info("action=academicYearCopied sourceId={} academicYearId={}", academicYearId, saved.getId());
        return academicYearService.toResponse(saved);
    }

    @Transactional
    @LogActivity(action = "CREATE", entityType = "INTAKE", description = "Added an intake to academic year {0}")
    public IntakeResponse createIntake(Long academicYearId, IntakeRequest request) {
        AcademicYear year = academicYearService.loadOwned(academicYearId);
        Intake intake = Intake.builder().academicYear(year).tenantId(year.getTenantId()).build();
        apply(intake, request);
        validate(intake);
        Intake saved = intakeRepository.save(intake);
        log.info("action=intakeCreated intakeId={} academicYearId={}", saved.getId(), academicYearId);
        return academicYearService.toIntakeResponse(saved);
    }

    /**
     * Edits an intake and reports how many applications and allocations it has. They keep their
     * intake and dates; the counts are there so the admin sees who the change touches.
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "INTAKE", description = "Updated intake {0}")
    public IntakeResponse updateIntake(Long intakeId, IntakeRequest request) {
        Intake intake = loadOwned(intakeId);
        apply(intake, request);
        validate(intake);
        Intake saved = intakeRepository.save(intake);
        log.info("action=intakeUpdated intakeId={}", intakeId);
        return withCounts(academicYearService.toIntakeResponse(saved), intake.getAcademicYear().getId());
    }

    @Transactional
    @LogActivity(action = "DELETE", entityType = "INTAKE", description = "Deleted intake {0}")
    public void deleteIntake(Long intakeId) {
        Intake intake = loadOwned(intakeId);
        AcademicYear year = intake.getAcademicYear();
        if (applicationRepository.existsByAcademicYear_Id(year.getId())) {
            throw new ResourceInUseException(intake.getName() + " has applications and cannot be deleted. "
                    + "Close its applications now instead.");
        }
        intakeRepository.delete(intake);
        intakeRepository.flush();
        log.info("action=intakeDeleted intakeId={}", intakeId);
    }

    /** "Close applications now": the window closes today, in South African time. */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "INTAKE", description = "Closed applications for intake {0}")
    public IntakeResponse closeApplicationsNow(Long intakeId) {
        Intake intake = loadOwned(intakeId);
        LocalDate today = academicYearService.today();
        if (intake.getApplicationOpenDate().isAfter(today)) {
            throw new InvalidAcademicCalendarException("Applications for " + intake.getName() + " open on "
                    + intake.getApplicationOpenDate() + " and are not open yet. Change the dates instead.");
        }
        if (intake.getApplicationCloseDate().isAfter(today)) {
            intake.setApplicationCloseDate(today);
        }
        Intake saved = intakeRepository.save(intake);
        log.info("action=intakeApplicationsClosed intakeId={} closeDate={}", intakeId, today);
        return withCounts(academicYearService.toIntakeResponse(saved), intake.getAcademicYear().getId());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────

    private AcademicYear saveYearWithIntakes(AcademicYear year, Long institutionId, List<Intake> intakes) {
        AcademicYear withInstitution = academicYearService.prepareNewYear(year, institutionId);
        intakes.forEach(IntakeService::validateDates);
        academicYearService.validate(withInstitution);
        AcademicYear saved = academicYearRepository.save(withInstitution);
        intakes.forEach(i -> i.setAcademicYear(saved));
        intakeRepository.saveAll(intakes);
        return saved;
    }

    /** Splits a year into {@code type}'s equal parts, each with its window filled in. */
    private List<Intake> split(AcademicYear year, IntakeType type) {
        long days = ChronoUnit.DAYS.between(year.getStartDate(), year.getEndDate()) + 1;
        int parts = type.getPartsPerYear();
        if (days < parts * 2L) {
            throw new InvalidAcademicCalendarException("The year is too short to split into " + parts + " intakes");
        }
        List<Intake> intakes = new ArrayList<>();
        for (int part = 1; part <= parts; part++) {
            LocalDate start = year.getStartDate().plusDays(days * (part - 1) / parts);
            LocalDate end = part == parts ? year.getEndDate() : year.getStartDate().plusDays(days * part / parts - 1);
            IntakeWindowPolicy.Window window = intakeWindowPolicy.windowFor(year.getTenantId(), start);
            intakes.add(Intake.builder()
                    .tenantId(year.getTenantId())
                    .intakeType(type)
                    .name(type.nameFor(part))
                    .stayStartDate(start)
                    .stayEndDate(end)
                    .applicationOpenDate(window.open())
                    .applicationCloseDate(window.close())
                    .build());
        }
        return intakes;
    }

    /** Copies the request onto the intake, filling in any empty window date from the move-in date. */
    private void apply(Intake intake, IntakeRequest request) {
        intake.setIntakeType(request.getIntakeType());
        intake.setName(request.getName().trim());
        intake.setStayStartDate(request.getStayStartDate());
        intake.setStayEndDate(request.getStayEndDate());
        LocalDate open = request.getApplicationOpenDate();
        LocalDate close = request.getApplicationCloseDate();
        if (open == null || close == null) {
            IntakeWindowPolicy.Window window = intakeWindowPolicy.windowFor(intake.getTenantId(),
                    request.getStayStartDate());
            open = open != null ? open : window.open();
            close = close != null ? close : window.close();
        }
        intake.setApplicationOpenDate(open);
        intake.setApplicationCloseDate(close);
    }

    private void validate(Intake intake) {
        validateDates(intake);
        AcademicYear year = intake.getAcademicYear();
        if (intake.getStayStartDate().isBefore(year.getStartDate()) || intake.getStayEndDate().isAfter(year.getEndDate())) {
            throw new InvalidAcademicCalendarException(intake.getName() + " must fall inside the year ("
                    + year.getStartDate() + " to " + year.getEndDate() + ")");
        }
        boolean nameTaken = intake.getId() == null
                ? intakeRepository.existsByAcademicYear_IdAndName(year.getId(), intake.getName())
                : intakeRepository.existsByAcademicYear_IdAndNameAndIdNot(year.getId(), intake.getName(), intake.getId());
        if (nameTaken) {
            throw new DuplicateResourceException("This year already has an intake called " + intake.getName());
        }
    }

    private static void validateDates(Intake intake) {
        if (!intake.getStayStartDate().isBefore(intake.getStayEndDate())) {
            throw new InvalidAcademicCalendarException(intake.getName() + " must start before it ends");
        }
        if (intake.getApplicationOpenDate().isAfter(intake.getApplicationCloseDate())) {
            throw new InvalidAcademicCalendarException("Applications for " + intake.getName()
                    + " must open before they close");
        }
    }

    private IntakeResponse withCounts(IntakeResponse response, Long academicYearId) {
        response.setApplicationCount(applicationRepository.countByAcademicYear_Id(academicYearId));
        response.setAllocationCount(roomAllocationRepository.countByAcademicYear_Id(academicYearId));
        return response;
    }

    private Intake loadOwned(Long intakeId) {
        Intake intake = intakeRepository.findById(intakeId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_LABEL + " not found with id: " + intakeId));
        return tenantOwnershipGuard.assertOwned(intake, ENTITY_LABEL, intakeId);
    }

    /** Every year in the name moves on by one: "2027" becomes "2028", "2026/2027" becomes "2027/2028". */
    static String nextName(String name) {
        Matcher matcher = YEAR_NUMBER.matcher(name);
        StringBuilder next = new StringBuilder();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            matcher.appendReplacement(next, String.valueOf(Integer.parseInt(matcher.group()) + 1));
        }
        if (!found) {
            throw new InvalidAcademicCalendarException("Give the copy a name: '" + name + "' has no year in it to move on");
        }
        matcher.appendTail(next);
        return next.toString();
    }

    private static Long callerTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? TenantContext.PLATFORM_TENANT_ID : tenantId;
    }
}
