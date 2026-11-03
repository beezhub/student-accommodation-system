package com.beezhub.student_accommodation.config.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageHealthIndicatorTest {

    @Mock
    private S3Client s3Client;

    private StorageHealthIndicator storageHealthIndicator;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        storageHealthIndicator = new StorageHealthIndicator(s3Client, bucketName);
    }

    @Test
    void health_bucketExists_statusUp() {
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        Health health = storageHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("bucket", bucketName);
    }

    @Test
    void health_bucketDoesNotExist_statusDown() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("missing").build());

        Health health = storageHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Bucket not found");
    }

    @Test
    void health_exceptionThrown_statusDown() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(SdkClientException.create("Connection failed"));

        Health health = storageHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Connection failed");
    }
}
