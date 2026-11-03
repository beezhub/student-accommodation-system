package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.config.StorageConfig;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    S3Client s3Client;
    @Mock
    S3Presigner s3Presigner;
    @Mock
    StorageConfig storageConfig;

    @InjectMocks
    S3FileStorageService s3FileStorageService;

    @BeforeEach
    void setUp() {
        when(storageConfig.getBucketName()).thenReturn("test-bucket");
    }

    @Test
    void upload_validFile_returnsKeyAndSendsTypeAndLength() {
        String key = "test/file.txt";
        InputStream content = new ByteArrayInputStream("test".getBytes());

        String result = s3FileStorageService.upload(key, content, 4, "text/plain");

        assertEquals(key, result);
        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(put.capture(), any(RequestBody.class));
        assertEquals("test-bucket", put.getValue().bucket());
        assertEquals(key, put.getValue().key());
        assertEquals("text/plain", put.getValue().contentType());
        assertEquals(4L, put.getValue().contentLength());
    }

    @Test
    void upload_storageError_throwsFileStorageException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("refused"));

        assertThrows(FileStorageException.class, () -> s3FileStorageService.upload(
                "test/file.txt", new ByteArrayInputStream(new byte[0]), 0, "text/plain"));
    }

    @Test
    void delete_existingFile_removed() {
        s3FileStorageService.delete("test/file.txt");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void exists_fileExists_returnsTrue() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertTrue(s3FileStorageService.exists("test/file.txt"));
    }

    @Test
    void exists_noSuchKey_returnsFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        assertFalse(s3FileStorageService.exists("test/file.txt"));
    }

    /** A HEAD 404 has no body to name the error, so a server may report it as a bare status. */
    @Test
    void exists_bare404_returnsFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        assertFalse(s3FileStorageService.exists("test/file.txt"));
    }

    @Test
    void exists_accessDenied_throwsFileStorageException() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Access Denied").build());

        assertThrows(FileStorageException.class, () -> s3FileStorageService.exists("test/file.txt"));
    }

    @Test
    void generatePresignedUrl_validKeyAndExpiry_returnsUrlWithAttachmentAndClampedType() throws Exception {
        String key = "test/file.pdf";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentType("application/pdf").build());
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://test-bucket.s3.af-south-1.amazonaws.com/test/file.pdf").toURL());
        ArgumentCaptor<GetObjectPresignRequest> request = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        when(s3Presigner.presignGetObject(request.capture())).thenReturn(presigned);

        String url = s3FileStorageService.generatePresignedUrl(key, Duration.ofMinutes(10));

        assertEquals("https://test-bucket.s3.af-south-1.amazonaws.com/test/file.pdf", url);
        assertEquals(Duration.ofMinutes(10), request.getValue().signatureDuration());
        GetObjectRequest getObject = request.getValue().getObjectRequest();
        assertEquals("attachment", getObject.responseContentDisposition());
        assertEquals(DocumentContentTypes.clamp("application/pdf"), getObject.responseContentType());
    }

    @Test
    void download_existingKey_returnsInputStream() {
        ResponseInputStream<GetObjectResponse> stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream("bytes".getBytes())));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream);

        InputStream result = s3FileStorageService.download("test/file.txt");

        assertSame(stream, result);
    }

    @Test
    void download_storageError_throwsFileStorageException() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(SdkClientException.create("refused"));

        assertThrows(FileStorageException.class, () -> s3FileStorageService.download("test/file.txt"));
    }

    @Test
    void listObjects_withPrefix_passesPrefixAndMapsObjectsSkippingFolderMarkers() {
        Instant modified = Instant.parse("2026-09-01T10:15:30Z");
        ListObjectsV2Response page = ListObjectsV2Response.builder()
                .contents(
                        S3Object.builder().key("Student/").build(),
                        S3Object.builder().key("Student/1/ID/a.pdf").lastModified(modified).build())
                .build();
        ArgumentCaptor<ListObjectsV2Request> args = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        when(s3Client.listObjectsV2Paginator(args.capture()))
                .thenAnswer(i -> new ListObjectsV2Iterable(s3Client, i.getArgument(0)));
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(page);

        List<StoredObject> result = s3FileStorageService.listObjects("Student/").toList();

        assertEquals("test-bucket", args.getValue().bucket());
        assertEquals("Student/", args.getValue().prefix());
        assertEquals(1, result.size());
        assertEquals("Student/1/ID/a.pdf", result.get(0).key());
        assertEquals(modified, result.get(0).lastModified());
    }

    @Test
    void listObjects_nullPrefix_listsWholeBucket() {
        ArgumentCaptor<ListObjectsV2Request> args = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        when(s3Client.listObjectsV2Paginator(args.capture()))
                .thenAnswer(i -> new ListObjectsV2Iterable(s3Client, i.getArgument(0)));
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().build());

        assertEquals(0, s3FileStorageService.listObjects(null).count());
        assertEquals("", args.getValue().prefix());
    }
}
