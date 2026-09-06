package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.model.event.StoredFileDeletionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * GH-145: the stored object used to be deleted inline inside the transaction, so it was destroyed
 * before the row was durable. Any later rollback restored a row pointing at an object that no
 * longer existed, and downloads failed forever after. The orphan cleanup job removes objects with
 * no row; nothing restores a row with no object.
 */
@ExtendWith(MockitoExtension.class)
class StoredFileDeletionListenerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private StoredFileDeletionListener listener;

    @Test
    void onStoredFileDeletion_removesTheObject() {
        listener.onStoredFileDeletion(new StoredFileDeletionEvent("student/1/id.pdf"));

        verify(fileStorageService).delete("student/1/id.pdf");
    }

    /**
     * There is no transaction left to roll back by the time this runs, and the orphan cleanup job
     * is the backstop, so a storage failure must not escape onto the caller's thread.
     */
    @Test
    void onStoredFileDeletion_storageFailure_isSwallowed() {
        doThrow(new FileStorageException("storage error")).when(fileStorageService).delete(anyString());

        assertDoesNotThrow(() -> listener.onStoredFileDeletion(
                new StoredFileDeletionEvent("student/1/id.pdf")));
    }

    /**
     * The phase is the entire point. Running before commit would put back exactly the defect this
     * class exists to remove.
     */
    @Test
    void listener_runsAfterCommit() throws Exception {
        Method handler = StoredFileDeletionListener.class
                .getMethod("onStoredFileDeletion", StoredFileDeletionEvent.class);

        TransactionalEventListener annotation = handler.getAnnotation(TransactionalEventListener.class);
        assertNotNull(annotation);
        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
    }
}
