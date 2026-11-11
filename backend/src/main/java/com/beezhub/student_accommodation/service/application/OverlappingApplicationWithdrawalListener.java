package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.event.AllocationAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Withdraws the student's overlapping applications once an accept has committed (GH-497).
 *
 * <p>After commit, so a rolled-back accept withdraws nothing. The service opens its own
 * transactions, which database work in an AFTER_COMMIT listener needs, as
 * {@code WaitingListRefillListener} does. Exceptions are swallowed because one thrown here would
 * reach the student as a failed accept although their place is already committed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OverlappingApplicationWithdrawalListener {

    private final OverlappingApplicationWithdrawalService withdrawalService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAllocationAccepted(AllocationAcceptedEvent event) {
        Long allocationId = event.allocation() != null ? event.allocation().getId() : null;
        if (allocationId == null) {
            return;
        }
        try {
            withdrawalService.withdrawOverlapping(allocationId);
        } catch (Exception e) {
            log.error("action=overlappingWithdrawalFailed acceptedAllocationId={}", allocationId, e);
        }
    }
}
