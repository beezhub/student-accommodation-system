package com.beezhub.student_accommodation.service.document;

import java.time.Instant;

/**
 * A stored object as reported by the storage backend.
 *
 * @param key          The object key (path) inside the bucket
 * @param lastModified When the object was last written, or {@code null} if the backend did not report it
 */
public record StoredObject(String key, Instant lastModified) {
}
