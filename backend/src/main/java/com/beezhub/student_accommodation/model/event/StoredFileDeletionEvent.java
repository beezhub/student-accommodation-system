package com.beezhub.student_accommodation.model.event;

/**
 * Published when a stored object should be removed, handled after commit.
 *
 * <p>GH-145: the delete used to run inline inside the transaction, so the object was destroyed
 * before the row was durable. Any later rollback restored a row pointing at an object that no
 * longer existed, and downloads failed forever. The orphan cleanup job deletes objects without
 * rows; nothing restores a row without an object.
 *
 * @param objectPath the storage key to delete
 */
public record StoredFileDeletionEvent(String objectPath) {
}
