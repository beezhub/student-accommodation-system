package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.event.AllocationAcceptedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GH-497: the withdrawal runs only after the accept commits, and never fails the accept. */
class OverlappingApplicationWithdrawalListenerTest {

    private final OverlappingApplicationWithdrawalService service = mock(OverlappingApplicationWithdrawalService.class);
    private final OverlappingApplicationWithdrawalListener listener = new OverlappingApplicationWithdrawalListener(service);

    private static AllocationAcceptedEvent accepted(Long id) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(id);
        return new AllocationAcceptedEvent(allocation);
    }

    @Test
    void onAllocationAccepted_runsAfterCommitOnly() throws NoSuchMethodException {
        TransactionalEventListener annotation = OverlappingApplicationWithdrawalListener.class
                .getMethod("onAllocationAccepted", AllocationAcceptedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }

    @Test
    void onAllocationAccepted_withdrawsForTheAcceptedAllocation() {
        listener.onAllocationAccepted(accepted(50L));

        verify(service).withdrawOverlapping(50L);
    }

    @Test
    void onAllocationAccepted_failure_isSwallowed() {
        when(service.withdrawOverlapping(50L)).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> listener.onAllocationAccepted(accepted(50L))).doesNotThrowAnyException();
    }

    @Test
    void onAllocationAccepted_noAllocationId_doesNothing() {
        listener.onAllocationAccepted(accepted(null));
        listener.onAllocationAccepted(new AllocationAcceptedEvent(null));

        verify(service, never()).withdrawOverlapping(anyLong());
    }
}
