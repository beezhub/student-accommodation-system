package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
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
        
        // Mock ErrorResponseException with NoSuchKey code
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        ErrorResponseException exception = new ErrorResponseException(errorResponse, null, null);
        
        doThrow(exception).when(minioClient).statObject(any(StatObjectArgs.class));

        boolean exists = minioFileStorageService.exists(key);

        assertFalse(exists);
    }

    @Test
    void generatePresignedUrl_validKeyAndExpiry_returnsUrl() throws Exception {
        String key = "test/file.txt";
        String expectedUrl = "http://minio/test-bucket/test/file.txt";
        
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(expectedUrl);

        String url = minioFileStorageService.generatePresignedUrl(key, Duration.ofMinutes(10));

        assertEquals(expectedUrl, url);
        verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void download_existingKey_returnsInputStream() throws Exception {
        String key = "test/file.txt";
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(mockResponse);

        InputStream result = minioFileStorageService.download(key);

        assertNotNull(result);
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }

    @Test
    void download_minioError_throwsFileStorageException() throws Exception {
        String key = "test/file.txt";
        doThrow(new RuntimeException("MinIO error")).when(minioClient).getObject(any(GetObjectArgs.class));

        assertThrows(FileStorageException.class, () -> minioFileStorageService.download(key));
    }

    @Test
    void listObjects_withPrefix_passesPrefixAndMapsItemsSkippingDirectories() {
        java.time.ZonedDateTime modified = java.time.ZonedDateTime.parse("2026-09-01T10:15:30Z");
        io.minio.messages.Item file = mock(io.minio.messages.Item.class);
        when(file.isDir()).thenReturn(false);
        when(file.objectName()).thenReturn("Student/1/ID/a.pdf");
        when(file.lastModified()).thenReturn(modified);
        io.minio.messages.Item dir = mock(io.minio.messages.Item.class);
        when(dir.isDir()).thenReturn(true);

        org.mockito.ArgumentCaptor<ListObjectsArgs> args = org.mockito.ArgumentCaptor.forClass(ListObjectsArgs.class);
        when(minioClient.listObjects(args.capture()))
                .thenReturn(java.util.List.of(new Result<>(dir), new Result<>(file)));

        java.util.List<StoredObject> result = minioFileStorageService.listObjects("Student/").toList();

        assertEquals("test-bucket", args.getValue().bucket());
        assertEquals("Student/", args.getValue().prefix());
        assertTrue(args.getValue().recursive());
        assertEquals(1, result.size());
        assertEquals("Student/1/ID/a.pdf", result.get(0).key());
        assertEquals(modified.toInstant(), result.get(0).lastModified());
    }

    @Test
    void listObjects_nullPrefix_listsWholeBucket() {
        org.mockito.ArgumentCaptor<ListObjectsArgs> args = org.mockito.ArgumentCaptor.forClass(ListObjectsArgs.class);
        when(minioClient.listObjects(args.capture())).thenReturn(java.util.List.of());

        assertEquals(0, minioFileStorageService.listObjects(null).count());
        assertEquals("", args.getValue().prefix());
    }
}
