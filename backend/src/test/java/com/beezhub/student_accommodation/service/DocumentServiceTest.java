package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    FileStorageService fileStorageService;
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
    @Mock
    ApplicationRepository applicationRepository;
    @Mock
    RoomAllocationRepository roomAllocationRepository;
    @Mock
    UserNotificationService userNotificationService;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    RoomAllocationService roomAllocationService;

    @InjectMocks
    DocumentService documentService;

    private AppUser user;
    private Student student;
    private DocumentType docType;
    private Document document;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("U")
                .lastName("Ser")
                .userRole(UserRole.STUDENT)
                .build();

        student = new Student();
        student.setId(10L);
        student.setAppUser(user);

        docType = new DocumentType();
        docType.setId(2L);
        docType.setTypeName("ID");

        document = new Document();
        document.setId(100L);
        document.setDocumentName("file.pdf");
        document.setDocumentPath("path/to/file.pdf");
        document.setStudent(student);
        document.setDocumentType(docType);

        // Mock application repository for document operations
        Application approvedApp = Application.builder()
                .id(1L)
                .student(student)
                .status(ApplicationStatus.APPROVED)
                .build();
        when(applicationRepository.findByStudent_Id(anyLong())).thenReturn(List.of(approvedApp));

        // Mock room allocation repository
        when(roomAllocationRepository.findActiveAllocationByStudentId(anyLong())).thenReturn(Optional.empty());
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
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/file.pdf");

        documentService.uploadSingleDocument(file, "user@example.com", 2L);

        verify(fileStorageService, times(1)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void uploadSingleDocument_dbFailure_minioFileDeleted() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getSize()).thenReturn(10L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        when(file.getContentType()).thenReturn("application/pdf");

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findStudentByAppUser(user)).thenReturn(Optional.of(student));
        
        // Mock DB failure
        when(documentRepository.save(any(Document.class))).thenThrow(new RuntimeException("DB Error"));
        
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/file.pdf");

        assertThrows(RuntimeException.class, () -> documentService.uploadSingleDocument(file, "user@example.com", 2L));

        // Verify upload called
        verify(fileStorageService, times(1)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        // Verify delete called (compensating transaction)
        verify(fileStorageService, times(1)).delete(anyString());
    }

    @Test
    void getDocumentInfo_success_and_presignedUrl() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(documentMapper.toDocumentResponse(document)).thenReturn(new DocumentResponse());
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class))).thenReturn("http://presigned");

        var resp = documentService.getDocumentInfo(100L, "user@example.com");
        assertNotNull(resp);
        assertEquals("http://presigned", resp.getFileUrl());
    }

    @Test
    void getDocumentInfo_unauthorized_throws() {
        AppUser other = AppUser.builder()
                .id(2L)
                .email("other@example.com")
                .userRole(UserRole.STUDENT)
                .build();
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(other));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentService.getDocumentInfo(100L, "other@example.com"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void deleteDocument_success_and_failure() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        // success
        doNothing().when(fileStorageService).delete(anyString());
        doNothing().when(documentRepository).delete(document);
        documentService.deleteDocument(100L, "user@example.com");
        verify(fileStorageService, times(1)).delete(anyString());

        // failure from storage service
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        doThrow(new RuntimeException("storage error")).when(fileStorageService).delete(anyString());
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
}
