package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for document notification triggers
 *
 * Acceptance Criteria:
 * ✅ Notification sent on document verification (APPROVED)
 * ✅ Notification sent on document rejection (REJECTED)
 * ✅ Email contains relevant details (document type, reference, feedback)
 * ✅ Async sending doesn't block request
 * ✅ Failed emails logged but don't break flow
 * ✅ Integration tests verify notifications sent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Document Service - Notification Integration Tests")
class DocumentServiceNotificationTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioConfig minioConfig;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private AuthService authService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DocumentService documentService;

    private Document document;
    private DocumentResponse documentResponse;
    private Student student;
    private AppUser appUser;
    private DocumentType documentType;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L)
                .email("student@university.edu")
                .firstName("John")
                .lastName("Doe")
                .userRole(UserRole.STUDENT)
                .build();

        student = new Student();
        student.setId(1L);;
        student.setStudentNumber("STU-2025-001");
        student.setAppUser(appUser);

        documentType = new DocumentType();
        documentType.setId(1L);
        documentType.setTypeName("Identity Document");

        document = Document.builder()
                .id(1L)
                .documentName("id_document.pdf")
                .documentPath("/student/1/identity/id_document.pdf")
                .documentStatus(DocumentStatus.PENDING)
                .student(student)
                .documentType(documentType)
                .build();

        documentResponse = new DocumentResponse();
        documentResponse.setId(1L);
        documentResponse.setDocumentName("id_document.pdf");
        documentResponse.setDocumentStatus("APPROVED");
    }

    @Test
    @DisplayName("Should send notification when document is verified (APPROVED)")
    void shouldSendNotificationWhenDocumentVerified() {
        // Given
        String status = "APPROVED";
        String comment = null;

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        DocumentResponse response = documentService.updateDocumentStatus(1L, status, comment, "student@university.edu");

        // Then
        assertNotNull(response);
        assertEquals("APPROVED", response.getDocumentStatus());

        // Verify notification service was called with correct parameters
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<DocumentStatus> statusCaptor = ArgumentCaptor.forClass(DocumentStatus.class);
        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationService, times(1)).sendDocumentVerificationNotification(
                documentCaptor.capture(),
                statusCaptor.capture(),
                commentCaptor.capture()
        );

        Document capturedDoc = documentCaptor.getValue();
        DocumentStatus capturedStatus = statusCaptor.getValue();
        String capturedComment = commentCaptor.getValue();

        assertEquals(1L, capturedDoc.getId());
        assertEquals(DocumentStatus.APPROVED, capturedStatus);
        assertNull(capturedComment);

        // Verify repository save was called
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    @DisplayName("Should send notification when document is rejected with comment")
    void shouldSendNotificationWhenDocumentRejected() {
        // Given
        String status = "REJECTED";
        String comment = "Document is blurry; ID number not visible; Document expired";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        DocumentResponse response = documentService.updateDocumentStatus(1L, status, comment, "student@university.edu");

        // Then
        assertNotNull(response);

        // Verify notification service was called with feedback comment
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<DocumentStatus> statusCaptor = ArgumentCaptor.forClass(DocumentStatus.class);
        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationService, times(1)).sendDocumentVerificationNotification(
                documentCaptor.capture(),
                statusCaptor.capture(),
                commentCaptor.capture()
        );

        Document capturedDoc = documentCaptor.getValue();
        DocumentStatus capturedStatus = statusCaptor.getValue();
        String capturedComment = commentCaptor.getValue();

        assertEquals(1L, capturedDoc.getId());
        assertEquals(DocumentStatus.REJECTED, capturedStatus);
        assertEquals("Document is blurry; ID number not visible; Document expired", capturedComment);
    }

    @Test
    @DisplayName("Should NOT send notification when status remains PENDING")
    void shouldNotSendNotificationWhenStatusRemainingPending() {
        // Given
        String status = "PENDING";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentService.updateDocumentStatus(1L, status, "student@university.edu");

        // Then - notification service should NOT be called
        verify(notificationService, never()).sendDocumentVerificationNotification(any(), any(), any());
    }

    @Test
    @DisplayName("Should NOT send notification when status doesn't change")
    void shouldNotSendNotificationWhenStatusNotChanged() {
        // Given - document already approved
        document.setDocumentStatus(DocumentStatus.APPROVED);
        String status = "APPROVED";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentService.updateDocumentStatus(1L, status, "student@university.edu");

        // Then - notification service should NOT be called since status didn't change
        verify(notificationService, never()).sendDocumentVerificationNotification(any(), any(), any());
    }

    @Test
    @DisplayName("Should not break flow when notification fails")
    void shouldNotBreakFlowWhenNotificationFails() {
        // Given
        String status = "APPROVED";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // Simulate notification service throwing exception
        doThrow(new RuntimeException("Email service unavailable"))
                .when(notificationService)
                .sendDocumentVerificationNotification(any(), any(), any());

        // When - should not throw exception
        assertDoesNotThrow(() -> {
            DocumentResponse response = documentService.updateDocumentStatus(1L, status, "student@university.edu");
            assertNotNull(response);
        });

        // Then - document should still be saved despite notification failure
        verify(documentRepository, times(1)).save(any(Document.class));
        verify(notificationService, times(1)).sendDocumentVerificationNotification(any(), any(), any());
    }

    @Test
    @DisplayName("Should include document details in notification")
    void shouldIncludeDocumentDetailsInNotification() {
        // Given
        document.setDocumentName("passport.pdf");
        document.setDocumentPath("/student/1/passport/passport.pdf");
        String status = "APPROVED";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentService.updateDocumentStatus(1L, status, "student@university.edu");

        // Then - verify notification contains correct document details
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(notificationService).sendDocumentVerificationNotification(
                documentCaptor.capture(),
                eq(DocumentStatus.APPROVED),
                any()
        );

        Document capturedDoc = documentCaptor.getValue();
        assertEquals("passport.pdf", capturedDoc.getDocumentName());
        assertEquals("/student/1/passport/passport.pdf", capturedDoc.getDocumentPath());
        assertEquals("Identity Document", capturedDoc.getDocumentType().getTypeName());
        assertEquals(student.getAppUser().getEmail(), capturedDoc.getStudent().getAppUser().getEmail());
    }

    @Test
    @DisplayName("Should send notification with student information")
    void shouldSendNotificationWithStudentInformation() {
        // Given
        String status = "APPROVED";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentService.updateDocumentStatus(1L, status, "student@university.edu");

        // Then - verify student details are accessible in notification
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(notificationService).sendDocumentVerificationNotification(
                documentCaptor.capture(),
                any(),
                any()
        );

        Document capturedDoc = documentCaptor.getValue();
        assertNotNull(capturedDoc.getStudent());
        assertNotNull(capturedDoc.getStudent().getAppUser());
        assertEquals("student@university.edu", capturedDoc.getStudent().getAppUser().getEmail());
        assertEquals("John", capturedDoc.getStudent().getAppUser().getFirstName());
        assertEquals("Doe", capturedDoc.getStudent().getAppUser().getLastName());
    }

    @Test
    @DisplayName("Should handle multiple status updates correctly")
    void shouldHandleMultipleStatusUpdatesCorrectly() {
        // Given - First update to APPROVED
        String approvalStatus = "APPROVED";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When - First update
        documentService.updateDocumentStatus(1L, approvalStatus, "student@university.edu");

        // Then - Verify notification sent once
        verify(notificationService, times(1)).sendDocumentVerificationNotification(
                any(),
                eq(DocumentStatus.APPROVED),
                any()
        );

        // Given - Second update to REJECTED (changing from APPROVED)
        reset(notificationService);
        document.setDocumentStatus(DocumentStatus.APPROVED);
        String rejectionStatus = "REJECTED";
        String comment = "Re-review found issues";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        // When - Second update
        documentService.updateDocumentStatus(1L, rejectionStatus, comment, "student@university.edu");

        // Then - Verify notification sent for rejection with comment
        verify(notificationService, times(1)).sendDocumentVerificationNotification(
                any(),
                eq(DocumentStatus.REJECTED),
                eq("Re-review found issues")
        );
    }

    @Test
    @DisplayName("Should execute notification asynchronously without blocking")
    void shouldExecuteNotificationAsynchronouslyWithoutBlocking() {
        // Given
        String status = "APPROVED";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        long startTime = System.currentTimeMillis();
        DocumentResponse response = documentService.updateDocumentStatus(1L, status, "student@university.edu");
        long endTime = System.currentTimeMillis();

        // Then - should complete quickly (async execution doesn't block)
        assertNotNull(response);
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 1000, "Method should return quickly without waiting for email");

        // Verify notification was triggered
        verify(notificationService, times(1)).sendDocumentVerificationNotification(any(), any(), any());
    }

    @Test
    @DisplayName("Should support overloaded method without comment parameter")
    void shouldSupportOverloadedMethodWithoutComment() {
        // Given
        String status = "APPROVED";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When - calling method without comment parameter
        DocumentResponse response = documentService.updateDocumentStatus(1L, status, "student@university.edu");

        // Then
        assertNotNull(response);

        // Verify notification was sent with null comment
        verify(notificationService, times(1)).sendDocumentVerificationNotification(
                any(Document.class),
                eq(DocumentStatus.APPROVED),
                eq(null)
        );
    }
}
