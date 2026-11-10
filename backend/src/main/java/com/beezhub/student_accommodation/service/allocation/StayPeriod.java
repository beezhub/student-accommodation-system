package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;

import java.time.LocalDate;

/**
 * The planned dates of a stay in a room (GH-496): from move-in, and free again from the end date,
 * when the check-out is expected. The end date is exclusive, so a Semester 1 stay ending on 30 June
 * and a Semester 2 stay starting on 30 June share the bed without clashing.
 *
 * <p>An empty date means the stay is open on that side. An allocation with no planned dates (one
 * made without an application, or older than this change and with no intake to copy from) is
 * treated as open on both sides, so it keeps blocking the bed for any dates, as every allocation did
 * before.
 */
public record StayPeriod(LocalDate start, LocalDate end) {

    /** Stand-ins for an open side, inside the range PostgreSQL's DATE type stores. */
    static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
    static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

    /** A stay with no planned dates, which overlaps every other stay. */
    public static final StayPeriod UNBOUNDED = new StayPeriod(null, null);

    /**
     * The stay an application is for: its intake's stay dates, else its academic year's dates, else
     * open on both sides.
     */
    public static StayPeriod of(Application application) {
        if (application == null) {
            return UNBOUNDED;
        }
        Intake intake = application.getIntake();
        if (intake != null && intake.getStayStartDate() != null && intake.getStayEndDate() != null) {
            return new StayPeriod(intake.getStayStartDate(), intake.getStayEndDate());
        }
        AcademicYear year = application.getAcademicYear();
        if (year != null && year.getStartDate() != null && year.getEndDate() != null
                && year.getStartDate().isBefore(year.getEndDate())) {
            return new StayPeriod(year.getStartDate(), year.getEndDate());
        }
        return UNBOUNDED;
    }

    /** The planned stay of an allocation, falling back to its application's when none is stored. */
    public static StayPeriod of(RoomAllocation allocation) {
        if (allocation == null) {
            return UNBOUNDED;
        }
        if (allocation.getStayStartDate() != null || allocation.getStayEndDate() != null) {
            return new StayPeriod(allocation.getStayStartDate(), allocation.getStayEndDate());
        }
        return of(allocation.getApplication());
    }

    /** The one day {@code day}, for counting who is in a room on that day. */
    public static StayPeriod onDay(LocalDate day) {
        return new StayPeriod(day, day.plusDays(1));
    }

    /**
     * From {@code day} on, with no end: what is still promised from that day. Used where no stay is
     * asked about, such as a room's general availability, so a bed already promised for next year
     * is not shown as free.
     */
    public static StayPeriod startingOn(LocalDate day) {
        return new StayPeriod(day, null);
    }

    /** The first day, or a date before any stay when the start is open. Never null, for queries. */
    public LocalDate from() {
        return start == null ? EARLIEST : start;
    }

    /** The day the bed is free again, or a date after any stay when the end is open. Never null. */
    public LocalDate until() {
        return end == null ? LATEST : end;
    }

    /** Whether the stay is over on {@code today}: the bed is free from the end date. */
    public boolean hasEnded(LocalDate today) {
        return end != null && today != null && !today.isBefore(end);
    }

    /** Whether the two stays share at least one night. */
    public boolean overlaps(StayPeriod other) {
        return from().isBefore(other.until()) && other.from().isBefore(until());
    }

    /** Copies the planned dates onto an allocation. */
    public void applyTo(RoomAllocation allocation) {
        allocation.setStayStartDate(start);
        allocation.setStayEndDate(end);
    }
}
