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
import com.beezhub.student_accommodation.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Transactional
    public RoomAllocationResponse confirmCheckIn(Long id) {
        log.info("Confirming check-in for allocation with id: {}", id);
        RoomAllocation allocation = getAllocation(id);

        AllocationStatusValidator.validate(allocation, AllocationStatus.ACTIVE);
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
        log.info("Successfully checked in allocation with id: {}", id);
        
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
