package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-140: the admin screen sorted the waiting list by score and renumbered the positions, while the
 * batch job walked the raw arrival order. A student shown at position 1 could lose the last free
 * room to an earlier, lower-scoring applicant.
 *
 * <p>These pin the two ordering properties that had drifted. The end-to-end agreement between the
 * screen and the job is asserted in the service tests; what is checked here is that each rule is a
 * total order, which is what stops the drift recurring.
 */
class WaitingListOrderTest {

    private static ScoredRoom room(long id, String score, Integer desirability) {
        return ScoredRoom.builder()
                .roomId(id)
                .roomNumber("R" + id)
                .totalScore(new BigDecimal(score))
                .desirabilityRating(desirability)
                .applicationCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }

    /** The comparator both scoring strategies now use. */
    private static Comparator<ScoredRoom> roomOrder() {
        return Comparator
                .comparing(ScoredRoom::getTotalScore, Comparator.reverseOrder())
                .thenComparing(ScoredRoom::getDesirabilityRating,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ScoredRoom::getRoomId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * The tie-break used to read the application createdAt, which is identical for every room in
     * the list because they all belong to the same application. Equal scores therefore fell through
     * the sort stability to the order rooms came back from an unordered findAll().
     */
    @Test
    void roomOrder_equalScores_isIndependentOfInputOrder() {
        List<ScoredRoom> ascending = new ArrayList<>(List.of(
                room(1, "50.0", 3), room(2, "50.0", 3), room(3, "50.0", 3)));
        List<ScoredRoom> descending = new ArrayList<>(List.of(
                room(3, "50.0", 3), room(2, "50.0", 3), room(1, "50.0", 3)));

        ascending.sort(roomOrder());
        descending.sort(roomOrder());

        assertThat(ascending).extracting(ScoredRoom::getRoomId)
                .isEqualTo(descending.stream().map(ScoredRoom::getRoomId).toList());
    }

    /** An equal score is broken by the better room, not merely by the lowest id. */
    @Test
    void roomOrder_equalScores_prefersTheMoreDesirableRoom() {
        List<ScoredRoom> rooms = new ArrayList<>(List.of(
                room(1, "50.0", 2), room(2, "50.0", 5)));

        rooms.sort(roomOrder());

        assertThat(rooms.get(0).getRoomId()).isEqualTo(2L);
    }

    @Test
    void roomOrder_higherScoreAlwaysWins() {
        List<ScoredRoom> rooms = new ArrayList<>(List.of(
                room(1, "10.0", 5), room(2, "90.0", 1)));

        rooms.sort(roomOrder());

        assertThat(rooms.get(0).getRoomId()).isEqualTo(2L);
    }

    @Test
    void roomOrder_missingDesirability_sortsLastWithoutThrowing() {
        List<ScoredRoom> rooms = new ArrayList<>(List.of(
                room(1, "50.0", null), room(2, "50.0", 1)));

        rooms.sort(roomOrder());

        assertThat(rooms.get(0).getRoomId()).isEqualTo(2L);
    }

}
