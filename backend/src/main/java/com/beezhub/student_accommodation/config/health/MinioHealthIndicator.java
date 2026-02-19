package com.beezhub.student_accommodation.config.health;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioHealthIndicator(MinioClient minioClient, @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @Override
    public Health health() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder().bucket(bucketName).build());
            if (bucketExists) {
                return Health.up()
                        .withDetail("bucket", bucketName)
                        .withDetail("status", "Available")
                        .build();
            } else {
                return Health.down()
                        .withDetail("bucket", bucketName)
                        .withDetail("error", "Bucket not found")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("bucket", bucketName)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
