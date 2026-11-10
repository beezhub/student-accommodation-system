package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.ApplicationWindowClosedException;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which intakes of one company a student of one institution can apply for, and the checks a
 * submission makes per company (GH-495). The application form and the submit endpoint both use
 * it, so a problem the form shows as a building is picked is the same one submit would refuse.
 *
 * <p>Only the company's own years count: a company that has not set up a year for the student's
 * institution, or an "All institutions" year, takes no applications from them. Every query names
 * the company explicitly, so the result does not depend on the caller's tenant filter; students are
 * platform tenant 0 and see every company's intakes this way, but only these window and stay
 * dates, never another student's data.
 */
@Component
@RequiredArgsConstructor
public class CompanyIntakePolicy {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final AcademicYearRepository academicYearRepository;
    private final IntakeRepository intakeRepository;
    private final AcademicYearService academicYearService;
    private final DocumentTypeService documentTypeService;

    /**
     * The company's years that have not ended and apply to the institution: its own and "All
     * institutions" years. A year for the institution takes priority over an "All institutions"
     * year whose dates overlap it, as it does for the current year (GH-494).
     */
    public List<AcademicYear> applicableYears(Long companyId, Long institutionId) {
        List<AcademicYear> years = academicYearRepository
                .findNotEndedByTenant(companyId, academicYearService.today()).stream()
                .filter(y -> y.getInstitutionId() == null || y.getInstitutionId().equals(institutionId))
                .toList();
        List<AcademicYear> specific = years.stream().filter(y -> y.getInstitutionId() != null).toList();
        return years.stream()
                .filter(y -> y.getInstitutionId() != null || specific.stream().noneMatch(s -> overlaps(s, y)))
                .toList();
    }

    /** Intakes of the applicable years whose window is open today, earliest move-in first. */
    public List<Intake> openIntakes(Long companyId, Long institutionId) {
        return intakesOf(applicableYears(companyId, institutionId)).stream()
                .filter(this::isOpen)
                .toList();
    }

    /** The intake of the applicable years whose window opens next, after today. */
    public Optional<Intake> nextToOpen(Long companyId, Long institutionId, Set<Long> excludedYearIds) {
        LocalDate today = academicYearService.today();
        return intakesOf(applicableYears(companyId, institutionId)).stream()
                .filter(i -> !excludedYearIds.contains(i.getAcademicYear().getId()))
                .filter(i -> i.getApplicationOpenDate().isAfter(today))
                .min(Comparator.comparing(Intake::getApplicationOpenDate).thenComparing(Intake::getStayStartDate));
    }

    /**
     * The intake a submission to {@code company} is for: the one requested, or the company's one
     * open intake when none was requested. Refuses an intake of another company, of another
     * institution, or whose window is not open, naming the company each time.
     */
    public Intake resolveIntake(Tenant company, Long requestedIntakeId, Long institutionId) {
        if (requestedIntakeId == null) {
            return onlyOpenIntake(company, institutionId);
        }
        Intake intake = intakeRepository.findById(requestedIntakeId)
                .filter(i -> company.getId().equals(i.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("The intake chosen for " + company.getName()
                        + " is not one of theirs. Choose one of " + company.getName() + "'s intakes."));
        Long yearInstitution = intake.getAcademicYear().getInstitutionId();
        if (yearInstitution != null && !yearInstitution.equals(institutionId)) {
            throw new IllegalArgumentException(company.getName() + ": " + label(intake)
                    + " is for students of another institution.");
        }
        if (!isOpen(intake)) {
            throw new ApplicationWindowClosedException(closedMessage(company, intake, institutionId));
        }
        return intake;
    }

    /** Required document types of the company the student has not uploaded, by name. */
    public List<String> missingDocuments(Long companyId, Set<String> uploadedTypeNames) {
        return documentTypeService.getEffectiveRequiredTypeNames(companyId).stream()
                .filter(name -> !uploadedTypeNames.contains(name))
                .sorted()
                .toList();
    }

    /** An intake in plain words: "Semester 2, 2027". */
    public static String label(Intake intake) {
        return intake.getName() + ", " + intake.getAcademicYear().getName();
    }

    public static String formatDay(LocalDate day) {
        return day.format(DAY);
    }

    private Intake onlyOpenIntake(Tenant company, Long institutionId) {
        List<Intake> open = openIntakes(company.getId(), institutionId);
        if (open.size() == 1) {
            return open.get(0);
        }
        if (open.isEmpty()) {
            String next = nextToOpen(company.getId(), institutionId, Set.of())
                    .map(i -> " Applications for " + label(i) + " open on " + formatDay(i.getApplicationOpenDate()) + ".")
                    .orElse("");
            throw new ApplicationWindowClosedException(
                    company.getName() + " is not taking applications right now." + next);
        }
        throw new IllegalArgumentException("Choose an intake for " + company.getName() + ": "
                + open.stream().map(CompanyIntakePolicy::label).collect(Collectors.joining(" or ")) + ".");
    }

    private String closedMessage(Tenant company, Intake intake, Long institutionId) {
        if (academicYearService.today().isBefore(intake.getApplicationOpenDate())) {
            return company.getName() + " opens applications for " + label(intake) + " on "
                    + formatDay(intake.getApplicationOpenDate()) + ".";
        }
        String instead = openIntakes(company.getId(), institutionId).stream()
                .filter(i -> !Objects.equals(i.getId(), intake.getId()))
                .findFirst()
                .map(i -> " You can apply for " + label(i) + " instead.")
                .orElseGet(() -> nextToOpen(company.getId(), institutionId, Set.of())
                        .map(i -> " Applications for " + label(i) + " open on "
                                + formatDay(i.getApplicationOpenDate()) + ".")
                        .orElse(""));
        return "Applications to " + company.getName() + " for " + label(intake) + " closed on "
                + formatDay(intake.getApplicationCloseDate()) + "." + instead;
    }

    private List<Intake> intakesOf(List<AcademicYear> years) {
        if (years.isEmpty()) {
            return List.of();
        }
        return intakeRepository.findByAcademicYear_IdInOrderByStayStartDateAscNameAsc(
                years.stream().map(AcademicYear::getId).toList());
    }

    private boolean isOpen(Intake intake) {
        return academicYearService.isOpenToday(intake.getApplicationOpenDate(), intake.getApplicationCloseDate());
    }

    private static boolean overlaps(AcademicYear a, AcademicYear b) {
        return !a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate());
    }
}
