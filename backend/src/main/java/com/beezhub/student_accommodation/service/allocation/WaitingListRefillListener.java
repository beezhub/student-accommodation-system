package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Refills a vacated room from the waiting list once the decline or cancellation that freed it has
 * committed.
 *
 * <p>Running after commit means the refill sees the released capacity, and a refill failure can
 * never roll back the student's decline or cancel. The batch service opens its own transactions
 * (see {@code TransactionTemplateConfig#REQUIRES_NEW}), which is required for database work in an
 * AFTER_COMMIT listener. Exceptions are swallowed here because an exception thrown from an
 * after-commit callback would surface to the caller even though its data is already committed.
 *
 * <p>Triggered by decline, cancel, check-out and end. Three paths still free a room without
 * publishing and so do not refill: {@code OfferExpiryService}, {@code CheckInExpiryService} and
 * {@code AllocationMoveService}. The first two are followed by the batch job on its own
 * schedule; the move is a swap rather than a vacancy. Recorded here so the gap is known rather
 * than assumed closed.
 *
 * <p>The refill is a no-op while the {@code auto-allocation} feature flag is off, which is how
 * it ships.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WaitingListRefillListener {

    private final AllocationBatchService allocationBatchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomVacated(RoomVacatedEvent event) {
        log.info("action=waitingListRefill trigger=roomVacated allocationId={} roomId={} status={}",
                event.allocationId(), event.roomId(), event.newStatus());
        try {
            allocationBatchService.allocateNextFromWaitingList();
        } catch (Exception e) {
            log.error("action=waitingListRefillFailed allocationId={} roomId={}",
                    event.allocationId(), event.roomId(), e);
        }
    }
}
