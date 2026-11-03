package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.StorageConfig;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageBucketServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private StorageConfig storageConfig;

    @InjectMocks
    private StorageBucketService storageBucketService;

    @BeforeEach
    void setUp() {
        when(storageConfig.getBucketName()).thenReturn("sas-documents");
    }

    @Test
    void createBucketIfNotExists_bucketMissing_createsIt() {
        when(storageConfig.isCreateBucket()).thenReturn(true);
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("missing").build());

        storageBucketService.createBucketIfNotExists();

        verify(s3Client).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void createBucketIfNotExists_bucketPresent_leavesItAlone() {
        when(storageConfig.isCreateBucket()).thenReturn(true);
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        storageBucketService.createBucketIfNotExists();

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    /**
     * On S3 the bucket belongs to Terraform and the instance role has no s3:CreateBucket, so the
     * application must not call S3 at all for it (GH-331).
     */
    @Test
    void createBucketIfNotExists_creationDisabled_makesNoStorageCalls() {
        when(storageConfig.isCreateBucket()).thenReturn(false);

        storageBucketService.createBucketIfNotExists();

        verifyNoInteractions(s3Client);
    }

    @Test
    void createBucketIfNotExists_storageUnreachable_throwsFileStorageException() {
        when(storageConfig.isCreateBucket()).thenReturn(true);
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(SdkClientException.create("connection refused"));

        assertThatThrownBy(() -> storageBucketService.createBucketIfNotExists())
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("sas-documents");
    }
}
