package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.AllocationRule;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.repository.AllocationRuleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ranks applicants in a waiting-list queue.
 *
 * <p>GH-225: {@code rule.fcfsWeight} was configurable in the UI and inert in practice.
 * {@code OwnerConfigurableScoringStrategy} set the arrival term to the maximum constant
 * unconditionally, so the weight multiplied the same number for every room of every student and
 * could not influence any ranking. That was structural, not a missing line: the strategy scores
 * rooms for one student and never sees the other applicants, while arrival is a property of an
 * application relative to the queue.
 *
 * <p>So arrival is scored here, where the queue is, rather than there. The queue order was
 * previously arrival alone — a plain sort on {@code createdAt} — which meant the need weights could
 * not influence it either. Both now do.
 *
 * <h2>The three design constraints from GH-140</h2>
 * <ul>
 *   <li><b>Precomputed.</b> {@link #priorityByEntryId} builds the whole map before any sorting, so
 *       no rule lookup ever runs inside a comparator.</li>
 *   <li><b>One rule set per queue.</b> The caller passes the tenant-level rule, not each student's
 *       best room's building rule. Otherwise scores are not comparable across students and a
 *       student's arrival weight would depend on which rooms happen to be free.</li>
 *   <li><b>Absolute clock.</b> The arrival term is days waited against a cap, not a pool-relative
 *       rank, so a student's score does not move as other people join or leave the queue, and the
 *       admin screen and the batch job cannot disagree about it.</li>
 * </ul>
 */
@Component
@Slf4j
public class ApplicantPriorityScorer {

    private final AllocationRuleRepository allocationRuleRepository;

    public ApplicantPriorityScorer(AllocationRuleRepository allocationRuleRepository) {
        this.allocationRuleRepository = allocationRuleRepository;
    }

    /** Every component is scored out of this, so the weights are what express relative importance. */
    static final BigDecimal COMPONENT_MAX = new BigDecimal("100");

    /**
     * Days waited at which the arrival term reaches {@link #COMPONENT_MAX}.
     *
     * <p>Beyond it everyone still waiting scores the same on arrival, so a very old entry cannot
     * accumulate unbounded advantage over a merely old one.
     *
     * <p>This is a scoring horizon, not a service level: it does not shorten anyone's wait, it only
     * decides when waiting longer stops earning more.
     */
    static final long ARRIVAL_CAP_DAYS = 30L;

    /**
     * The one rule that ranks this queue: the current tenant's highest-priority active rule that is
     * not tied to a building.
     *
     * <p>Resolved here, once, rather than per student. Taking each student's best room's building
     * rule would make the scores incomparable — a student's arrival weight would depend on which
     * rooms happened to be free when the job ran (GH-140).
     *
     * <p>The tenant filter scopes the query, so callers must have the tenant in context. It only
     * applies inside a transaction, and the batch job calls this without one.
     */
    @Transactional(readOnly = true)
    public AllocationRule tenantRule() {
        return allocationRuleRepository.findByBuildingIsNullAndActiveTrue().stream()
                .max(Comparator.comparing(AllocationRule::getPriority,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseGet(ApplicantPriorityScorer::neutralRule);
    }

    /** All weights at 1, so a tenant with no rule configured is ranked by the raw components. */
    private static AllocationRule neutralRule() {
        return AllocationRule.builder()
                .ruleName("Default")
                .fcfsWeight(BigDecimal.ONE)
                .nsfasPriorityWeight(BigDecimal.ONE)
                .universityPrefWeight(BigDecimal.ONE)
                .accessibilityWeight(BigDecimal.ONE)
                .active(true)
                .priority(0)
                .build();
    }

    /**
     * A priority score for every entry, keyed by entry id.
     *
     * @param tenantRule the one rule for this queue; see the class notes on why it is not per-room
     * @param now        passed in so the batch job and the admin screen can score against the same
     *                   instant, and so tests are not clock-dependent
     */
    public Map<Long, BigDecimal> priorityByEntryId(
            List<WaitingListEntry> entries, AllocationRule tenantRule, LocalDateTime now) {
        Map<Long, BigDecimal> byEntry = new HashMap<>();
        for (WaitingListEntry entry : entries) {
            byEntry.put(entry.getId(), priorityOf(entry, tenantRule, now));
        }
        return byEntry;
    }

    /**
     * The queue order: highest priority first, then the earlier arrival, then the lower id.
     *
     * <p>The two tie-breaks matter. Priority is a weighted sum that will often tie exactly — every
     * component is a flag worth 100 or 0 except arrival — and without them equal-priority students
     * would fall through sort stability to whatever order the database returned.
     */
    public Comparator<WaitingListEntry> queueOrder(Map<Long, BigDecimal> priorityByEntryId) {
        return Comparator
                .comparing((WaitingListEntry entry) ->
                                priorityByEntryId.getOrDefault(entry.getId(), BigDecimal.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(WaitingListEntry::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WaitingListEntry::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /** The weighted sum for one entry. Package-private so the components can be asserted directly. */
    BigDecimal priorityOf(WaitingListEntry entry, AllocationRule rule, LocalDateTime now) {
        Student student = entry.getStudent();
        Application application = entry.getApplication();

        BigDecimal arrival = arrivalScore(entry.getCreatedAt(), now)
                .multiply(weight(rule.getFcfsWeight()));
        BigDecimal bursary = flag(hasBursary(student, application))
                .multiply(weight(rule.getNsfasPriorityWeight()));
        BigDecimal institution = flag(isPriorityInstitution(student, rule))
                .multiply(weight(rule.getUniversityPrefWeight()));
        BigDecimal accessibility = flag(needsAccessibleAccommodation(student, application))
                .multiply(weight(rule.getAccessibilityWeight()));

        return arrival.add(bursary).add(institution).add(accessibility)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Days waited, linear to the cap.
     *
     * <p>A null creation time scores zero rather than throwing. Such a row is invisible to the
     * arrival term but keeps its place through the tie-breaks in {@link #queueOrder}.
     */
    BigDecimal arrivalScore(LocalDateTime createdAt, LocalDateTime now) {
        if (createdAt == null || now == null || createdAt.isAfter(now)) {
            return BigDecimal.ZERO;
        }
        long daysWaited = Duration.between(createdAt, now).toDays();
        long capped = Math.min(daysWaited, ARRIVAL_CAP_DAYS);
        return COMPONENT_MAX
                .multiply(BigDecimal.valueOf(capped))
                .divide(BigDecimal.valueOf(ARRIVAL_CAP_DAYS), 4, RoundingMode.HALF_UP);
    }

    /**
     * Whether the student's funding is a bursary or NSFAS.
     *
     * <p>Keyword matching on free text, mirroring {@code OwnerConfigurableScoringStrategy}, so the
     * queue and the room scoring agree about who is funded. It is the same weak detection that
     * class documents as standing in for a dedicated column, and it should be replaced in both
     * places at once.
     */
    boolean hasBursary(Student student, Application application) {
        return mentionsFunding(application == null ? null : application.getSpecialRequirements())
                || mentionsFunding(student == null ? null : student.getSpecialRequirements());
    }

    private static boolean mentionsFunding(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("nsfas") || lower.contains("financial aid") || lower.contains("bursary");
    }

    boolean isPriorityInstitution(Student student, AllocationRule rule) {
        String priorityIds = rule.getPriorityInstitutionIds();
        if (priorityIds == null || priorityIds.isBlank()
                || student == null || student.getInstitution() == null
                || student.getInstitution().getId() == null) {
            return false;
        }
        String wanted = student.getInstitution().getId().toString();
        for (String id : priorityIds.split(",")) {
            if (wanted.equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the student needs accessible accommodation.
     *
     * <p>Deliberately a statement about the student, not about a room. The room-scoring equivalent
     * rewards a <em>match</em> between requirement and room, which cannot rank applicants because
     * it depends on which room is being considered.
     */
    boolean needsAccessibleAccommodation(Student student, Application application) {
        if (application != null && Boolean.TRUE.equals(application.getRequiresAccessibility())) {
            return true;
        }
        return mentionsAccessibility(application == null ? null : application.getSpecialRequirements())
                || mentionsAccessibility(student == null ? null : student.getSpecialRequirements());
    }

    /** The same keywords {@code OwnerConfigurableScoringStrategy.hasAccessibilityRequirements} uses. */
    private static boolean mentionsAccessibility(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("wheelchair") || lower.contains("accessible")
                || lower.contains("mobility") || lower.contains("disability")
                || lower.contains("handicap");
    }

    private static BigDecimal flag(boolean present) {
        return present ? COMPONENT_MAX : BigDecimal.ZERO;
    }

    /** A null weight is treated as 1, matching the entity defaults. */
    private static BigDecimal weight(BigDecimal configured) {
        return configured == null ? BigDecimal.ONE : configured;
    }
}
