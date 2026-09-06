package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.model.event.StoredFileDeletionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Removes a stored object only once the transaction that orphaned it has committed.
 *
 * <p>GH-145: deleting inline meant the object was gone before the row was durable, so a rollback
 * left a committed row pointing at nothing. Ordering it after commit makes the failure mode the
 * harmless one instead: a rollback leaves the object in place, and OrphanFileCleanupJob already
 * removes objects that no row references.
 *
 * <p>A failure here is logged, not rethrown. There is no transaction left to roll back, and the
 * cleanup job is the backstop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoredFileDeletionListener {

    private final FileStorageService fileStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStoredFileDeletion(StoredFileDeletionEvent event) {
        try {
            fileStorageService.delete(event.objectPath());
            log.info("action=storedFileDeleted objectPath={}", event.objectPath());
        } catch (RuntimeException e) {
            log.error("action=storedFileDeleteFailed objectPath={} error={} — orphan cleanup will collect it",
                    event.objectPath(), e.getMessage(), e);
        }
    }
}
