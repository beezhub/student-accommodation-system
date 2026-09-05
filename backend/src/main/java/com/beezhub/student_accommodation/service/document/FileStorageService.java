package com.beezhub.student_accommodation.service.document;

import java.io.InputStream;
import java.time.Duration;
import java.util.stream.Stream;

public interface FileStorageService {
    /**
     * Uploads a file to the storage.
     *
     * @param key         The object key (path)
     * @param content     The input stream of the file content
     * @param size        The size of the file
     * @param contentType The content type of the file
     * @return The URL or key of the uploaded file
     */
    String upload(String key, InputStream content, long size, String contentType);

    /**
     * Downloads a file from the storage.
     *
     * @param key The object key
     * @return The input stream of the file content
     */
    InputStream download(String key);

    /**
     * Deletes a file from the storage.
     *
     * @param key The object key
     */
    void delete(String key);

    /**
     * Checks if a file exists in the storage.
     *
     * @param key The object key
     * @return True if the file exists, false otherwise
     */
    boolean exists(String key);

    /**
     * Generates a presigned URL for accessing the file.
     *
     * @param key    The object key
     * @param expiry The duration for which the URL is valid
     * @return The presigned URL
     */
    String generatePresignedUrl(String key, Duration expiry);

    /**
     * Returns the content type of a stored object.
     *
     * @param key The object key
     * @return The MIME content type, or "application/octet-stream" if unknown
     */
    String getContentType(String key);

    /**
     * Lists objects whose key starts with the given prefix.
     *
     * @param prefix The key prefix to list under; an empty string lists the whole bucket
     * @return A stream of stored objects (keys and last-modified timestamps)
     */
    Stream<StoredObject> listObjects(String prefix);
}
