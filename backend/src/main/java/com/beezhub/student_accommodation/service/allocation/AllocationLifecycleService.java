package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.event.AllocationCreatedEvent;
import com.beezhub.student_accommodation.model.event.AllocationMovedEvent;
import com.beezhub.student_accommodation.model.event.AllocationAcceptedEvent;
import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import com.beezhub.student_accommodation.model.enums.*;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.document.DocumentReadinessGuard;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class AllocationLifecycleService {

    /** Human-readable form for the deadline echoed to the student when an offer has lapsed. */
    private static final DateTimeFormatter OFFER_DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    @Value("${app.allocation.offer-expiry-days:7}")
    private int offerExpiryDays;

    @Value("${app.allocation.check-in-expiry-days:7}")
    private int checkInExpiryDays;

    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationMapper roomAllocationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final WaitingListService waitingListService;
    private final RoomOccupancyManager roomOccupancyManager;
    private final AllocationMoveService allocationMoveService;
    private final AllocationAuditService allocationAuditService;
    private final MeterRegistry meterRegistry;
    private final AllocationAccessGuard allocationAccessGuard;
    private final AuthService authService;
    private final UserNotificationService userNotificationService;
    private final TenantOwnershipGuard tenantOwnershipGuard;
    private final StayCapacityGuard stayCapacityGuard;
    private final DocumentReadinessGuard documentReadinessGuard;

    public AllocationLifecycleService(
            RoomAllocationRepository roomAllocationRepository,
            StudentRepository studentRepository,
            RoomRepository roomRepository,
            ApplicationRepository applicationRepository,
            RoomAllocationMapper roomAllocationMapper,
            ApplicationEventPublisher eventPublisher,
            WaitingListService waitingListService,
            RoomOccupancyManager roomOccupancyManager,
            AllocationMoveService allocationMoveService,
            AllocationAuditService allocationAuditService,
            MeterRegistry meterRegistry,
            AllocationAccessGuard allocationAccessGuard,
            AuthService authService,
            UserNotificationService userNotificationService,
            TenantOwnershipGuard tenantOwnershipGuard,
            StayCapacityGuard stayCapacityGuard,
            DocumentReadinessGuard documentReadinessGuard) {
        this.roomAllocationRepository = roomAllocationRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.applicationRepository = applicationRepository;
        this.roomAllocationMapper = roomAllocationMapper;
        this.eventPublisher = eventPublisher;
        this.waitingListService = waitingListService;
        this.roomOccupancyManager = roomOccupancyManager;
        this.allocationMoveService = allocationMoveService;
        this.allocationAuditService = allocationAuditService;
        this.meterRegistry = meterRegistry;
        this.allocationAccessGuard = allocationAccessGuard;
        this.authService = authService;
        this.userNotificationService = userNotificationService;
        this.tenantOwnershipGuard = tenantOwnershipGuard;
        this.stayCapacityGuard = stayCapacityGuard;
        this.documentReadinessGuard = documentReadinessGuard;
    }

    /**
     * Creates a new room allocation with comprehensive business rule validation.
     */
    @Transactional
    public RoomAllocationResponse createRoomAllocation(RoomAllocationRequest request) {
        log.info("Creating new room allocation for student: {} to room: {}", request.getStudentId(),
                request.getRoomId());

        Student student = getStudent(request.getStudentId());
        Room room = getRoom(request.getRoomId());

        Application application = null;
        if (request.getApplicationId() != null) {
            application = validateAndGetApplication(request.getApplicationId(), student.getId());
        }

        // Capacity and the one-bed-per-student rule follow the planned stay (GH-496), so a 2026
        // resident can be offered a 2027 room and a 2027 offer does not take a 2026 bed.
        StayPeriod stay = StayPeriod.of(application);
        stayCapacityGuard.assertNotEnded(stay);
        stayCapacityGuard.assertStudentFree(student.getId(), stay);
        stayCapacityGuard.assertBedFree(room, stay);
        // Batch, manual, staged and per-application offers all come through here (GH-498).
        documentReadinessGuard.assertReadyForRoom(student, room.getTenantId(), application);

        RoomAllocation allocation = buildNewAllocation(request, student, room, application);
        stay.applyTo(allocation);
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        allocationAuditService.logStatusChange(saved, null, AllocationStatus.OFFERED);

        waitingListService.updateStatusToAllocated(student.getId());
        roomOccupancyManager.syncOccupancy(room);

        sendAllocationNotification(saved);

        meterRegistry.counter("sas.allocations.created").increment();

        log.info("Successfully created room allocation with id: {}", saved.getId());
        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse acceptOffer(Long id, AcceptOfferRequest request) {
        log.info("Accepting room allocation offer with id: {}", id);
        RoomAllocation allocation = getAllocation(id);
        allocationAccessGuard.assertCanAccessAllocation(allocation);

        // The check-in clock starts on move-in day, not today, so an offer accepted months ahead
        // keeps its deadline until the student can arrive (GH-496).
        LocalDateTime checkInFrom = stayCapacityGuard.checkInFrom(StayPeriod.of(allocation));
        LocalDateTime expectedCheckIn = request.getExpectedCheckInDate() != null
                ? request.getExpectedCheckInDate() : checkInFrom;
        validateOfferAcceptance(allocation, expectedCheckIn, checkInFrom);

        AllocationStatus previousStatus = allocation.getStatus();
        allocation.setStatus(AllocationStatus.ACCEPTED);
        allocation.setExpectedCheckInDate(expectedCheckIn);
        allocation.setCheckInExpiryDate(checkInFrom.plusDays(checkInExpiryDays));
        // Places accepted long before move-in are asked to reconfirm (GH-496).
        allocation.setAcceptedAt(LocalDateTime.now());

        updateApplicationStatus(allocation, com.beezhub.student_accommodation.model.enums.ApplicationStatus.ALLOCATED);
        RoomAllocation saved = roomAllocationRepository.save(allocation);

        allocationAuditService.logStatusChange(saved, previousStatus, AllocationStatus.ACCEPTED);

        waitingListService.updateStatusToAllocated(saved.getStudent().getId());
        eventPublisher.publishEvent(new AllocationAcceptedEvent(saved));

        meterRegistry.counter("sas.allocations.accepted").increment();

        log.info("Successfully accepted room allocation with id: {}", id);
        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public RoomAllocationResponse declineOffer(Long id, DeclineOfferRequest request) {
        log.info("Declining room allocation offer with id: {} for reason: {}", id, request.getDeclineReason());
        RoomAllocation allocation = getAllocation(id);
        allocationAccessGuard.assertCanAccessAllocation(allocation);

        long previousDeclineCount = getPreviousDeclineCount(allocation);

        AllocationStatus previousStatus = allocation.getStatus();
        AllocationStatusValidator.validate(allocation, AllocationStatus.DECLINED);

        allocation.setStatus(AllocationStatus.DECLINED);
        allocation.setEndDate(LocalDateTime.now());
        allocation.setDeclineReason(request.getDeclineReason());
        allocation.setDeclineReasonText(request.getDeclineReasonText());

        RoomAllocation saved = roomAllocationRepository.save(allocation);
        // After the status change, not before it. Occupancy is derived from the live count now,
        // so syncing first would count this allocation as still active (GH-136).
        roomOccupancyManager.syncOccupancy(saved.getRoom());
        allocationAuditService.logStatusChange(saved, previousStatus, AllocationStatus.DECLINED);
        log.info("Successfully declined room allocation with id: {}. Previous decline count: {}", id,
                previousDeclineCount);

        handleDeclineConsequences(saved, previousDeclineCount);

        // Waiting-list refill runs AFTER_COMMIT (WaitingListRefillListener) so it sees the freed
        // room and a refill failure cannot roll back this decline.
        eventPublisher.publishEvent(new RoomVacatedEvent(allocation.getId(), roomIdOf(allocation), AllocationStatus.DECLINED));

        meterRegistry.counter("sas.allocations.declined").increment();

        return roomAllocationMapper.toResponse(saved);
    }

    @Transactional
    public void deleteRoomAllocation(Long id) {
        log.info("Cancelling (deleting) room allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        AllocationStatus previousStatus = allocation.getStatus();
        AllocationStatusValidator.validate(allocation, AllocationStatus.CANCELLED);
        allocation.setStatus(AllocationStatus.CANCELLED);
        allocation.setEndDate(LocalDateTime.now());

        roomOccupancyManager.syncOccupancy(allocation.getRoom());
        RoomAllocation saved = roomAllocationRepository.save(allocation);
        allocationAuditService.logStatusChange(saved, previousStatus, AllocationStatus.CANCELLED);

        // Without this the application stays ALLOCATED, which counts as active, so the student
        // is permanently blocked from re-applying to this tenant while the cancelled allocation
        // no longer holds them a room (GH-138). The allocation is passed, not the save result,
        // because they are the same managed instance.
        waitingListService.restoreAfterCancellation(allocation, previousStatus);
        log.info("Successfully cancelled room allocation with id: {}", id);

        // Refill from the waiting list only after this cancellation has committed.
        eventPublisher.publishEvent(new RoomVacatedEvent(allocation.getId(), roomIdOf(allocation), AllocationStatus.CANCELLED));
    }

    /**
     * Allocation statuses that still hold a room for an application. ACTIVE is deliberately absent:
     * a student who has checked in is living there, and an admin rejecting the application is not
     * grounds for silently ending an occupancy. That case needs a deliberate move-out.
     */
    private static final java.util.Set<AllocationStatus> OUTSTANDING_STATUSES =
            java.util.EnumSet.of(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED);

    /**
     * Cancels any outstanding offer or acceptance for an application that has reached a terminal
     * status, and releases the rooms they held.
     *
     * <p>GH-137 stopped a rejected application being resurrected by an accept or a check-in, but
     * left the state that made the resurrection possible: rejecting an application left its OFFERED
     * or ACCEPTED allocation alive. The room stayed counted as occupied until the hourly expiry
     * sweep ran, and the student kept seeing a live offer on their dashboard that would now fail if
     * they acted on it. Refusing correctly is still a worse experience than not offering (GH-223).
     *
     * <p>Deliberately does not touch the waiting list the way {@code deleteRoomAllocation} does.
     * Cancelling an allocation on its own means the student is still waiting for a room; here the
     * application itself is over, so putting them back on the queue would undo the admin's
     * decision.
     *
     * @return how many allocations were cancelled
     */
    @Transactional
    public int releaseOutstandingAllocations(Long applicationId, String reason) {
        List<RoomAllocation> outstanding =
                roomAllocationRepository.findByApplicationIdAndStatusIn(applicationId, OUTSTANDING_STATUSES);
        if (outstanding.isEmpty()) {
            return 0;
        }

        int cancelled = 0;
        for (RoomAllocation allocation : outstanding) {
            AllocationStatus previousStatus = allocation.getStatus();
            if (previousStatus == null || !previousStatus.canTransitionTo(AllocationStatus.CANCELLED)) {
                log.warn("action=allocationReleaseSkipped allocationId={} from={} applicationId={}",
                        allocation.getId(), previousStatus, applicationId);
                continue;
            }

            allocation.setStatus(AllocationStatus.CANCELLED);
            allocation.setEndDate(LocalDateTime.now());
            RoomAllocation saved = roomAllocationRepository.save(allocation);

            // After the status change, not before: occupancy is derived from the live count, so
            // syncing first would still count this allocation as active (GH-136).
            roomOccupancyManager.syncOccupancy(saved.getRoom());
            allocationAuditService.logStatusChange(saved, previousStatus, AllocationStatus.CANCELLED);

            // AFTER_COMMIT, so the refill sees the freed room and a refill failure cannot roll back
            // the admin's decision on the application.
            eventPublisher.publishEvent(
                    new RoomVacatedEvent(saved.getId(), roomIdOf(saved), AllocationStatus.CANCELLED));

            log.info("action=allocationReleased allocationId={} from={} applicationId={} reason={}",
                    saved.getId(), previousStatus, applicationId, reason);
            cancelled++;
        }
        return cancelled;
    }

    /**
     * Moves an offer's response deadline.
     *
     * <p>GH-139 made the accept path enforce {@code offerExpiryDate}, which was right, but left a
     * lapsed offer with no recovery route for anyone: nothing anywhere writes that column after the
     * offer is created, and the deadline guard blocks a platform admin accepting on the student's
     * behalf too. The only route was to cancel and re-allocate, losing the student's place and the
     * audit connection to the original offer (GH-198).
     *
     * <p>Extending rather than letting an admin accept past the deadline is deliberate. Accepting a
     * room is the student's decision; an extension hands it back to them, where accepting on their
     * behalf would take it away. It also keeps one rule about deadlines — the accept path stays
     * strict — instead of two.
     *
     * <p>Only while the allocation is still OFFERED. Once the sweep has flipped it to EXPIRED the
     * room has been released and possibly re-let, and EXPIRED is terminal, so re-allocation is the
     * honest route from there.
     */
    @Transactional
    public RoomAllocationResponse extendOfferDeadline(Long id, LocalDateTime newExpiryDate, String reason) {
        log.info("Extending offer deadline for allocation {} to {}", id, newExpiryDate);
        RoomAllocation allocation = getAllocation(id);

        if (allocation.getStatus() != AllocationStatus.OFFERED) {
            throw new IllegalStateException(
                    "Only an outstanding offer can have its deadline extended. This allocation is "
                            + allocation.getStatus()
                            + (allocation.getStatus() == AllocationStatus.EXPIRED
                                    ? ", so its room has already been released; allocate the student a room again."
                                    : "."));
        }

        LocalDateTime now = LocalDateTime.now();
        if (!newExpiryDate.isAfter(now)) {
            throw new IllegalArgumentException("The new deadline must be in the future");
        }
        // A fresh offer gets offerExpiryDays, so an extension cannot quietly grant more than
        // starting over would. Without a ceiling a mistyped year leaves an offer open for decades,
        // holding a room out of the pool the whole time.
        LocalDateTime ceiling = now.plusDays(offerExpiryDays);
        if (newExpiryDate.isAfter(ceiling)) {
            throw new IllegalArgumentException(
                    "The deadline cannot be more than " + offerExpiryDays
                            + " days from now, which is what a new offer would give");
        }

        LocalDateTime previousDeadline = allocation.getOfferExpiryDate();
        allocation.setOfferExpiryDate(newExpiryDate);
        // The 24-hour reminder has already been sent against the old deadline, so without this the
        // student would get no warning before the new one.
        allocation.setReminder24hSent(false);

        RoomAllocation saved = roomAllocationRepository.save(allocation);

        allocationAuditService.logAuditAction(
                saved,
                AllocationAuditActionType.OVERRIDE,
                currentAdminOrNull(),
                "Offer deadline moved from "
                        + (previousDeadline == null ? "none" : previousDeadline.format(OFFER_DEADLINE_FORMAT))
                        + " to " + newExpiryDate.format(OFFER_DEADLINE_FORMAT) + ": " + reason,
                null,
                roomOf(saved));

        notifyOfferExtended(saved, newExpiryDate);

        log.info("action=offerDeadlineExtended allocationId={} from={} to={}",
                id, previousDeadline, newExpiryDate);
        return roomAllocationMapper.toResponse(saved);
    }

    /** The acting administrator, or null when the caller cannot be resolved. */
    private AppUser currentAdminOrNull() {
        try {
            return authService.getCurrentUser();
        } catch (RuntimeException e) {
            // An unresolvable caller must not cost us the record that the extension happened.
            log.warn("action=offerExtensionAdminUnresolved");
            return null;
        }
    }

    /** The student was told a deadline; they have to be told it moved, or they act on a stale one. */
    private void notifyOfferExtended(RoomAllocation allocation, LocalDateTime newExpiryDate) {
        try {
            if (allocation.getStudent() == null || allocation.getStudent().getAppUser() == null) {
                return;
            }
            userNotificationService.createOfferExtendedNotification(
                    allocation.getStudent().getAppUser().getId(),
                    allocation.getRoom() != null ? allocation.getRoom().getRoomNumber() : "your room",
                    newExpiryDate.format(OFFER_DEADLINE_FORMAT));
        } catch (Exception e) {
            log.error("Failed to notify student of extended offer deadline for allocation {}",
                    allocation.getId(), e);
        }
    }

    private static Room roomOf(RoomAllocation allocation) {
        return allocation.getRoom();
    }

    @Transactional
    public RoomAllocationResponse allocateRoomByApplication(Long applicationId, Long roomId) {
        log.info("Allocating room {} for application: {}", roomId, applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with id: " + applicationId));
        tenantOwnershipGuard.assertOwned(application, "Application", applicationId);

        if (!"APPROVED".equalsIgnoreCase(String.valueOf(application.getStatus()))) {
            throw new ApplicationNotApprovedException(
                    "Application must be APPROVED before allocating a room. Current status: "
                            + application.getStatus());
        }

        if (roomAllocationRepository.existsByApplicationId(applicationId)) {
            throw new ApplicationAlreadyAllocatedException(
                    "Application with ID " + applicationId + " has already been allocated a room");
        }

        RoomAllocationRequest request = new RoomAllocationRequest();
        request.setStudentId(application.getStudent().getId());
        request.setRoomId(roomId);
        request.setApplicationId(applicationId);

        return createRoomAllocation(request);
    }

    @Transactional
    public RoomAllocationResponse moveStudent(Long allocationId, Long newRoomId, String reason, Long adminId) {
        return allocationMoveService.moveStudent(allocationId, newRoomId, reason, adminId);
    }

    @Transactional
    public RoomAllocationResponse updateRoomAllocation(Long id, RoomAllocationRequest request) {
        log.info("Updating room allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);
        validateAllocationIsEditable(allocation);

        Room oldRoom = allocation.getRoom();
        Student student = getStudent(request.getStudentId());
        Room newRoom = getRoom(request.getRoomId());

        StayPeriod stay = StayPeriod.of(allocation);
        validateStudentChange(allocation, student, stay);

        boolean roomChanged = false;
        if (!oldRoom.getId().equals(newRoom.getId())) {
            stayCapacityGuard.assertBedFree(newRoom, stay);
            roomChanged = true;
        }

        allocation.setStudent(student);
        allocation.setRoom(newRoom);

        if (request.getAllocationDate() != null) {
            allocation.setAllocationDate(request.getAllocationDate());
        }

        if (request.getEndDate() != null) {
            allocation.setEndDate(request.getEndDate());
        }

        RoomAllocation updated = roomAllocationRepository.save(allocation);
        log.info("Successfully updated room allocation with id: {}", id);

        if (roomChanged) {
            // Both rooms, after the save (GH-136).
            roomOccupancyManager.syncOccupancy(oldRoom);
            roomOccupancyManager.syncOccupancy(newRoom);
            notifyRoomChanged(updated, oldRoom);
        }

        return roomAllocationMapper.toResponse(updated);
    }

    @Transactional
    public MessageResponse enqueueWaitingListForAdmin(Long studentId, Long applicationId) {
        return waitingListService.enqueueWaitingListForAdmin(studentId, applicationId);
    }

    @Transactional
    public MessageResponse enqueueWaitingList(Long studentId, Long applicationId) {
        return waitingListService.enqueueWaitingList(studentId, applicationId);
    }

    // ==================== Private Helpers ====================

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));
    }

    private Room getRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));
        tenantOwnershipGuard.assertOwned(room, "Room", id);
        return room;
    }

    /**
     * Statuses an allocation may still be edited in. Every other status has already released the
     * room: a DECLINED, EXPIRED, COMPLETED or CANCELLED allocation had its occupancy decremented
     * when it ended, so re-pointing it at another room decremented the old room a second time and
     * incremented the new one, corrupting both counts (GH-129).
     */
    private static final java.util.Set<AllocationStatus> EDITABLE_STATUSES =
            java.util.EnumSet.of(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED);

    private void validateAllocationIsEditable(RoomAllocation allocation) {
        if (!EDITABLE_STATUSES.contains(allocation.getStatus())) {
            log.warn("action=allocationUpdateRejected allocationId={} status={}",
                    allocation.getId(), allocation.getStatus());
            throw new InvalidAllocationStatusTransitionException(
                    "Only OFFERED or ACCEPTED allocations can be updated; this one is "
                            + allocation.getStatus());
        }
    }

    /**
     * A different student may only be moved onto this allocation when they are not already housed,
     * otherwise the update would give one student two active allocations. An ACTIVE allocation is
     * moved between rooms through {@code AllocationMoveService}, which keeps its own audit trail.
     */
    private void validateStudentChange(RoomAllocation allocation, Student newStudent, StayPeriod stay) {
        Student current = allocation.getStudent();
        if (current != null && current.getId().equals(newStudent.getId())) {
            return;
        }
        stayCapacityGuard.assertStudentFree(newStudent.getId(), stay);
    }

    private static Long roomIdOf(RoomAllocation allocation) {
        return allocation.getRoom() == null ? null : allocation.getRoom().getId();
    }

    private RoomAllocation getAllocation(Long id) {
        return roomAllocationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + id));
    }

    private Application validateAndGetApplication(Long applicationId, Long studentId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found with id: " + applicationId));
        // assertReadable, not assertOwned. The batch job allocates under the tenant it derives from
        // the preferred building, because applications created by a platform admin are stamped with
        // the platform tenant (GH-146). The room is held to assertOwned, so the offer stays in the
        // caller's company either way.
        tenantOwnershipGuard.assertReadable(application, "Application", applicationId);

        if (!application.getStudent().getId().equals(studentId)) {
            throw new IllegalArgumentException("Application does not belong to the specified student");
        }

        ApplicationStatus appStatus = application.getStatus();
        if (appStatus != ApplicationStatus.APPROVED && appStatus != ApplicationStatus.AWAITING_ALLOCATION) {
            throw new ApplicationNotApprovedException(
                    "Application must be APPROVED or AWAITING_ALLOCATION before allocating a room. Current status: "
                            + appStatus);
        }

        if (roomAllocationRepository.existsByApplicationId(applicationId)) {
            throw new ApplicationAlreadyAllocatedException(
                    "Application with ID " + applicationId + " has already been allocated a room");
        }
        return application;
    }

    private RoomAllocation buildNewAllocation(RoomAllocationRequest request, Student student, Room room,
            Application application) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setApplication(application);
        allocation.setTenantId(room.getTenantId());
        // Neither construction site ever stamped this, so academic_year_id was null on every
        // row. The wait estimate counts allocations for the year and always got zero, which is
        // below its minimum sample, so it returned null every time (GH-140).
        //
        // From the application rather than the current year: an allocation with no application
        // has no academic year, and guessing one would inflate the velocity the estimate reads.
        if (application != null) {
            allocation.setAcademicYear(application.getAcademicYear());
        }
        allocation.setAllocationDate(
                request.getAllocationDate() != null ? request.getAllocationDate() : LocalDateTime.now());
        allocation.setEndDate(request.getEndDate());
        AllocationStatusValidator.validate(allocation, AllocationStatus.OFFERED);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setOfferExpiryDate(LocalDateTime.now().plusDays(offerExpiryDays));
        return allocation;
    }

    private void sendAllocationNotification(RoomAllocation allocation) {
        try {
            log.info("Triggering room allocation notification for student {} to room {}",
                    allocation.getStudent().getId(), allocation.getRoom().getRoomNumber());
            eventPublisher.publishEvent(new AllocationCreatedEvent(allocation));
            userNotificationService.createRoomOfferNotification(
                    allocation.getStudent().getAppUser().getId(),
                    allocation.getRoom().getRoomNumber(),
                    allocation.getRoom().getBuilding().getName(),
                    allocation.getOfferExpiryDate());
        } catch (Exception e) {
            log.error(
                    "Failed to trigger notification for room allocation {} - notification will be retried by scheduler",
                    allocation.getId(), e);
        }
    }

    /** An open offer is re-sent for the new room. Once accepted, there is nothing left to accept. */
    private void notifyRoomChanged(RoomAllocation allocation, Room oldRoom) {
        if (allocation.getStatus() == AllocationStatus.OFFERED) {
            sendAllocationNotification(allocation);
        } else {
            eventPublisher.publishEvent(new AllocationMovedEvent(allocation, oldRoom, LocalDateTime.now(), null));
        }
    }

    private void validateOfferAcceptance(RoomAllocation allocation, LocalDateTime expectedCheckIn,
            LocalDateTime checkInFrom) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxDate = checkInFrom.plusDays(checkInExpiryDays);
        if (expectedCheckIn.isBefore(checkInFrom.minusHours(1)) || expectedCheckIn.isAfter(maxDate)) {
            throw new IllegalArgumentException("Expected check-in date must be within " + checkInExpiryDays
                    + " days from " + (checkInFrom.isAfter(now) ? "move-in day" : "now"));
        }
        if (allocation.getStatus() != AllocationStatus.OFFERED) {
            throw new IllegalStateException(
                    "Can only accept OFFERED allocations, current status: " + allocation.getStatus());
        }
        // The hourly sweep (ScheduledJobRunner.runOfferExpiry -> AllocationExpiryService.expireOffers)
        // is the only thing that flips a stale offer to EXPIRED, so a row stayed acceptable for up to
        // an hour past the deadline the student was emailed. Enforce that deadline here (GH-139).
        //
        // isBefore is deliberate: it is the same strict predicate the sweep query uses, so there is no
        // instant at which one admits a row the other would expire. A null deadline is unreachable in
        // practice - both OFFERED creation paths stamp one - and a null row is equally invisible to
        // the sweep, so treating it as "no deadline" keeps the two consistent.
        LocalDateTime deadline = allocation.getOfferExpiryDate();
        if (deadline != null && deadline.isBefore(now)) {
            log.warn("action=offerAcceptRejected reason=offerExpired allocationId={} offerExpiryDate={}",
                    allocation.getId(), deadline);
            throw new IllegalStateException("This offer expired on "
                    + deadline.format(OFFER_DEADLINE_FORMAT) + " and can no longer be accepted");
        }
        AllocationStatusValidator.validate(allocation, AllocationStatus.ACCEPTED);
        // Rejecting an application does not cancel its outstanding offer, so a student could
        // still accept it: the allocation guard passed, and the application status was silently
        // overwritten from REJECTED to ALLOCATED, un-rejecting it and keeping the room (GH-137).
        ApplicationStatusValidator.validate(allocation.getApplication(),
                com.beezhub.student_accommodation.model.enums.ApplicationStatus.ALLOCATED);
    }

    private void updateApplicationStatus(RoomAllocation allocation,
            com.beezhub.student_accommodation.model.enums.ApplicationStatus status) {
        if (allocation.getApplication() != null) {
            allocation.getApplication().setStatus(status);
            applicationRepository.save(allocation.getApplication());
        }
    }

    private long getPreviousDeclineCount(RoomAllocation allocation) {
        if (allocation.getApplication() != null) {
            return roomAllocationRepository
                    .countDeclinedAllocationsByApplicationId(allocation.getApplication().getId());
        }
        return 0;
    }

    private void handleDeclineConsequences(RoomAllocation allocation, long previousDeclineCount) {
        waitingListService.handleDeclineOutcome(allocation, (int) previousDeclineCount + 1);
    }
}
