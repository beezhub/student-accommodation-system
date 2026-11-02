package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.StorageConfig;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageBucketService {

    private final S3Client s3Client;
    private final StorageConfig storageConfig;

    @PostConstruct
    public void init() {
        createBucketIfNotExists();
    }

    public void createBucketIfNotExists() {
        String bucketName = storageConfig.getBucketName();
        if (!storageConfig.isCreateBucket()) {
            // S3 on PROD: Terraform owns the bucket and the instance role cannot create one.
            log.info("action=bucketCreationSkipped bucket={}", bucketName);
            return;
        }
        try {
            if (!bucketExists(bucketName)) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Bucket created: {}", bucketName);
            } else {
                log.info("Bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            throw new FileStorageException("Error creating bucket: " + bucketName, e);
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
}
