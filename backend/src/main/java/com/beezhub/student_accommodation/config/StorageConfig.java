package com.beezhub.student_accommodation.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * Object storage through the AWS SDK: Amazon S3 on PROD, and the MinIO container locally and on
 * NONPROD, which speaks the same API (docs/PROD_ENVIRONMENT_PLAN.md, GH-331).
 *
 * <p>The properties keep their {@code minio.*} names and {@code MINIO_*} variables, so every
 * existing {@code .env} and compose file still configures the client unchanged.
 */
@Configuration
public class StorageConfig {

    /** Access key and secret key from configuration. Right for MinIO, and for S3 with an IAM user. */
    static final String CREDENTIALS_STATIC = "static";

    /**
     * Temporary credentials from the EC2 instance role, through the instance metadata service. The
     * SDK uses an IMDSv2 session token, so this works with IMDSv1 switched off. From a container on
     * a bridge network the metadata service is two hops away, so the instance needs a metadata
     * response hop limit of 2.
     */
    static final String CREDENTIALS_AWS_INSTANCE_ROLE = "aws-instance-role";

    /** MinIO ignores the region, but the SDK needs one to sign requests. */
    static final String MINIO_SIGNING_REGION = "us-east-1";

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Getter
    @Value("${minio.bucket-name}")
    private String bucketName;

    /** The bucket's region on S3. Blank for MinIO. */
    @Value("${minio.region:}")
    private String region;

    @Value("${minio.credentials:" + CREDENTIALS_STATIC + "}")
    private String credentials;

    /**
     * Whether the application creates its bucket at startup. On for MinIO, where nothing else
     * does. Off for S3, where the bucket and its versioning, encryption and public-access block
     * belong to Terraform, and the instance role is deliberately not allowed to create buckets.
     */
    @Getter
    @Value("${minio.create-bucket:true}")
    private boolean createBucket;

    @Value("${minio.connect-timeout-s:30}")
    private int connectTimeoutSeconds;

    @Value("${minio.read-timeout-s:30}")
    private int readTimeoutSeconds;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(signingRegion())
                .credentialsProvider(credentialsProvider())
                .forcePathStyle(usesPathStyle())
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                        .socketTimeout(Duration.ofSeconds(readTimeoutSeconds)))
                // Since SDK 2.30 every upload carries a CRC checksum by default, sent as a trailer
                // in aws-chunked encoding. Only when an operation requires one keeps uploads plain,
                // which every S3-compatible server accepts, MinIO included.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(signingRegion())
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(usesPathStyle())
                        .build())
                .build();
    }

    private Region signingRegion() {
        return Region.of(region == null || region.isBlank() ? MINIO_SIGNING_REGION : region);
    }

    /**
     * MinIO is addressed as {@code endpoint/bucket/key}: a hostname such as {@code minio:9000} has
     * no per-bucket subdomain. S3 uses its own virtual-hosted style, {@code bucket.endpoint/key}.
     */
    private boolean usesPathStyle() {
        String host = URI.create(endpoint).getHost();
        return host == null || !host.endsWith(".amazonaws.com");
    }

    private AwsCredentialsProvider credentialsProvider() {
        return switch (credentials) {
            case CREDENTIALS_STATIC -> StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
            case CREDENTIALS_AWS_INSTANCE_ROLE -> InstanceProfileCredentialsProvider.create();
            default -> throw new IllegalArgumentException("minio.credentials must be '" + CREDENTIALS_STATIC
                    + "' or '" + CREDENTIALS_AWS_INSTANCE_ROLE + "', not '" + credentials + "'");
        };
    }

}
