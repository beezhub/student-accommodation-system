package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    MinioClient minioClient;
    @Mock
    MinioConfig minioConfig;
    @Mock
    DocumentRepository documentRepository;
    @Mock
    DocumentTypeRepository documentTypeRepository;
    @Mock
    AppUserRepository appUserRepository;
    @Mock
    StudentRepository studentRepository;
    @Mock
    DocumentMapper documentMapper;

    @InjectMocks
    DocumentService documentService;

    private AppUser user;
    private Student student;
    private DocumentType docType;
    private Document document;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setFirstName("U");
        user.setLastName("Ser");
        user.setUserRole(UserRole.STUDENT);

        student = new Student();
        student.setId(10L);
        student.setAppUser(user);

        docType = new DocumentType();
        docType.setId(2L);
        docType.setTypeName("ID");

        document = new Document();
        document.setId(100L);
        document.setDocumentName("file.pdf");
        document.setDocumentPath("path");
        document.setStudent(student);
        document.setDocumentType(docType);

        when(minioConfig.getBucketName()).thenReturn("bucket");
    }

    @Test
    void uploadSingleDocument_success() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getSize()).thenReturn(10L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        when(file.getContentType()).thenReturn("application/pdf");

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findStudentByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(new DocumentResponse());

        documentService.uploadSingleDocument(file, "user@example.com", 2L);

        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void getDocumentInfo_success_and_presignedUrl() throws Exception {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(documentMapper.toDocumentResponse(document)).thenReturn(new DocumentResponse());
        // use doReturn to avoid checked exception signature on
        // when(...).thenReturn(...)
        doReturn("http://presigned").when(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));

        var resp = documentService.getDocumentInfo(100L, "user@example.com");
        assertNotNull(resp);
        assertEquals("http://presigned", resp.getFileUrl());
    }

    @Test
    void getDocumentInfo_unauthorized_throws() {
        AppUser other = new AppUser();
        other.setId(2L);
        other.setEmail("other@example.com");
        other.setUserRole(UserRole.STUDENT);
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(other));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.getDocumentInfo(100L, "other@example.com"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void updateDocumentStatus_valid_and_invalid() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(new DocumentResponse());

        var resp = documentService.updateDocumentStatus(100L, "APPROVED", "user@example.com");
        assertNotNull(resp);

        // invalid status
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        assertThrows(RuntimeException.class,
                () -> documentService.updateDocumentStatus(100L, "not-a-status", "user@example.com"));
    }

    @Test
    void deleteDocument_success_and_failure() throws Exception {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        // success
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));
        doNothing().when(documentRepository).delete(document);
        documentService.deleteDocument(100L, "user@example.com");
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));

        // failure from minio
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        doThrow(new RuntimeException("minio error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));
        assertThrows(RuntimeException.class, () -> documentService.deleteDocument(100L, "user@example.com"));
    }

    @Test
    void getAllDocumentsByUserId_authorization() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        // userId mismatch and not admin
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.getAllDocumentsByUserId(999L, "user@example.com"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void getAllDocumentsByUserId_success() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findAllByStudent_AppUser_Id(1L)).thenReturn(java.util.List.of(document));
        when(documentMapper.toDocumentResponseList(anyList())).thenReturn(java.util.List.of(new DocumentResponse()));
        var list = documentService.getAllDocumentsByUserId(1L, "user@example.com");
        assertEquals(1, list.size());
    }

    @Test
    void uploadDocuments_multipleFiles_success() throws Exception {
        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);
        when(f1.getOriginalFilename()).thenReturn("a.pdf");
        when(f1.getSize()).thenReturn(10L);
        when(f1.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(f1.getContentType()).thenReturn("application/pdf");
        when(f2.getOriginalFilename()).thenReturn("b.pdf");
        when(f2.getSize()).thenReturn(20L);
        when(f2.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{2}));
        when(f2.getContentType()).thenReturn("application/pdf");

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(2L)).thenReturn(Optional.of(docType));
        when(documentTypeRepository.findById(3L)).thenReturn(Optional.of(docType));
        when(studentRepository.findStudentByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(new DocumentResponse());

        var out = documentService.uploadDocuments(java.util.List.of(f1, f2), "user@example.com", java.util.List.of(2L, 3L));
        assertEquals(2, out.size());
        verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void downloadDocument_success() throws Exception {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        io.minio.GetObjectResponse response = mock(io.minio.GetObjectResponse.class, RETURNS_DEEP_STUBS);
        when(minioClient.getObject(any())).thenReturn(response);
        when(response.headers().get("Content-Type")).thenReturn("application/pdf");

        var dd = documentService.downloadDocument(100L, "user@example.com");
        assertEquals("file.pdf", dd.getFilename());
        assertEquals("application/pdf", dd.getContentType());
        assertNotNull(dd.getResource());
    }
}
