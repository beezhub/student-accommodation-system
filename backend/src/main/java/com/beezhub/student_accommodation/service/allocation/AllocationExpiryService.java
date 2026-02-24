package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationExpiryService {

    private final RoomAllocationRepository roomAllocationRepository;
    private final OfferExpiryService offerExpiryService;
    private final CheckInExpiryService checkInExpiryService;
    private final AllocationBatchService allocationBatchService;
    private final Environment environment;

    public int expireOffers() {
        List<RoomAllocation> expiredOffers = roomAllocationRepository.findByStatusAndOfferExpiryDateBefore(
                AllocationStatus.OFFERED, LocalDateTime.now());

        if (expiredOffers.isEmpty()) {
            log.info("No expired offers found");
            return 0;
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

        log.info("Offer expiry completed: {} succeeded, {} failed", successCount, failureCount);

        if (successCount > 0) {
            triggerBatchAllocation(successCount, "offers");
        }

        return successCount;
    }

    public int expireCheckIns() {
        List<RoomAllocation> expiredCheckIns = roomAllocationRepository.findByStatusAndCheckInExpiryDateBefore(
                AllocationStatus.ACCEPTED, LocalDateTime.now());

        if (expiredCheckIns.isEmpty()) {
            log.info("No expired check-ins found");
            return 0;
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

        log.info("Check-in expiry completed: {} succeeded, {} failed", successCount, failureCount);

        if (successCount > 0) {
            triggerBatchAllocation(successCount, "check-ins");
        }

        return successCount;
    }

    private void triggerBatchAllocation(int count, String type) {
        try {
            boolean useBatchOnExpiry = environment.getProperty("app.allocation.batch-on-expiry", Boolean.class, true);
            if (useBatchOnExpiry) {
                log.info("Triggering batch allocation after expiring {} {}", count, type);
                allocationBatchService.batchAllocateFromWaitingList(count);
            } else {
                allocationBatchService.allocateNextFromWaitingList();
            }
        } catch (Exception e) {
            log.error("Failed to allocate from waiting list after expiring {}", type, e);
        }
    }
}
