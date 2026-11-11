package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import com.beezhub.student_accommodation.exceptions.InvalidAllocationStatusTransitionException;
import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.document.DocumentReadinessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationCheckInService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final UserNotificationService userNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomOccupancyManager roomOccupancyManager;
    private final AllocationAuditService allocationAuditService;
    private final AuthService authService;
    private final StayCapacityGuard stayCapacityGuard;
    private final DocumentReadinessGuard documentReadinessGuard;

    /** Human-readable form for the deadline echoed to the admin who is past it. */
    private static final DateTimeFormatter CHECK_IN_DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    /** Recorded in the audit log's constraint_bypassed column when an admin overrides. */
    static final String CHECK_IN_DEADLINE_CONSTRAINT = "CHECK_IN_DEADLINE";

    /**
     * Checks a student in, refusing once the check-in deadline has passed.
     *
     * @param overrideDeadline lets an admin check a student in anyway. Recorded in the audit log.
     */
    @Transactional
    public RoomAllocationResponse confirmCheckIn(Long id, boolean overrideDeadline) {
        log.info("Confirming check-in for allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        // Before the transition check, because AllocationStatus.canTransitionTo permits a
        // self-transition: ACTIVE -> ACTIVE passed, so a second check-in succeeded and re-stamped
        // checkInDate with the later attempt — losing the record of when the student actually
        // moved in. GH-138's note says repeat-sensitive callers must test this explicitly; this
        // one did not.
        if (allocation.getStatus() == AllocationStatus.ACTIVE) {
            throw new InvalidAllocationStatusTransitionException(
                    "This student is already checked in"
                            + (allocation.getCheckInDate() == null
                                    ? "." : " (since " + allocation.getCheckInDate() + ")."));
        }

        AllocationStatusValidator.validate(allocation, AllocationStatus.ACTIVE);
        boolean pastDeadline = assertWithinCheckInDeadline(allocation, overrideDeadline);
        // The offer stands on the planned dates, but nobody moves into a bed a resident still
        // sleeps in. The admin dashboard lists the overdue check-out (GH-496).
        stayCapacityGuard.assertBedPhysicallyFree(allocation);
        // An expired or unapproved required document holds the check-in back (GH-498). Not
        // covered by the deadline override: the replacement needs approving first.
        documentReadinessGuard.assertReadyForCheckIn(allocation);
        // After the allocation guard, so an illegal allocation transition still reports as such
        // rather than being masked by the application state (GH-137).
        ApplicationStatusValidator.validate(allocation.getApplication(),
                com.beezhub.student_accommodation.model.enums.ApplicationStatus.CHECKED_IN);
        allocation.setStatus(AllocationStatus.ACTIVE);
        allocation.setCheckInDate(LocalDateTime.now());

        if (allocation.getApplication() != null) {
            Application application = allocation.getApplication();
            application.setStatus(com.beezhub.student_accommodation.model.enums.ApplicationStatus.CHECKED_IN);
            applicationRepository.save(application);
            log.info("Updated application {} status to CHECKED_IN", application.getId());
        }

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        // Every other allocation change recounts the room; check-in did not, so a room whose
        // residents had all checked in still read as empty on the admin dashboard (GH-541).
        roomOccupancyManager.syncOccupancy(saved.getRoom());
        log.info("Successfully checked in allocation with id: {}", id);

        if (pastDeadline) {
            recordDeadlineOverride(saved);
        }

        try {
            if (allocation.getStudent() != null && allocation.getStudent().getAppUser() != null) {
                userNotificationService.createCheckInConfirmedNotification(
                    allocation.getStudent().getAppUser().getId(),
                    allocation.getRoom().getRoomNumber(),
                    allocation.getRoom().getBuilding() != null ? 
                        allocation.getRoom().getBuilding().getName() : "Unknown Building"
                );
            }
        } catch (Exception e) {
            log.error("Failed to send check-in confirmation notification for allocation {}", id, e);
        }
        
        return roomAllocationMapper.toResponse(saved);
    }

    /**
     * Refuses a check-in past its deadline unless the admin has explicitly overridden it.
     *
     * <p>{@code confirmCheckIn} never read {@code checkInExpiryDate}, so the only thing enforcing
     * the deadline was the hourly sweep and an ACCEPTED allocation stayed checkable-in for up to an
     * hour past it. This is the same gap the offer deadline had before GH-139, but the remedy
     * differs because this endpoint is admin-triggered rather than student-triggered: an admin
     * checking someone in slightly late is a judgement they may legitimately want to make, so it is
     * refused by default and permitted deliberately, not silently allowed (GH-197).
     *
     * <p>{@code isBefore} matches the predicate the sweep query uses, so there is no instant at
     * which one admits a row the other would expire. A null deadline is treated as no deadline, for
     * the same reason: a null row is equally invisible to the sweep.
     *
     * @return whether the deadline had passed, so the caller can record the override
     */
    private boolean assertWithinCheckInDeadline(RoomAllocation allocation, boolean overrideDeadline) {
        LocalDateTime deadline = allocation.getCheckInExpiryDate();
        if (deadline == null || !deadline.isBefore(LocalDateTime.now())) {
            return false;
        }
        if (!overrideDeadline) {
            log.warn("action=checkInRejected reason=deadlinePassed allocationId={} checkInExpiryDate={}",
                    allocation.getId(), deadline);
            throw new IllegalStateException("The check-in deadline for this allocation passed on "
                    + deadline.format(CHECK_IN_DEADLINE_FORMAT)
                    + ". Check in anyway by confirming the override, which is recorded against the "
                    + "allocation.");
        }
        log.warn("action=checkInDeadlineOverridden allocationId={} checkInExpiryDate={}",
                allocation.getId(), deadline);
        return true;
    }

    /** Writes the override to the allocation's audit trail, so a late check-in is traceable. */
    private void recordDeadlineOverride(RoomAllocation allocation) {
        AppUser admin = null;
        try {
            admin = authService.getCurrentUser();
        } catch (RuntimeException e) {
            // An unresolvable caller must not lose the record that an override happened.
            log.warn("action=checkInOverrideAdminUnresolved allocationId={}", allocation.getId());
        }
        allocationAuditService.logAuditActionWithConstraint(
                allocation,
                AllocationAuditActionType.OVERRIDE,
                admin,
                "Checked in after the deadline of "
                        + allocation.getCheckInExpiryDate().format(CHECK_IN_DEADLINE_FORMAT),
                null,
                allocation.getRoom(),
                CHECK_IN_DEADLINE_CONSTRAINT);
    }

    @Transactional
    public RoomAllocationResponse confirmCheckOut(Long id) {
        log.info("Confirming check-out for allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        assertNotAlreadyClosed(allocation, "check out");

        AllocationStatusValidator.validate(allocation, AllocationStatus.COMPLETED);
        allocation.setStatus(AllocationStatus.COMPLETED);
        allocation.setCheckOutDate(LocalDateTime.now());
        allocation.setEndDate(LocalDateTime.now());

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        roomOccupancyManager.syncOccupancy(allocation.getRoom());
        log.info("Successfully checked out allocation with id: {}", id);

        // RoomVacatedEvent, not AllocationFreedEvent. Nothing listened for the latter: it was a
        // leftover from before the decline and cancel paths were migrated, so every check-out and
        // end published into the void and the freed room was never refilled (GH-138).
        eventPublisher.publishEvent(new RoomVacatedEvent(
                allocation.getId(), roomIdOf(allocation), AllocationStatus.COMPLETED));

        return roomAllocationMapper.toResponse(saved);
    }

    /**
     * Ends a lease early. This is the administrative counterpart of a student checking out.
     *
     * <p>The status is TERMINATED ("Lease cancelled early") rather than COMPLETED ("Student moved
     * out naturally"). Both methods wrote COMPLETED, which made TERMINATED unreachable despite
     * ACTIVE -> TERMINATED being a legal transition, and lost the distinction between the two
     * (GH-138).
     */
    @Transactional
    public RoomAllocationResponse endAllocation(Long id) {
        log.info("Ending room allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        assertNotAlreadyClosed(allocation, "end");

        AllocationStatusValidator.validate(allocation, AllocationStatus.TERMINATED);
        allocation.setStatus(AllocationStatus.TERMINATED);
        allocation.setEndDate(LocalDateTime.now());

        RoomAllocation updated = roomAllocationRepository.save(allocation);
        roomOccupancyManager.syncOccupancy(allocation.getRoom());
        log.info("Successfully ended room allocation with id: {}", id);

        eventPublisher.publishEvent(new RoomVacatedEvent(
                allocation.getId(), roomIdOf(allocation), AllocationStatus.TERMINATED));

        return roomAllocationMapper.toResponse(updated);
    }

    /**
     * Refuses to close an allocation that is already closed.
     *
     * <p>The status machine permits a self-transition, so a second check-out on a COMPLETED
     * allocation passed validation and ran the whole method again. Under the old occupancy delta
     * that decremented the room a second time; occupancy is derived now, but the method would
     * still re-stamp the dates and publish a second vacated event (GH-138).
     */
    private void assertNotAlreadyClosed(RoomAllocation allocation, String action) {
        if (allocation.getStatus() != null && allocation.getStatus().isTerminal()) {
            throw new InvalidAllocationStatusTransitionException(
                    "Cannot " + action + " an allocation that is already "
                            + allocation.getStatus().getStatus().toLowerCase());
        }
    }

    /** Null-safe, because an allocation is not guaranteed to still hold a room. */
    private static Long roomIdOf(RoomAllocation allocation) {
        return allocation.getRoom() != null ? allocation.getRoom().getId() : null;
    }

    private RoomAllocation getAllocation(Long id) {
        return roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));
    }
}
