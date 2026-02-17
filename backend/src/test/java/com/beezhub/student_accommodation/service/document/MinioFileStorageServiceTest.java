package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    @Mock
    MinioClient minioClient;
    @Mock
    MinioConfig minioConfig;

    @InjectMocks
    MinioFileStorageService minioFileStorageService;

    @BeforeEach
    void setUp() {
        when(minioConfig.getBucketName()).thenReturn("test-bucket");
    }

    @Test
    void upload_validFile_returnUrl() throws Exception {
        String key = "test/file.txt";
        InputStream content = new ByteArrayInputStream("test".getBytes());
        long size = 4;
        String contentType = "text/plain";

        String result = minioFileStorageService.upload(key, content, size, contentType);

        assertEquals(key, result);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void delete_existingFile_removed() throws Exception {
        String key = "test/file.txt";

        minioFileStorageService.delete(key);

        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void exists_fileExists_returnsTrue() throws Exception {
        String key = "test/file.txt";
        
        // Mock successful statObject
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);

        boolean exists = minioFileStorageService.exists(key);

        assertTrue(exists);
    }
    
    @Test
    void exists_fileDoesNotExist_returnsFalse() throws Exception {
        String key = "test/file.txt";
        
        // Mock exception on statObject
        doThrow(new RuntimeException("Not found")).when(minioClient).statObject(any(StatObjectArgs.class));

        boolean exists = minioFileStorageService.exists(key);

        assertFalse(exists);
    }

    @Test
    void generatePresignedUrl_success() throws Exception {
        String key = "test/file.txt";
        String expectedUrl = "http://minio/test-bucket/test/file.txt";
        
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(expectedUrl);

        String url = minioFileStorageService.generatePresignedUrl(key, Duration.ofMinutes(10));

        assertEquals(expectedUrl, url);
        verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }
}
