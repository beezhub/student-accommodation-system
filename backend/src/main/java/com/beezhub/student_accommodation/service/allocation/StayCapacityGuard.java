package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException;
import com.beezhub.student_accommodation.exceptions.RoomStillOccupiedException;
import com.beezhub.student_accommodation.exceptions.StayEndedException;
import com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The capacity rules by planned stay dates (GH-496), shared by every path that offers, moves or
 * checks in a student.
 *
 * <ul>
 *   <li>A room has a free bed for a stay when fewer OFFERED, ACCEPTED and ACTIVE allocations than
 *       its capacity have a planned stay overlapping it.</li>
 *   <li>A student can hold one bed per stay: a 2026 resident can be offered a 2027 room.</li>
 *   <li>A stay that has ended is never offered.</li>
 *   <li>Check-in needs a bed that is physically free: a resident who has not checked out still
 *       holds it, whatever the planned dates say.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StayCapacityGuard {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final RoomAllocationRepository roomAllocationRepository;
    private final Clock clock;

    /** Today in South African time, the day every stay date is read against. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Beds taken in the room during the stay, by planned dates. */
    public long bedsTaken(Room room, StayPeriod stay) {
        return roomAllocationRepository.countHoldingBeds(room.getId(), stay.from(), stay.until());
    }

    /** Throws unless the room has a free bed for the stay. */
    public void assertBedFree(Room room, StayPeriod stay) {
        long taken = bedsTaken(room, stay);
        if (room.getCapacity() != null && taken >= room.getCapacity()) {
            log.warn("action=roomFullForStay roomId={} taken={} capacity={} stayStart={} stayEnd={}",
                    room.getId(), taken, room.getCapacity(), stay.start(), stay.end());
            throw new RoomCapacityExceededException("Room " + room.getRoomNumber()
                    + " is at full capacity (" + room.getCapacity() + " occupants)" + forDates(stay));
        }
    }

    /** Throws when the student already holds a bed for dates overlapping the stay. */
    public void assertStudentFree(Long studentId, StayPeriod stay) {
        if (roomAllocationRepository.existsLiveAllocationForStudentDuring(studentId, stay.from(), stay.until())) {
            log.warn("action=studentAlreadyHousedForStay studentId={} stayStart={} stayEnd={}",
                    studentId, stay.start(), stay.end());
            throw new StudentAlreadyAllocatedException(
                    "Student with ID " + studentId + " already has an active room allocation" + forDates(stay));
        }
    }

    /** Throws when the stay is already over: a room is never offered for it. */
    public void assertNotEnded(StayPeriod stay) {
        if (stay.hasEnded(today())) {
            throw new StayEndedException("This stay ended on " + stay.end().format(DAY)
                    + ", so a room can no longer be offered for it.");
        }
    }

    /**
     * Throws while every bed in the room is still physically taken by a checked-in student. The
     * next student's offer stands, since capacity follows the planned dates, but they cannot move
     * in until a resident checks out.
     */
    public void assertBedPhysicallyFree(RoomAllocation allocation) {
        assertBedPhysicallyFree(allocation.getRoom(), allocation.getId());
    }

    /** As {@link #assertBedPhysicallyFree(RoomAllocation)}, for a student moving into {@code room}. */
    public void assertBedPhysicallyFree(Room room, Long allocationId) {
        if (room == null || room.getCapacity() == null) {
            return;
        }
        long checkedIn = roomAllocationRepository.countByRoom_IdAndStatus(room.getId(), AllocationStatus.ACTIVE);
        if (checkedIn >= room.getCapacity()) {
            log.warn("action=checkInRejected reason=roomStillOccupied allocationId={} roomId={} checkedIn={}",
                    allocationId, room.getId(), checkedIn);
            throw new RoomStillOccupiedException("Room " + room.getRoomNumber()
                    + " is still occupied: a resident has not checked out yet. Check them out first.");
        }
    }

    /**
     * When the check-in clock starts: move-in day, or now when the stay has already started or has
     * no planned dates. An offer made months ahead must not pass its check-in deadline before the
     * student arrives.
     */
    public LocalDateTime checkInFrom(StayPeriod stay) {
        LocalDateTime now = LocalDateTime.now();
        if (stay.start() == null) {
            return now;
        }
        LocalDateTime moveIn = stay.start().atStartOfDay();
        return moveIn.isAfter(now) ? moveIn : now;
    }

    private static String forDates(StayPeriod stay) {
        if (stay.start() == null || stay.end() == null) {
            return "";
        }
        return " for " + stay.start().format(DAY) + " to " + stay.end().format(DAY);
    }
}
