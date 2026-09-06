package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentLockedException;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.exceptions.FileValidationException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.model.event.StoredFileDeletionEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import com.beezhub.student_accommodation.service.document.FileValidationService;
import com.beezhub.student_accommodation.service.document.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
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
    DocumentMapper documentMapper;
    @Mock
    ApplicationRepository applicationRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

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

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/file.pdf");
        when(fileValidationService.scanFile(any(), anyString())).thenReturn(ValidationResult.valid());

        documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L);

        verify(fileValidationService, times(1)).scanFile(any(), anyString());
        verify(fileStorageService, times(1)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    /**
     * Verifies that upload succeeds without requiring an active application.
     * Documents are now decoupled from applications (TASK-2.01).
     */
    @Test
    void uploadDocument_withoutApplicationId_succeeds() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("id.pdf");
        when(file.getSize()).thenReturn(20L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 4, 5, 6 }));
        when(file.getContentType()).thenReturn("application/pdf");

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/id.pdf");
        when(fileValidationService.scanFile(any(), anyString())).thenReturn(ValidationResult.valid());

        // Should succeed — no application needed
        assertDoesNotThrow(() ->
                documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));

        verify(documentRepository, times(1)).save(argThat(doc -> doc.getStudent() != null));
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
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));

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
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.findByStudentIdAndDocumentTypeId(student.getId(), docType.getId())).thenReturn(Optional.empty());

        when(fileValidationService.scanFile(any(), anyString())).thenReturn(ValidationResult.invalid("Malware detected"));

        FileValidationException ex = assertThrows(FileValidationException.class,
            () -> documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));
        assertTrue(ex.getMessage().contains("File rejected"));

        verify(fileValidationService, times(1)).scanFile(any(), anyString());
        verify(fileStorageService, never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void deleteDocument_validUser_deletesRecordAndDefersTheFile() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        doNothing().when(documentRepository).delete(document);

        documentLifecycleService.deleteDocument(100L, "user@example.com");

        verify(documentRepository, times(1)).delete(document);
        // The storage layer is deliberately untouched here. Deleting inline destroyed the object
        // while the transaction could still roll back, including from the activity-log write this
        // method is annotated with, which joins the same transaction (GH-145).
        verifyNoInteractions(fileStorageService);
        verify(eventPublisher).publishEvent(any(StoredFileDeletionEvent.class));
    }

    @Test
    void deleteDocument_publishesTheObjectPathItRemoved() {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        documentLifecycleService.deleteDocument(100L, "user@example.com");

        ArgumentCaptor<StoredFileDeletionEvent> event =
                ArgumentCaptor.forClass(StoredFileDeletionEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(document.getDocumentPath(), event.getValue().objectPath());
    }

    // ------------------------------------------------------------------ GH-145 review lock

    /** Puts the document in the state the lock protects: approved, application still live. */
    private void givenApprovedDocumentOnALiveApplication() {
        document.setDocumentStatus(DocumentStatus.APPROVED);
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(applicationRepository.existsByStudent_IdAndStatusIn(eq(10L), any())).thenReturn(true);
    }

    /**
     * The checklist and the allocation gate both read the live document pack by student id, so
     * wiping an approved document changed the evidence behind an approved or allocated
     * application underneath them.
     */
    @Test
    void deleteDocument_approvedWithLiveApplication_isRefused() {
        givenApprovedDocumentOnALiveApplication();

        assertThrows(DocumentLockedException.class,
                () -> documentLifecycleService.deleteDocument(100L, "user@example.com"));

        verify(documentRepository, never()).delete(any(Document.class));
        verifyNoInteractions(eventPublisher);
    }

    /** Once nothing depends on the evidence, the student is free to clear their pack. */
    @Test
    void deleteDocument_approvedButNoLiveApplication_isAllowed() {
        document.setDocumentStatus(DocumentStatus.APPROVED);
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(applicationRepository.existsByStudent_IdAndStatusIn(eq(10L), any())).thenReturn(false);

        documentLifecycleService.deleteDocument(100L, "user@example.com");

        verify(documentRepository).delete(document);
    }

    @Test
    void deleteDocument_pendingDocument_isNotLocked() {
        document.setDocumentStatus(DocumentStatus.PENDING);
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        documentLifecycleService.deleteDocument(100L, "user@example.com");

        verify(documentRepository).delete(document);
        verifyNoInteractions(applicationRepository);
    }

    /**
     * Admins are exempt, and this is the only unlock path: a document cannot be rejected once the
     * application has moved past APPROVED, so without this a locked document would be permanent.
     */
    @Test
    void deleteDocument_adminCaller_isNotLocked() {
        AppUser admin = AppUser.builder().id(9L).email("admin@example.com")
                .userRole(UserRole.PLATFORM_ADMIN).build();
        document.setDocumentStatus(DocumentStatus.APPROVED);
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        documentLifecycleService.deleteDocument(100L, "admin@example.com");

        verify(documentRepository).delete(document);
        verifyNoInteractions(applicationRepository);
    }

    /**
     * Pins the status set itself. Without this any future edit to LIVE_APPLICATION_STATUSES passes
     * silently, because every other test stubs the repository on a matcher.
     */
    @Test
    void deleteDocument_locksOnLiveStatusesOnly() {
        givenApprovedDocumentOnALiveApplication();

        assertThrows(DocumentLockedException.class,
                () -> documentLifecycleService.deleteDocument(100L, "user@example.com"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ApplicationStatus>> statuses =
                ArgumentCaptor.forClass(Collection.class);
        verify(applicationRepository).existsByStudent_IdAndStatusIn(eq(10L), statuses.capture());
        assertTrue(statuses.getValue().containsAll(java.util.List.of(
                ApplicationStatus.PENDING,
                ApplicationStatus.UNDER_REVIEW,
                ApplicationStatus.CHANGES_REQUESTED,
                ApplicationStatus.APPROVED,
                ApplicationStatus.AWAITING_ALLOCATION,
                ApplicationStatus.ALLOCATED,
                ApplicationStatus.CHECKED_IN)));
        assertFalse(statuses.getValue().contains(ApplicationStatus.REJECTED));
        assertFalse(statuses.getValue().contains(ApplicationStatus.EXPIRED));
        assertFalse(statuses.getValue().contains(ApplicationStatus.CANCELLED));
    }
}
