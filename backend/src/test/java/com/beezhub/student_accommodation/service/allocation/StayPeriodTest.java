package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** GH-496: planned stay dates, with the end date exclusive. */
class StayPeriodTest {

    private static final StayPeriod SEMESTER_1 = new StayPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30));
    private static final StayPeriod SEMESTER_2 = new StayPeriod(LocalDate.of(2027, 7, 15), LocalDate.of(2027, 11, 30));
    private static final StayPeriod ANNUAL_2027 = new StayPeriod(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
    private static final StayPeriod ANNUAL_2026 = new StayPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));

    @Test
    void overlaps_semester1AndSemester2_doNotClash() {
        assertThat(SEMESTER_1.overlaps(SEMESTER_2)).isFalse();
        assertThat(SEMESTER_2.overlaps(SEMESTER_1)).isFalse();
    }

    @Test
    void overlaps_annualBlocksBothSemesters() {
        assertThat(ANNUAL_2027.overlaps(SEMESTER_1)).isTrue();
        assertThat(ANNUAL_2027.overlaps(SEMESTER_2)).isTrue();
    }

    @Test
    void overlaps_2026And2027_neverClash() {
        assertThat(ANNUAL_2026.overlaps(ANNUAL_2027)).isFalse();
    }

    @Test
    void overlaps_endDateIsFreeForTheNextStay() {
        StayPeriod next = new StayPeriod(LocalDate.of(2027, 6, 30), LocalDate.of(2027, 7, 30));
        assertThat(SEMESTER_1.overlaps(next)).isFalse();
    }

    @Test
    void overlaps_unboundedOverlapsEverything() {
        assertThat(StayPeriod.UNBOUNDED.overlaps(ANNUAL_2026)).isTrue();
        assertThat(StayPeriod.UNBOUNDED.from()).isEqualTo(StayPeriod.EARLIEST);
        assertThat(StayPeriod.UNBOUNDED.until()).isEqualTo(StayPeriod.LATEST);
    }

    @Test
    void hasEnded_fromTheEndDate() {
        assertThat(ANNUAL_2026.hasEnded(LocalDate.of(2026, 11, 29))).isFalse();
        assertThat(ANNUAL_2026.hasEnded(LocalDate.of(2026, 11, 30))).isTrue();
        assertThat(StayPeriod.UNBOUNDED.hasEnded(LocalDate.of(2026, 11, 30))).isFalse();
        assertThat(ANNUAL_2026.hasEnded(null)).isFalse();
    }

    @Test
    void of_applicationUsesIntakeThenYear() {
        Application application = new Application();
        assertThat(StayPeriod.of(application)).isEqualTo(StayPeriod.UNBOUNDED);

        AcademicYear year = new AcademicYear();
        year.setStartDate(LocalDate.of(2027, 1, 15));
        year.setEndDate(LocalDate.of(2027, 12, 10));
        application.setAcademicYear(year);
        assertThat(StayPeriod.of(application))
                .isEqualTo(new StayPeriod(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 12, 10)));

        Intake intake = new Intake();
        intake.setStayStartDate(SEMESTER_2.start());
        intake.setStayEndDate(SEMESTER_2.end());
        application.setIntake(intake);
        assertThat(StayPeriod.of(application)).isEqualTo(SEMESTER_2);
        assertThat(StayPeriod.of((Application) null)).isEqualTo(StayPeriod.UNBOUNDED);
    }

    @Test
    void of_allocationPrefersItsOwnDates() {
        RoomAllocation allocation = new RoomAllocation();
        assertThat(StayPeriod.of(allocation)).isEqualTo(StayPeriod.UNBOUNDED);

        SEMESTER_1.applyTo(allocation);
        assertThat(StayPeriod.of(allocation)).isEqualTo(SEMESTER_1);
        assertThat(StayPeriod.of((RoomAllocation) null)).isEqualTo(StayPeriod.UNBOUNDED);
    }

    @Test
    void startingOnAndOnDay() {
        LocalDate day = LocalDate.of(2026, 11, 10);
        assertThat(StayPeriod.startingOn(day).until()).isEqualTo(StayPeriod.LATEST);
        assertThat(StayPeriod.onDay(day).end()).isEqualTo(day.plusDays(1));
    }
}
