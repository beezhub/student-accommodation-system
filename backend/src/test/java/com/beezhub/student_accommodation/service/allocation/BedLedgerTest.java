package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/** GH-496: the batch run counts beds by stay dates, including the stays it has already matched. */
@ExtendWith(MockitoExtension.class)
class BedLedgerTest {

    private static final StayPeriod SEMESTER_1 = new StayPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30));
    private static final StayPeriod SEMESTER_2 = new StayPeriod(LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30));
    private static final StayPeriod ANNUAL = new StayPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    private BedLedger ledger(int capacity) {
        Room room = new Room();
        room.setId(1L);
        room.setCapacity(capacity);
        lenient().when(roomAllocationRepository.countHoldingBeds(eq(1L), any(), any())).thenReturn(0L);
        return new BedLedger(List.of(room), roomAllocationRepository);
    }

    @Test
    void tryTake_semester1AndSemester2_shareOneBed() {
        BedLedger ledger = ledger(1);

        assertThat(ledger.tryTake(1L, SEMESTER_1)).isTrue();
        assertThat(ledger.tryTake(1L, SEMESTER_2)).isTrue();
    }

    @Test
    void tryTake_annualAfterASemester_refused() {
        BedLedger ledger = ledger(1);

        assertThat(ledger.tryTake(1L, SEMESTER_1)).isTrue();
        assertThat(ledger.tryTake(1L, ANNUAL)).isFalse();
    }

    @Test
    void tryTake_sameStayTwiceInASingleRoom_secondRefused() {
        BedLedger ledger = ledger(1);

        assertThat(ledger.tryTake(1L, ANNUAL)).isTrue();
        assertThat(ledger.tryTake(1L, ANNUAL)).isFalse();
    }

    @Test
    void tryTake_bedAlreadyBookedInTheDatabase_refused() {
        BedLedger ledger = ledger(1);
        lenient().when(roomAllocationRepository.countHoldingBeds(1L, ANNUAL.from(), ANNUAL.until())).thenReturn(1L);

        assertThat(ledger.tryTake(1L, ANNUAL)).isFalse();
    }

    @Test
    void tryTake_unknownRoom_refused() {
        BedLedger ledger = ledger(1);

        assertThat(ledger.tryTake(2L, ANNUAL)).isFalse();
    }
}
