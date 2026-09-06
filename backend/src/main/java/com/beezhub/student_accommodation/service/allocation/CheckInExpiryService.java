package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.ScheduledJobException;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.AllocationExpiredEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.allocation.RoomOccupancyManager;
import com.beezhub.student_accommodation.service.allocation.AllocationStatusValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInExpiryService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final WaitingListRepository waitingListRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserNotificationService userNotificationService;
    private final RoomOccupancyManager roomOccupancyManager;
    private final RoomRepository roomRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.allocation.check-in-expiry-days:7}")
    private int checkInExpiryDays;

    public void expireSingleCheckIn(RoomAllocation allocation) {
        transactionTemplate.execute(status -> {
            try {
                // Re-fetch to ensure latest state and avoid race conditions
                RoomAllocation current = roomAllocationRepository.findByIdWithDetails(allocation.getId())
                        .orElse(null);

                if (current == null) {
                    log.warn("Allocation {} no longer exists, skipping check-in expiry", allocation.getId());
                    return null;
                }

                // Verify transition to CHECK_IN_EXPIRED is valid (might have been checked in in
                // the meantime)
                if (!current.getStatus().canTransitionTo(AllocationStatus.CHECK_IN_EXPIRED)) {
                    log.info("Allocation {} status {} cannot transition to CHECK_IN_EXPIRED, skipping check-in expiry",
                            current.getId(), current.getStatus());
                    return null;
                }

                log.info("Expiring check-in for allocation {}, student {}, room {}",
                        current.getId(), current.getStudent().getId(), current.getRoom().getRoomNumber());

                // Update allocation status
                AllocationStatusValidator.validate(current, AllocationStatus.CHECK_IN_EXPIRED);
                current.setStatus(AllocationStatus.CHECK_IN_EXPIRED);
                current.setEndDate(LocalDateTime.now());
                roomAllocationRepository.save(current);

                // Free the room
                roomOccupancyManager.syncOccupancy(current.getRoom());

                // Cancel application
                cancelApplicationAfterCheckInExpiry(current);

                // Send notifications
                try {
                    eventPublisher.publishEvent(new AllocationExpiredEvent(current, true));
                    userNotificationService.createCheckInExpiredNotification(
                            current.getStudent().getAppUser().getId(),
                            current.getRoom().getRoomNumber(),
                            current.getRoom().getBuilding() != null ? current.getRoom().getBuilding().getName()
                                    : "Unknown Building");
                } catch (Exception e) {
                    log.error("Failed to trigger check-in expiry notifications for allocation {}", current.getId(), e);
                }

                log.info("Successfully expired check-in for allocation {}", current.getId());
                return null;
            } catch (OptimisticLockingFailureException e) {
                // Someone changed this allocation while the sweep was working on it — the student
                // accepting, most likely. Rethrown as itself rather than wrapped, so the caller can
                // tell a lost race from a real failure and skip it quietly (GH-196).
                status.setRollbackOnly();
                throw e;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new ScheduledJobException("Failed to expire check-in for allocation " + allocation.getId(), e);
            }
        });
    }

    private void cancelApplicationAfterCheckInExpiry(RoomAllocation allocation) {
        Application application = allocation.getApplication();
        Student student = allocation.getStudent();

        if (application == null) {
            log.warn("No application linked to allocation {}; cannot cancel", allocation.getId());
            return;
        }

        log.info("Cancelling application {} after check-in expiry for student {}",
                application.getId(), student.getId());

        // Update application status to CANCELLED
        if (!ApplicationStatusValidator.canApply(application, ApplicationStatus.CANCELLED)) {
            log.info("action=applicationCancelSkipped applicationId={} from={}",
                    application.getId(), application.getStatus());
            return;
        }
        application.setStatus(ApplicationStatus.CANCELLED);
        String remarks = "Application cancelled: Student failed to check in within " + checkInExpiryDays +
                " days of accepting the offer.";
        application.setRemarks(remarks);
        applicationRepository.save(application);

        // Cancel waiting list entry if exists
        waitingListRepository.findByStudent_IdAndStatus(student.getId(), "WAITING")
                .ifPresent(entry -> {
                    entry.setStatus("CANCELLED");
                    waitingListRepository.save(entry);
                    log.info("Cancelled waiting list entry for student {}", student.getId());
                });
    }
}
