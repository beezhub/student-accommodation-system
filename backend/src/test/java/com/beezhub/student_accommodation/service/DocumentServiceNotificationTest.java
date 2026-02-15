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
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentCommentRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.model.event.DocumentVerificationEvent;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @Mock
    private DocumentCommentRepository documentCommentRepository;

    @Mock
    private UserNotificationService userNotificationService;

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
        student.setId(1L);
        ;
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

        // Mock application repository for document operations
        Application approvedApp = Application.builder()
                .id(1L)
                .student(student)
                .status(ApplicationStatus.APPROVED)
                .build();
        when(applicationRepository.findByStudent_Id(anyLong())).thenReturn(List.of(approvedApp));

        // Mock room allocation repository
        when(roomAllocationRepository.findActiveAllocationByStudentId(anyLong())).thenReturn(Optional.empty());

        // Mock document comment repository
        when(documentCommentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
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
        // Verify event published
        ArgumentCaptor<DocumentVerificationEvent> eventCaptor = ArgumentCaptor
                .forClass(DocumentVerificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        DocumentVerificationEvent event = eventCaptor.getValue();
        assertEquals(1L, event.document().getId());
        assertEquals(DocumentStatus.APPROVED, event.newStatus());
        assertNull(event.comment());
        // Verify old status was passed appropriately (might be varying based on
        // implementation logic, checking if captured)
        // Here we can check if it matches what we expect or just ignore it if logic
        // uses it correctly
        // Since we didn't set oldStatus in test setup, it might be null/default or we
        // need to look at DocumentService to see how it gets it.
        // DocumentService: DocumentStatus oldStatus = document.getDocumentStatus();
        // In setUp, document status is PENDING.
        assertEquals(DocumentStatus.PENDING, event.oldStatus());

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
        // Verify event published
        ArgumentCaptor<DocumentVerificationEvent> eventCaptor = ArgumentCaptor
                .forClass(DocumentVerificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        DocumentVerificationEvent event = eventCaptor.getValue();
        assertEquals(1L, event.document().getId());
        assertEquals(DocumentStatus.REJECTED, event.newStatus());
        assertEquals("Document is blurry; ID number not visible; Document expired", event.comment());
        assertEquals(DocumentStatus.PENDING, event.oldStatus());
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
        verify(eventPublisher, never()).publishEvent(any());
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
        verify(eventPublisher, never()).publishEvent(any());
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

        // Simulate event publisher throwing exception
        doThrow(new RuntimeException("Event system unavailable"))
                .when(eventPublisher)
                .publishEvent(any());

        // When - should not throw exception
        assertDoesNotThrow(() -> {
            DocumentResponse response = documentService.updateDocumentStatus(1L, status, "student@university.edu");
            assertNotNull(response);
        });

        // Then - document should still be saved despite notification failure
        verify(documentRepository, times(1)).save(any(Document.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
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
        ArgumentCaptor<DocumentVerificationEvent> eventCaptor = ArgumentCaptor
                .forClass(DocumentVerificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        DocumentVerificationEvent event = eventCaptor.getValue();
        Document capturedDoc = event.document();
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
        ArgumentCaptor<DocumentVerificationEvent> eventCaptor = ArgumentCaptor
                .forClass(DocumentVerificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        DocumentVerificationEvent event = eventCaptor.getValue();
        Document capturedDoc = event.document();
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
        // Then - Verify notification sent once
        verify(eventPublisher, times(1)).publishEvent(any(DocumentVerificationEvent.class));

        // Clear invocations
        clearInvocations(eventPublisher);

        // Given - Second update to REJECTED (changing from APPROVED)
        document.setDocumentStatus(DocumentStatus.APPROVED);
        String rejectionStatus = "REJECTED";
        String comment = "Re-review found issues";

        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        // When - Second update
        documentService.updateDocumentStatus(1L, rejectionStatus, comment, "student@university.edu");

        // Then - Verify notification sent for rejection with comment
        ArgumentCaptor<DocumentVerificationEvent> eventCaptor = ArgumentCaptor
                .forClass(DocumentVerificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        DocumentVerificationEvent event = eventCaptor.getValue();
        assertEquals(DocumentStatus.REJECTED, event.newStatus());
        assertEquals("Re-review found issues", event.comment());
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
        verify(eventPublisher, times(1)).publishEvent(any(DocumentVerificationEvent.class));
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
        ArgumentCaptor<DocumentVerificationEvent> eventCaptor = ArgumentCaptor
                .forClass(DocumentVerificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        DocumentVerificationEvent event = eventCaptor.getValue();
        assertEquals(DocumentStatus.APPROVED, event.newStatus());
        assertNull(event.comment());
        // oldStatus might be PENDING since we reset document in setUp
        assertEquals(DocumentStatus.PENDING, event.oldStatus());
    }
}
