package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final Environment environment;
    private final OfferExpiryService offerExpiryService;
    private final CheckInExpiryService checkInExpiryService;
    private final AllocationBatchService allocationBatchService;

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
}
