package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.exceptions.FileValidationException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import com.beezhub.student_accommodation.service.document.FileValidationService;
import com.beezhub.student_accommodation.service.document.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentLifecycleServiceTest {

    @Mock
    FileStorageService fileStorageService;
    @Mock
    FileValidationService fileValidationService;
    @Mock
    DocumentRepository documentRepository;
    @Mock
    DocumentTypeRepository documentTypeRepository;
    @Mock
    AppUserRepository appUserRepository;
    @Mock
    StudentRepository studentRepository;
    @Mock
    ApplicationRepository applicationRepository;
    @Mock
    DocumentMapper documentMapper;

    @InjectMocks
    DocumentLifecycleService documentLifecycleService;

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
    }

    @Test
    void uploadSingleDocument_success() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getSize()).thenReturn(10L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        when(file.getContentType()).thenReturn("application/pdf");

        Application approvedApp = Application.builder()
                .id(1L)
                .student(student)
                .status(ApplicationStatus.APPROVED)
                .build();
        when(applicationRepository.findByStudent_Id(anyLong())).thenReturn(List.of(approvedApp));

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findStudentByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/file.pdf");
        when(fileValidationService.scanFile(any(), anyString())).thenReturn(ValidationResult.valid());

        documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L);

        verify(fileValidationService, times(1)).scanFile(any(), anyString());
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

        Application approvedApp = Application.builder()
                .id(1L)
                .student(student)
                .status(ApplicationStatus.APPROVED)
                .build();
        when(applicationRepository.findByStudent_Id(anyLong())).thenReturn(List.of(approvedApp));

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findStudentByAppUser(user)).thenReturn(Optional.of(student));
        
        when(documentRepository.save(any(Document.class))).thenThrow(new RuntimeException("DB Error"));
        
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/file.pdf");
        when(fileValidationService.scanFile(any(), anyString())).thenReturn(ValidationResult.valid());

        assertThrows(RuntimeException.class, () -> documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));

        verify(fileValidationService, times(1)).scanFile(any(), anyString());
        verify(fileStorageService, times(1)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(fileStorageService, times(1)).delete(anyString());
    }

    @Test
    void uploadSingleDocument_validationFailure_throwsException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("malware.exe");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        
        when(fileValidationService.scanFile(any(), anyString())).thenReturn(ValidationResult.invalid("Malware detected"));

        FileValidationException ex = assertThrows(FileValidationException.class, 
            () -> documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));
        assertTrue(ex.getMessage().contains("File rejected"));

        verify(fileValidationService, times(1)).scanFile(any(), anyString());
        verify(fileStorageService, never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void deleteDocument_validUser_deletesFileAndRecord() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        doNothing().when(fileStorageService).delete(anyString());
        doNothing().when(documentRepository).delete(document);
        documentLifecycleService.deleteDocument(100L, "user@example.com");
        verify(fileStorageService, times(1)).delete(anyString());
        verify(documentRepository, times(1)).delete(document);
    }

    @Test
    void deleteDocument_storageFailure_suppressesException() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        doThrow(new FileStorageException("storage error")).when(fileStorageService).delete(anyString());
        
        assertDoesNotThrow(() -> documentLifecycleService.deleteDocument(100L, "user@example.com"));
        
        verify(documentRepository).delete(document);
    }
}
