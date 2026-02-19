package com.beezhub.student_accommodation.config.health;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioHealthIndicatorTest {

    @Mock
    private MinioClient minioClient;

    private MinioHealthIndicator minioHealthIndicator;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        minioHealthIndicator = new MinioHealthIndicator(minioClient, bucketName);
    }

    @Test
    void health_bucketExists_statusUp() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        Health health = minioHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("bucket", bucketName);
    }

    @Test
    void health_bucketDoesNotExist_statusDown() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        Health health = minioHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Bucket not found");
    }

    @Test
    void health_exceptionThrown_statusDown() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        Health health = minioHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Connection failed");
    }
}
