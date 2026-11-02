package com.beezhub.student_accommodation.config.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/** Reported as {@code storage} in /actuator/health: S3 on PROD, the local container elsewhere (GH-331). */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final String bucketName;

    public StorageHealthIndicator(S3Client s3Client, @Value("${s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public Health health() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return Health.up()
                    .withDetail("bucket", bucketName)
                    .withDetail("status", "Available")
                    .build();
        } catch (NoSuchBucketException e) {
            return Health.down()
                    .withDetail("bucket", bucketName)
                    .withDetail("error", "Bucket not found")
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("bucket", bucketName)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
