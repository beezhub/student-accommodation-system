package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String upload(String key, InputStream content, long size, String contentType) {
        String bucketName = minioConfig.getBucketName();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(content, size, -1)
                            .contentType(contentType)
                            .build());
            return key;
        } catch (Exception e) {
            throw new FileStorageException("Error uploading file to MinIO: " + key, e);
        }
    }

    @Override
    public InputStream download(String key) {
        String bucketName = minioConfig.getBucketName();
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error downloading file from MinIO: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        String bucketName = minioConfig.getBucketName();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error deleting file from MinIO: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        String bucketName = minioConfig.getBucketName();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code()) || "NoSuchObject".equals(e.errorResponse().code())) {
                return false;
            }
            throw new FileStorageException("Error checking file existence in MinIO: " + key, e);
        } catch (Exception e) {
            throw new FileStorageException("Error checking file existence in MinIO: " + key, e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration expiry) {
        String bucketName = minioConfig.getBucketName();
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .method(Method.GET)
                            .expiry((int) expiry.toSeconds())
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error generating presigned URL for key: " + key, e);
        }
    }

    @Override
    public Stream<String> listObjects() {
        String bucketName = minioConfig.getBucketName();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build());

        return StreamSupport.stream(results.spliterator(), false)
                .map(itemResult -> {
                    try {
                        return itemResult.get().objectName();
                    } catch (Exception e) {
                        throw new FileStorageException("Error listing objects from MinIO", e);
                    }
                });
    }
}
