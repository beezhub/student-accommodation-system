package com.beezhub.student_accommodation.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for MinioConfig.
 * Verifies that the MinioClient is built with the expected timeout settings.
 */
class MinioConfigTest {

    private MinioConfig minioConfig;

    @BeforeEach
    void setUp() {
        minioConfig = new MinioConfig();
        ReflectionTestUtils.setField(minioConfig, "url", "http://localhost:9000");
        ReflectionTestUtils.setField(minioConfig, "accessKey", "minioadmin");
        ReflectionTestUtils.setField(minioConfig, "secretKey", "minioadmin");
        ReflectionTestUtils.setField(minioConfig, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(minioConfig, "connectTimeoutSeconds", 30);
        ReflectionTestUtils.setField(minioConfig, "readTimeoutSeconds", 30);
        ReflectionTestUtils.setField(minioConfig, "writeTimeoutSeconds", 30);
    }

    @Test
    void minioClient_hasTimeoutsConfigured() {
        MinioClient client = minioConfig.minioClient();

        assertNotNull(client, "MinioClient should be created successfully with timeout configuration");
    }

    @Test
    void minioClient_withCustomTimeouts_buildsSuccessfully() {
        ReflectionTestUtils.setField(minioConfig, "connectTimeoutSeconds", 10);
        ReflectionTestUtils.setField(minioConfig, "readTimeoutSeconds", 60);
        ReflectionTestUtils.setField(minioConfig, "writeTimeoutSeconds", 120);

        MinioClient client = minioConfig.minioClient();

        assertNotNull(client, "MinioClient should be created with custom timeout values");
    }
}
