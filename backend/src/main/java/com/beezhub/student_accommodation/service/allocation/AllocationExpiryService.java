package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.NotificationService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationExpiryService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final WaitingListRepository waitingListRepository;
    private final NotificationService notificationService;
    private final UserNotificationService userNotificationService;
    private final AllocationBatchService allocationBatchService;
    private final Environment environment;
    private final TransactionTemplate transactionTemplate;
    private final RoomRepository roomRepository;

    @Value("${app.allocation.offer-expiry-days:7}")
    private int offerExpiryDays;

    @Value("${app.allocation.check-in-expiry-days:7}")
    private int checkInExpiryDays;

    /**
     * Scheduled job to expire offers that haven't been responded to within the
     * expiry period.
     * Runs every hour (configurable via app.allocation.expiry-check-interval-ms).
     */
    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void expireOffers() {
        log.info("Starting scheduled job to expire unresponsive offers");

        try {
            List<RoomAllocation> expiredOffers = roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(
                    AllocationStatus.OFFERED, LocalDateTime.now());

            if (expiredOffers.isEmpty()) {
                log.info("No expired offers found");
                return;
            }

            log.info("Found {} expired offers to process", expiredOffers.size());
            int successCount = 0;
            int failureCount = 0;

            for (RoomAllocation allocation : expiredOffers) {
                try {
                    offerExpiryService.expireSingleOffer(allocation);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to expire offer for allocation {}: {}", allocation.getId(), e.getMessage(), e);
                }
            }

            log.info("Offer expiry job completed: {} succeeded, {} failed", successCount, failureCount);

            // Trigger waiting list allocation for freed rooms
            if (successCount > 0) {
                try {
                    // Use batch allocation if configured, otherwise fall back to single allocation
                    boolean useBatchOnExpiry = environment.getProperty("app.allocation.batch-on-expiry", Boolean.class,
                            true);
                    if (useBatchOnExpiry) {
                        log.info("Triggering batch allocation after expiring {} offers", successCount);
                        allocationBatchService.batchAllocateFromWaitingList(successCount);
                    } else {
                        allocationBatchService.allocateNextFromWaitingList();
                    }
                } catch (Exception e) {
                    log.error("Failed to allocate from waiting list after expiring offers", e);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in expireOffers job", e);
        }
    }

    /**
     * Scheduled job to expire check-ins that haven't been completed within the
     * expiry period.
     * Runs every hour (configurable via app.allocation.expiry-check-interval-ms).
     */
    @Scheduled(fixedDelayString = "${app.allocation.expiry-check-interval-ms:3600000}")
    public void expireCheckIns() {
        log.info("Starting scheduled job to expire overdue check-ins");

        try {
            List<RoomAllocation> expiredCheckIns = roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(
                    AllocationStatus.ACCEPTED, LocalDateTime.now());

            if (expiredCheckIns.isEmpty()) {
                log.info("No expired check-ins found");
                return;
            }

            log.info("Found {} expired check-ins to process", expiredCheckIns.size());
            int successCount = 0;
            int failureCount = 0;

            for (RoomAllocation allocation : expiredCheckIns) {
                try {
                    checkInExpiryService.expireSingleCheckIn(allocation);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to expire check-in for allocation {}: {}", allocation.getId(), e.getMessage(), e);
                }
            }

            log.info("Check-in expiry job completed: {} succeeded, {} failed", successCount, failureCount);

            // Trigger waiting list allocation for freed rooms
            if (successCount > 0) {
                try {
                    // Use batch allocation if configured, otherwise fall back to single allocation
                    boolean useBatchOnExpiry = environment.getProperty("app.allocation.batch-on-expiry", Boolean.class,
                            true);
                    if (useBatchOnExpiry) {
                        log.info("Triggering batch allocation after expiring {} check-ins", successCount);
                        allocationBatchService.batchAllocateFromWaitingList(successCount);
                    } else {
                        allocationBatchService.allocateNextFromWaitingList();
                    }
                } catch (Exception e) {
                    log.error("Failed to allocate from waiting list after expiring check-ins", e);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in expireCheckIns job", e);
        }
    }

    private void expireSingleOffer(RoomAllocation allocation) {
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
                RoomOccupancyManager.updateOccupancy(current.getRoom(), -1, roomRepository);

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

    private void expireSingleCheckIn(RoomAllocation allocation) {
        transactionTemplate.execute(status -> {
            try {
                // Re-fetch to ensure latest state and avoid race conditions
                RoomAllocation current = roomAllocationRepository.findByIdWithDetails(allocation.getId())
                        .orElse(null);

                if (current == null) {
                    log.warn("Allocation {} no longer exists, skipping check-in expiry", allocation.getId());
                    return null;
                }

                // Verify transition to CHECK_IN_EXPIRED is valid (might have been checked in in the meantime)
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
                RoomOccupancyManager.updateOccupancy(current.getRoom(), -1, roomRepository);

                // Cancel application
                cancelApplicationAfterCheckInExpiry(current);

                // Send notifications
                try {
                    notificationService.sendCheckInExpiredNotification(current);
                    userNotificationService.createCheckInExpiredNotification(
                            current.getStudent().getAppUser().getId(),
                            current.getRoom().getRoomNumber(),
                            current.getRoom().getBuilding() != null ?
                                    current.getRoom().getBuilding().getName() : "Unknown Building");
                } catch (Exception e) {
                    log.error("Failed to send check-in expiry notifications for allocation {}", current.getId(), e);
                }

                log.info("Successfully expired check-in for allocation {}", current.getId());
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException("Failed to expire check-in for allocation " + allocation.getId(), e);
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
