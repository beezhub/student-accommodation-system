package com.beezhub.student_accommodation.service.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.beezhub.student_accommodation.model.entity.AllocationRule;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.repository.AllocationRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GH-225: {@code fcfsWeight} was configurable and inert. The arrival term was the maximum constant
 * for every room of every student, and the queue was sorted on {@code createdAt} alone — so no
 * weight, arrival or need, could influence who was served first.
 */
@ExtendWith(MockitoExtension.class)
class ApplicantPriorityScorerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Mock private AllocationRuleRepository allocationRuleRepository;

    @InjectMocks private ApplicantPriorityScorer scorer;

    /** Weights as seeded: need at 1.0 against a 100-point flag, arrival at 0.5. */
    private static AllocationRule seededRule() {
        return AllocationRule.builder()
                .ruleName("Seeded")
                .fcfsWeight(new BigDecimal("0.5"))
                .nsfasPriorityWeight(BigDecimal.ONE)
                .universityPrefWeight(BigDecimal.ONE)
                .accessibilityWeight(BigDecimal.ONE)
                .active(true)
                .priority(0)
                .build();
    }

    private static WaitingListEntry entry(Long id, LocalDateTime createdAt) {
        Student student = new Student();
        student.setId(id);
        Application application = new Application();
        application.setId(id);
        application.setRequiresAccessibility(false);

        WaitingListEntry entry = new WaitingListEntry();
        entry.setId(id);
        entry.setCreatedAt(createdAt);
        entry.setStudent(student);
        entry.setApplication(application);
        return entry;
    }

    // ── The arrival term ────────────────────────────────────────────────────────────────────

    @Test
    void arrivalScoreRisesWithDaysWaited() {
        assertThat(scorer.arrivalScore(NOW, NOW)).isEqualByComparingTo("0");
        assertThat(scorer.arrivalScore(NOW.minusDays(15), NOW)).isEqualByComparingTo("50");
        assertThat(scorer.arrivalScore(NOW.minusDays(30), NOW)).isEqualByComparingTo("100");
    }

    /** Past the cap everyone still waiting scores the same, so age cannot accumulate for ever. */
    @Test
    void arrivalScoreStopsAtTheCap() {
        assertThat(scorer.arrivalScore(NOW.minusDays(31), NOW)).isEqualByComparingTo("100");
        assertThat(scorer.arrivalScore(NOW.minusDays(400), NOW)).isEqualByComparingTo("100");
    }

    /**
     * The term is an absolute clock, not a rank within the pool, so a student's score does not
     * change when other people join or leave the queue (GH-140).
     */
    @Test
    void arrivalScoreDoesNotDependOnTheRestOfThePool() {
        WaitingListEntry alone = entry(1L, NOW.minusDays(10));
        BigDecimal inPoolOfOne = scorer.priorityOf(alone, seededRule(), NOW);

        List<WaitingListEntry> crowded = new ArrayList<>(List.of(alone));
        for (long id = 2; id <= 50; id++) {
            crowded.add(entry(id, NOW.minusDays(id)));
        }
        Map<Long, BigDecimal> priorities =
                scorer.priorityByEntryId(crowded, seededRule(), NOW);

        assertThat(priorities.get(1L)).isEqualByComparingTo(inPoolOfOne);
    }

    @Test
    void aMissingArrivalTimeScoresZeroRatherThanThrowing() {
        assertThat(scorer.arrivalScore(null, NOW)).isEqualByComparingTo("0");
        assertThat(scorer.priorityOf(entry(1L, null), seededRule(), NOW)).isEqualByComparingTo("0");
    }

    // ── The policy the weights express ──────────────────────────────────────────────────────

    /**
     * The decision behind the seeded weights: a student with a need outranks one who has waited
     * longer without one. With every weight at 1.0 these two would have tied exactly, which is what
     * the issue flags as probably unintended.
     */
    @Test
    void aNeedOutranksAFullMonthOfWaiting() {
        WaitingListEntry waitedLongest = entry(1L, NOW.minusDays(30));

        WaitingListEntry hasBursary = entry(2L, NOW);
        hasBursary.getApplication().setSpecialRequirements("NSFAS funded");

        BigDecimal waiting = scorer.priorityOf(waitedLongest, seededRule(), NOW);
        BigDecimal need = scorer.priorityOf(hasBursary, seededRule(), NOW);

        assertThat(need).isGreaterThan(waiting);
        assertThat(waiting).isEqualByComparingTo("50.0000");   // 100 arrival x 0.5
        assertThat(need).isEqualByComparingTo("100.0000");     // 100 bursary x 1.0
    }

    /** Among students with the same need, the one who has waited longer wins. */
    @Test
    void waitingBreaksTiesBetweenStudentsOfEqualNeed() {
        WaitingListEntry earlier = entry(1L, NOW.minusDays(20));
        earlier.getApplication().setSpecialRequirements("NSFAS funded");
        WaitingListEntry later = entry(2L, NOW.minusDays(2));
        later.getApplication().setSpecialRequirements("NSFAS funded");

        assertThat(scorer.priorityOf(earlier, seededRule(), NOW))
                .isGreaterThan(scorer.priorityOf(later, seededRule(), NOW));
    }

    @Test
    void needsStack() {
        WaitingListEntry both = entry(1L, NOW);
        both.getApplication().setSpecialRequirements("NSFAS bursary");
        both.getApplication().setRequiresAccessibility(true);

        assertThat(scorer.priorityOf(both, seededRule(), NOW)).isEqualByComparingTo("200.0000");
    }

    @Test
    void accessibilityNeedIsAboutTheStudentNotAboutAnyRoom() {
        WaitingListEntry needsAccess = entry(1L, NOW);
        needsAccess.getApplication().setRequiresAccessibility(true);

        // No room is involved at all: this ranks applicants, and the room-scoring equivalent
        // rewards a match, which cannot rank applicants because it depends on the room.
        assertThat(scorer.needsAccessibleAccommodation(
                needsAccess.getStudent(), needsAccess.getApplication())).isTrue();
        assertThat(scorer.priorityOf(needsAccess, seededRule(), NOW)).isEqualByComparingTo("100.0000");
    }

    @Test
    void priorityInstitutionIsReadFromTheRulesList() {
        AllocationRule rule = seededRule();
        rule.setPriorityInstitutionIds("7, 9");

        WaitingListEntry listed = entry(1L, NOW);
        Institution institution = new Institution();
        institution.setId(9L);
        listed.getStudent().setInstitution(institution);

        WaitingListEntry notListed = entry(2L, NOW);
        Institution other = new Institution();
        other.setId(3L);
        notListed.getStudent().setInstitution(other);

        assertThat(scorer.isPriorityInstitution(listed.getStudent(), rule)).isTrue();
        assertThat(scorer.isPriorityInstitution(notListed.getStudent(), rule)).isFalse();
    }

    @Test
    void aWeightOfZeroRemovesItsComponent() {
        AllocationRule noArrival = seededRule();
        noArrival.setFcfsWeight(BigDecimal.ZERO);

        assertThat(scorer.priorityOf(entry(1L, NOW.minusDays(30)), noArrival, NOW))
                .isEqualByComparingTo("0");
    }

    // ── Ordering ────────────────────────────────────────────────────────────────────────────

    @Test
    void queueOrderIsPriorityThenArrivalThenId() {
        WaitingListEntry waitedLongest = entry(1L, NOW.minusDays(30));
        WaitingListEntry newcomerWithNeed = entry(2L, NOW);
        newcomerWithNeed.getApplication().setSpecialRequirements("bursary");
        WaitingListEntry waitedALittle = entry(3L, NOW.minusDays(5));

        List<WaitingListEntry> queue =
                new ArrayList<>(List.of(waitedALittle, waitedLongest, newcomerWithNeed));
        Map<Long, BigDecimal> priorities = scorer.priorityByEntryId(queue, seededRule(), NOW);
        queue.sort(scorer.queueOrder(priorities));

        assertThat(queue).extracting(WaitingListEntry::getId).containsExactly(2L, 1L, 3L);
    }

    /**
     * Priority is a sum of flags worth 100 or 0 plus arrival, so exact ties are common. Without the
     * tie-breaks, equal-priority students would fall through sort stability to whatever order the
     * database happened to return.
     */
    @Test
    void identicalPrioritiesFallBackToArrivalThenId() {
        WaitingListEntry sameTimeHigherId = entry(9L, NOW.minusDays(3));
        WaitingListEntry sameTimeLowerId = entry(4L, NOW.minusDays(3));
        WaitingListEntry earlier = entry(7L, NOW.minusDays(3).minusHours(1));

        List<WaitingListEntry> queue =
                new ArrayList<>(List.of(sameTimeHigherId, sameTimeLowerId, earlier));
        queue.sort(scorer.queueOrder(scorer.priorityByEntryId(queue, seededRule(), NOW)));

        assertThat(queue).extracting(WaitingListEntry::getId).containsExactly(7L, 4L, 9L);
    }

    /** The map is built up front precisely so no rule lookup runs inside a comparator (GH-140). */
    @Test
    void sortingNeverTouchesTheRuleRepository() {
        List<WaitingListEntry> queue = new ArrayList<>();
        for (long id = 1; id <= 20; id++) {
            queue.add(entry(id, NOW.minusDays(id)));
        }
        Map<Long, BigDecimal> priorities = scorer.priorityByEntryId(queue, seededRule(), NOW);

        queue.sort(scorer.queueOrder(priorities));

        verifyNoInteractions(allocationRuleRepository);
    }

    // ── Rule resolution ─────────────────────────────────────────────────────────────────────

    @Test
    void tenantRuleTakesTheHighestPriorityActiveRule() {
        AllocationRule low = seededRule();
        low.setPriority(1);
        AllocationRule high = seededRule();
        high.setPriority(5);
        high.setRuleName("Chosen");
        when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue())
                .thenReturn(List.of(low, high));

        assertThat(scorer.tenantRule().getRuleName()).isEqualTo("Chosen");
    }

    @Test
    void tenantRuleFallsBackToNeutralWeightsWhenNoneIsConfigured() {
        when(allocationRuleRepository.findByBuildingIsNullAndActiveTrue()).thenReturn(List.of());

        AllocationRule fallback = scorer.tenantRule();

        assertThat(fallback.getFcfsWeight()).isEqualByComparingTo("1");
        assertThat(fallback.getNsfasPriorityWeight()).isEqualByComparingTo("1");
    }
}
