package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferExpiryService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final WaitingListRepository waitingListRepository;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final RoomOccupancyManager roomOccupancyManager;
    private final RoomRepository roomRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.allocation.offer-expiry-days:7}")
    private int offerExpiryDays;

    public void expireSingleOffer(RoomAllocation allocation) {
        transactionTemplate.execute(status -> {
            try {
                // Re-fetch to ensure latest state and avoid race conditions
                RoomAllocation current = roomAllocationRepository.findByIdWithDetails(allocation.getId())
                        .orElse(null);

                if (current == null) {
                    log.warn("Allocation {} no longer exists, skipping expiry", allocation.getId());
                    return null;
                }

                // Verify transition to EXPIRED is valid (might have been accepted in the meantime)
                AllocationStatus currentStatus = current.getStatus();
                if (currentStatus == null || !currentStatus.canTransitionTo(AllocationStatus.EXPIRED)) {
                    log.info("Allocation {} status {} cannot transition to EXPIRED, skipping expiry",
                            current.getId(), currentStatus);
                    return null;
                }

                log.info("Expiring offer for allocation {}, student {}, room {}",
                        current.getId(), current.getStudent().getId(), current.getRoom().getRoomNumber());

                // Update allocation status
                AllocationStatusValidator.validate(current, AllocationStatus.EXPIRED);
                current.setStatus(AllocationStatus.EXPIRED);
                current.setEndDate(LocalDateTime.now());
                roomAllocationRepository.save(current);

                // Free the room
                roomOccupancyManager.updateOccupancy(current.getRoom(), -1);

                // Cancel application
                cancelApplicationAfterOfferExpiry(current);

                // Send notifications
                try {
                    notificationService.sendOfferExpiredNotification(current);
                    userNotificationService.createOfferExpiredNotification(
                            current.getStudent().getAppUser().getId(),
                            current.getRoom().getRoomNumber(),
                            current.getRoom().getBuilding() != null ?
                                    current.getRoom().getBuilding().getName() : "Unknown Building");
                } catch (Exception e) {
                    log.error("Failed to send expiry notifications for allocation {}", current.getId(), e);
                }

                log.info("Successfully expired offer for allocation {}", current.getId());
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("Failed to expire offer for allocation " + allocation.getId(), e);
            }
        });
    }

    private void cancelApplicationAfterOfferExpiry(RoomAllocation allocation) {
        Application application = allocation.getApplication();
        Student student = allocation.getStudent();

        if (application == null) {
            log.warn("No application linked to allocation {}; cannot cancel", allocation.getId());
            return;
        }

        log.info("Cancelling application {} after offer expiry for student {}",
                application.getId(), student.getId());

        // Update application status to CANCELLED
        application.setStatus(ApplicationStatus.CANCELLED);
        String remarks = "Application cancelled: Room offer expired without response within " + offerExpiryDays + " days.";
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
