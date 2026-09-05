package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.event.RoomVacatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WaitingListRefillListenerTest {

    @Mock
    private AllocationBatchService allocationBatchService;

    @InjectMocks
    private WaitingListRefillListener listener;

    private static final RoomVacatedEvent EVENT = new RoomVacatedEvent(1L, 7L, AllocationStatus.DECLINED);

    @Test
    void onRoomVacated_triggersSingleRefill() {
        listener.onRoomVacated(EVENT);

        verify(allocationBatchService).allocateNextFromWaitingList();
    }

    @Test
    void onRoomVacated_refillFails_exceptionSwallowedSoCommittedCallerIsNotFailed() {
        doThrow(new IllegalStateException("no rooms")).when(allocationBatchService).allocateNextFromWaitingList();

        assertThatCode(() -> listener.onRoomVacated(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void onRoomVacated_runsAfterCommitAndOutsideAnyTransaction() throws NoSuchMethodException {
        Method method = WaitingListRefillListener.class.getMethod("onRoomVacated", RoomVacatedEvent.class);
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(method.isAnnotationPresent(Transactional.class)).isFalse();
    }
}
