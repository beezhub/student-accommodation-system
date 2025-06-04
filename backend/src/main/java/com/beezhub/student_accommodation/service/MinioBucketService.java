package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.MinioConfig;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MinioBucketService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void init() {
        createBucketIfNotExists();
    }

    public void createBucketIfNotExists() {
       String bucketName = minioConfig.getBucketName();
        try {
            boolean bucketExists = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                System.out.println("Bucket created: " + bucketName);
            } else {
                System.out.println("Bucket already exists: " + bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating bucket: " + bucketName, e);
        }
    }
}
