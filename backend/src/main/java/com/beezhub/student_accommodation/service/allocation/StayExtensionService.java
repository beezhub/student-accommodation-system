package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.exceptions.StayExtensionRefusedException;
import com.beezhub.student_accommodation.mapper.RoomAllocationMapper;
import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.StayExtensionResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.model.event.StayExtendedEvent;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.DocumentIssue;
import com.beezhub.student_accommodation.service.document.DocumentReadinessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * "Stay for Semester 2" (GH-498): a student with an accepted place or a room moves their planned
 * check-out to the end of the next intake of the same type in the same year. Same allocation, room
 * and application; no admin step. The stay runs straight through any gap between the intakes, and
 * the bed counts as taken during it.
 *
 * <p>Allowed when the next intake exists and has not ended, the bed is free for the extra dates
 * (the overlap count from group C), the student holds no other bed for them, and every required
 * document is approved with none of the expiring ones expired. An Annual stay cannot be extended:
 * staying into next year is a new application.
 *
 * <p>The request is checked again with the room's row locked, so two students asking for the last
 * bed at once are served one after the other and only one gets it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StayExtensionService {

    private static final Set<AllocationStatus> EXTENDABLE = EnumSet.of(AllocationStatus.ACCEPTED, AllocationStatus.ACTIVE);

    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomRepository roomRepository;
    private final IntakeRepository intakeRepository;
    private final AllocationAccessGuard allocationAccessGuard;
    private final StayCapacityGuard stayCapacityGuard;
    private final DocumentReadinessGuard documentReadinessGuard;
    private final RoomAllocationMapper roomAllocationMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** What the outcome would be, and the next intake when there is one. */
    record Decision(boolean allowed, String reason, Intake next) {

        static Decision refused(String reason, Intake next) {
            return new Decision(false, reason, next);
        }
    }

    /** Whether the student can stay on for the next intake. Checked by {@link AllocationAccessGuard}. */
    @Transactional(readOnly = true)
    public StayExtensionResponse getOption(Long allocationId) {
        RoomAllocation allocation = load(allocationId);
        allocationAccessGuard.assertCanAccessAllocation(allocation);
        return toResponse(allocation, evaluate(allocation));
    }

    /**
     * Extends the stay to the end of the next intake, checking everything again with the room
     * locked. Checked by {@link AllocationAccessGuard}: students are platform-tenant users, so the
     * tenant filter does nothing for them.
     */
    @Transactional
    @LogActivity(action = "EXTEND_STAY", entityType = "ROOM_ALLOCATION", description = "Extended the stay of allocation {0}")
    public RoomAllocationResponse extend(Long allocationId) {
        RoomAllocation allocation = load(allocationId);
        allocationAccessGuard.assertCanAccessAllocation(allocation);
        if (allocation.getRoom() != null) {
            roomRepository.findByIdForUpdate(allocation.getRoom().getId());
        }
        Decision decision = evaluate(allocation);
        if (!decision.allowed()) {
            log.info("action=stayExtensionRefused allocationId={}", allocationId);
            throw new StayExtensionRefusedException(decision.reason());
        }
        LocalDate previousEnd = allocation.getStayEndDate();
        allocation.setStayEndDate(decision.next().getStayEndDate());
        RoomAllocation saved = roomAllocationRepository.save(allocation);
        eventPublisher.publishEvent(new StayExtendedEvent(saved.getId(), decision.next().getName(), previousEnd,
                saved.getStayEndDate()));
        log.info("action=stayExtended allocationId={} intakeId={} from={} to={}",
                saved.getId(), decision.next().getId(), previousEnd, saved.getStayEndDate());
        return roomAllocationMapper.toResponse(saved);
    }

    /** Every rule, in the order a student would want to hear about them. */
    Decision evaluate(RoomAllocation allocation) {
        Application application = allocation.getApplication();
        Intake current = application != null ? application.getIntake() : null;
        if (current == null || allocation.getStayEndDate() == null) {
            return Decision.refused("This place has no intake to extend.", null);
        }
        if (current.getIntakeType() == IntakeType.ANNUAL) {
            return Decision.refused("An Annual stay cannot be extended. Apply for next year instead.", null);
        }
        Intake next = nextIntake(current, allocation.getStayEndDate());
        if (next == null) {
            return Decision.refused("There is no later " + current.getIntakeType().name().toLowerCase()
                    + " this year to stay on for.", null);
        }
        if (!EXTENDABLE.contains(allocation.getStatus())) {
            return Decision.refused("Only an accepted place or the room you live in can be extended.", next);
        }
        StayPeriod extra = new StayPeriod(allocation.getStayEndDate(), next.getStayEndDate());
        if (extra.hasEnded(stayCapacityGuard.today())) {
            return Decision.refused(next.getName() + " has already ended.", next);
        }
        Room room = allocation.getRoom();
        if (room == null || (room.getCapacity() != null && stayCapacityGuard.bedsTaken(room, extra) >= room.getCapacity())) {
            return Decision.refused("This room is not free for " + next.getName() + ". You can apply for it instead.", next);
        }
        if (roomAllocationRepository.existsLiveAllocationForStudentDuring(
                allocation.getStudent().getId(), extra.from(), extra.until())) {
            return Decision.refused("You already have a place for " + next.getName() + ".", next);
        }
        List<DocumentIssue> issues = documentReadinessGuard.issuesForStay(allocation);
        if (!issues.isEmpty()) {
            return Decision.refused("Your documents need to be approved and up to date first: "
                    + DocumentReadinessGuard.describe(issues) + ".", next);
        }
        return new Decision(true, null, next);
    }

    /** The first intake of the same type in the same year that starts once the stay ends. */
    private Intake nextIntake(Intake current, LocalDate stayEnd) {
        if (current.getAcademicYear() == null) {
            return null;
        }
        return intakeRepository
                .findByAcademicYear_IdAndIntakeTypeAndStayStartDateGreaterThanEqualOrderByStayStartDateAsc(
                        current.getAcademicYear().getId(), current.getIntakeType(), stayEnd)
                .stream()
                .filter(intake -> !intake.getId().equals(current.getId()))
                .findFirst()
                .orElse(null);
    }

    private RoomAllocation load(Long allocationId) {
        return roomAllocationRepository.findByIdWithDetails(allocationId)
                .orElseThrow(() -> new RoomAllocationNotFoundException("Room allocation not found with id: " + allocationId));
    }

    private static StayExtensionResponse toResponse(RoomAllocation allocation, Decision decision) {
        Intake next = decision.next();
        return StayExtensionResponse.builder()
                .allocationId(allocation.getId())
                .allowed(decision.allowed())
                .reason(decision.reason())
                .currentStayEndDate(allocation.getStayEndDate())
                .nextIntakeId(next != null ? next.getId() : null)
                .nextIntakeName(next != null ? next.getName() : null)
                .nextStayStartDate(next != null ? next.getStayStartDate() : null)
                .nextStayEndDate(next != null ? next.getStayEndDate() : null)
                .build();
    }
}
