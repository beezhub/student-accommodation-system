package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentLockedException;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.exceptions.FileValidationException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
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
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy;
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
    DocumentMapper documentMapper;
    @Mock
    ApplicationRepository applicationRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    com.beezhub.student_accommodation.service.document.DocumentAccessGuard documentAccessGuard;
    @Mock
    DocumentExpiryPolicy documentExpiryPolicy;

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

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/file.pdf");
        when(fileValidationService.scanFile(any(), anyString(), anyLong())).thenReturn(ValidationResult.valid("application/pdf"));

        documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L);

        verify(fileValidationService, times(1)).scanFile(any(), anyString(), anyLong());
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

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/id.pdf");
        when(fileValidationService.scanFile(any(), anyString(), anyLong())).thenReturn(ValidationResult.valid("application/pdf"));

        // Should succeed — no application needed
        assertDoesNotThrow(() ->
                documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));

        verify(documentRepository, times(1)).save(argThat(doc -> doc.getStudent() != null));
    }

    @Test
    void uploadSingleDocument_dbFailure_storedFileDeleted() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.pdf");
        when(file.getSize()).thenReturn(10L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(anyLong())).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));

        when(documentRepository.save(any(Document.class))).thenThrow(new RuntimeException("DB Error"));

        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString())).thenReturn("path/to/file.pdf");
        when(fileValidationService.scanFile(any(), anyString(), anyLong())).thenReturn(ValidationResult.valid("application/pdf"));

        assertThrows(RuntimeException.class, () -> documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));

        verify(fileValidationService, times(1)).scanFile(any(), anyString(), anyLong());
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

        when(fileValidationService.scanFile(any(), anyString(), anyLong())).thenReturn(ValidationResult.invalid("Malware detected"));

        FileValidationException ex = assertThrows(FileValidationException.class,
            () -> documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));
        assertTrue(ex.getMessage().contains("File rejected"));

        verify(fileValidationService, times(1)).scanFile(any(), anyString(), anyLong());
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

    // --------------------------------------------------------- GH-498 expiring documents

    /** An approved document, a live application at company 7, and an upload replacing it. */
    private MultipartFile givenReplacementOfApprovedDocumentOnALiveApplication(boolean dueForReplacement)
            throws Exception {
        document.setDocumentStatus(DocumentStatus.APPROVED);
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn("new.pdf");
        lenient().when(file.getSize()).thenReturn(10L);
        lenient().when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1 }));
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(2L)).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(documentRepository.findByStudentIdAndDocumentTypeId(10L, 2L)).thenReturn(Optional.of(document));
        when(applicationRepository.existsByStudent_IdAndStatusIn(eq(10L), any())).thenReturn(true);
        Application live = Application.builder().id(30L).tenantId(7L).student(student).build();
        when(applicationRepository.findByStudent_IdAndStatusInOrderByIdDesc(eq(10L), any())).thenReturn(List.of(live));
        DocumentExpiryPolicy.CompanyRules rules = mock(DocumentExpiryPolicy.CompanyRules.class);
        when(documentExpiryPolicy.forCompany(7L)).thenReturn(rules);
        when(rules.isDueForReplacement(document, null)).thenReturn(dueForReplacement);
        return file;
    }

    /** Expired, or expiring within the reminder period, at the live application's company. */
    @Test
    void uploadSingleDocument_approvedButDueForReplacement_isReplacedDuringALiveApplication() throws Exception {
        MultipartFile file = givenReplacementOfApprovedDocumentOnALiveApplication(true);
        when(fileValidationService.scanFile(any(), anyString(), anyLong())).thenReturn(ValidationResult.valid("application/pdf"));
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

        documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L);

        verify(documentRepository).delete(document);
        ArgumentCaptor<Document> saved = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(saved.capture());
        assertEquals(DocumentStatus.PENDING, saved.getValue().getDocumentStatus());
    }

    /** A current approved document stays locked while the application is live. */
    @Test
    void uploadSingleDocument_approvedAndCurrent_isRefusedDuringALiveApplication() throws Exception {
        MultipartFile file = givenReplacementOfApprovedDocumentOnALiveApplication(false);

        assertThrows(DocumentLockedException.class,
                () -> documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));

        verify(documentRepository, never()).delete(any(Document.class));
        verify(documentRepository, never()).save(any(Document.class));
    }

    /** Only the upload path is relaxed: deleting still waits for an administrator. */
    @Test
    void deleteDocument_approvedOnALiveApplication_doesNotConsultExpiry() {
        givenApprovedDocumentOnALiveApplication();

        assertThrows(DocumentLockedException.class,
                () -> documentLifecycleService.deleteDocument(100L, "user@example.com"));

        verifyNoInteractions(documentExpiryPolicy);
    }

    // --------------------------------------------------------- GH-144 stored content type

    /**
     * The declared multipart content type is attacker-chosen, and it used to be stored verbatim
     * and echoed back on download, so the client controlled the response Content-Type. What
     * reaches storage must be the type resolved from the file bytes instead.
     */
    @Test
    void uploadDocuments_declaredContentTypeIsIgnored_theResolvedOneIsStored() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getContentType()).thenReturn("text/html");
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(2L)).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(file.getOriginalFilename()).thenReturn("transcript.pdf");
        when(file.getSize()).thenReturn(13L);
        when(file.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        when(fileValidationService.scanFile(any(InputStream.class), anyString(), anyLong()))
                .thenReturn(ValidationResult.valid("application/pdf"));
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArguments()[0]);

        documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L);

        verify(fileStorageService).upload(anyString(), any(InputStream.class), anyLong(),
                eq("application/pdf"));
    }

    /** A rejected file must never reach storage at all. */
    @Test
    void uploadDocuments_validationRejects_nothingIsStored() throws Exception {
        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(documentTypeRepository.findById(2L)).thenReturn(Optional.of(docType));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("cv.pdf");
        when(file.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        when(fileValidationService.scanFile(any(InputStream.class), anyString(), anyLong()))
                .thenReturn(ValidationResult.invalid("the file contents are not a PDF, JPG or PNG"));

        assertThrows(FileValidationException.class,
                () -> documentLifecycleService.uploadSingleDocument(file, "user@example.com", 2L));

        verify(fileStorageService, never()).upload(anyString(), any(InputStream.class), anyLong(), anyString());
    }
}
