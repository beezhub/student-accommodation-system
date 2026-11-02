package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.config.StorageConfig;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.stream.Stream;

/** {@link FileStorageService} over the S3 API: Amazon S3 on PROD, MinIO elsewhere (GH-331). */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService implements FileStorageService {

    private static final int NOT_FOUND = 404;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageConfig storageConfig;

    @Override
    public String upload(String key, InputStream content, long size, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(storageConfig.getBucketName())
                            .key(key)
                            .contentType(contentType)
                            .contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(content, size));
            return key;
        } catch (Exception e) {
            throw new FileStorageException("Error uploading file to storage: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(storageConfig.getBucketName())
                            .key(key)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error downloading file from storage: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(storageConfig.getBucketName())
                            .key(key)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error deleting file from storage: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3Client.headObject(headObject(key));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            // HEAD responses carry no body, so some servers answer a missing key with a bare 404
            // that does not map to NoSuchKeyException.
            if (e.statusCode() == NOT_FOUND) {
                return false;
            }
            throw new FileStorageException("Error checking file existence in storage: " + key, e);
        } catch (Exception e) {
            throw new FileStorageException("Error checking file existence in storage: " + key, e);
        }
    }

    @Override
    public String getContentType(String key) {
        try {
            String contentType = s3Client.headObject(headObject(key)).contentType();
            return contentType == null ? "application/octet-stream" : contentType;
        } catch (Exception e) {
            log.warn("Could not determine content type for key {}: {}", key, e.getMessage());
            return "application/octet-stream";
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiry) {
        try {
            // The stored type is clamped here too, not only on the download endpoint. A presigned
            // URL is served straight by object storage, so without these overrides an object
            // stored before GH-144 would still be delivered as whatever type the client claimed.
            // attachment stops the browser rendering it inline whatever the type says.
            GetObjectRequest getObject = GetObjectRequest.builder()
                    .bucket(storageConfig.getBucketName())
                    .key(key)
                    .responseContentType(DocumentContentTypes.clamp(getContentType(key)))
                    .responseContentDisposition("attachment")
                    .build();

            return s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(expiry)
                            .getObjectRequest(getObject)
                            .build())
                    .url()
                    .toString();
        } catch (Exception e) {
            throw new FileStorageException("Error generating presigned URL for key: " + key, e);
        }
    }

    /**
     * Pages through the listing lazily. A failure part-way through surfaces as the SDK's own
     * runtime exception when the stream is consumed; OrphanFileCleanupJob, the caller, catches it.
     */
    @Override
    public Stream<StoredObject> listObjects(String prefix) {
        return s3Client.listObjectsV2Paginator(
                        ListObjectsV2Request.builder()
                                .bucket(storageConfig.getBucketName())
                                .prefix(prefix == null ? "" : prefix)
                                .build())
                .contents()
                .stream()
                // A key ending in "/" is a folder marker some tools create, not a file.
                .filter(object -> !object.key().endsWith("/"))
                .map(object -> new StoredObject(object.key(), object.lastModified()));
    }

    private HeadObjectRequest headObject(String key) {
        return HeadObjectRequest.builder()
                .bucket(storageConfig.getBucketName())
                .key(key)
                .build();
    }
}
