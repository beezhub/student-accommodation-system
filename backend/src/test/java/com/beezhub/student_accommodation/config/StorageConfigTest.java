package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The client works against MinIO locally and on NONPROD, and against Amazon S3 on PROD (GH-331).
 * Presigning is local signing, so these run offline and pin how each target is addressed.
 */
class StorageConfigTest {

    private StorageConfig storageConfig;

    @BeforeEach
    void setUp() {
        storageConfig = new StorageConfig();
        ReflectionTestUtils.setField(storageConfig, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(storageConfig, "accessKey", "minioadmin");
        ReflectionTestUtils.setField(storageConfig, "secretKey", "minioadmin");
        ReflectionTestUtils.setField(storageConfig, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(storageConfig, "region", "");
        ReflectionTestUtils.setField(storageConfig, "credentials", StorageConfig.CREDENTIALS_STATIC);
        ReflectionTestUtils.setField(storageConfig, "connectTimeoutSeconds", 30);
        ReflectionTestUtils.setField(storageConfig, "readTimeoutSeconds", 30);
    }

    private String presign(String bucket, String key) {
        try (S3Presigner presigner = storageConfig.s3Presigner()) {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofHours(1))
                            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                            .build())
                    .url()
                    .toString();
        }
    }

    @Test
    void s3Client_minioDefaults_builds() {
        try (S3Client client = storageConfig.s3Client()) {
            assertThat(client).isNotNull();
        }
    }

    /** minio:9000 has no per-bucket subdomain, so MinIO must be addressed path-style. */
    @Test
    void s3Presigner_minioEndpoint_usesPathStyleAndTheDefaultSigningRegion() {
        String url = presign("test-bucket", "student/1/id.pdf");

        assertThat(url).startsWith("http://localhost:9000/test-bucket/student/1/id.pdf?");
        assertThat(url).contains("%2Fus-east-1%2Fs3%2Faws4_request");
    }

    @Test
    void s3Presigner_s3EndpointWithRegion_usesVirtualHostStyleAndThatRegion() {
        ReflectionTestUtils.setField(storageConfig, "endpoint", "https://s3.af-south-1.amazonaws.com");
        ReflectionTestUtils.setField(storageConfig, "region", "af-south-1");

        String url = presign("sas-documents", "student/1/id.pdf");

        assertThat(url).startsWith("https://sas-documents.s3.af-south-1.amazonaws.com/student/1/id.pdf?");
        assertThat(url).contains("X-Amz-Credential=minioadmin%2F");
        assertThat(url).contains("%2Faf-south-1%2Fs3%2Faws4_request");
    }

    /** The instance role replaces the key pair, so the client must build without one. */
    @Test
    void s3Client_awsInstanceRole_buildsWithoutAStaticKeyPair() {
        ReflectionTestUtils.setField(storageConfig, "endpoint", "https://s3.af-south-1.amazonaws.com");
        ReflectionTestUtils.setField(storageConfig, "region", "af-south-1");
        ReflectionTestUtils.setField(storageConfig, "accessKey", null);
        ReflectionTestUtils.setField(storageConfig, "secretKey", null);
        ReflectionTestUtils.setField(storageConfig, "credentials", StorageConfig.CREDENTIALS_AWS_INSTANCE_ROLE);

        try (S3Client client = storageConfig.s3Client()) {
            assertThat(client).isNotNull();
        }
    }

    /** A typo must stop startup rather than fall back to a credential mode nobody chose. */
    @Test
    void s3Client_unknownCredentialsMode_failsAtStartup() {
        ReflectionTestUtils.setField(storageConfig, "credentials", "instance-role");

        assertThatThrownBy(() -> storageConfig.s3Client())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instance-role");
    }
}
