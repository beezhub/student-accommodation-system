package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Document Status Service Tests")
class DocumentStatusServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private DocumentEventService documentEventService;

    @Mock
    private ApplicationDocumentCoordinator applicationDocumentCoordinator;

    @Mock
    private DocumentCommentService documentCommentService;

    @InjectMocks
    private DocumentStatusService documentStatusService;

    private Document document;
    private DocumentResponse documentResponse;
    private Student student;
    private AppUser appUser;
    private DocumentType documentType;
    private Application application;

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

        application = Application.builder()
                .id(100L)
                .student(student)
                .status(ApplicationStatus.APPROVED)
                .build();

        when(applicationDocumentCoordinator.findApprovedApplication(anyLong())).thenReturn(application);
        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
    }

    @Test
    @DisplayName("Should trigger event when document is verified (APPROVED)")
    void shouldTriggerEventWhenDocumentVerified() {
        // Given
        String status = "APPROVED";
        String comment = null;

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        DocumentResponse response = documentStatusService.updateDocumentStatus(1L, status, comment, "student@university.edu");

        // Then
        assertNotNull(response);
        assertEquals("APPROVED", response.getDocumentStatus());

        verify(documentEventService).publishDocumentStatusEvent(
                any(Document.class),
                eq(DocumentStatus.PENDING),
                eq(DocumentStatus.APPROVED),
                eq(null)
        );

        verify(documentRepository).save(any(Document.class));
        verify(applicationDocumentCoordinator).tryAllocateOrQueue(student, application);
    }

    @Test
    @DisplayName("Should trigger event when document is rejected with comment")
    void shouldTriggerEventWhenDocumentRejected() {
        // Given
        String status = "REJECTED";
        String comment = "Document is blurry";

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        DocumentResponse response = documentStatusService.updateDocumentStatus(1L, status, comment, "student@university.edu");

        // Then
        assertNotNull(response);

        verify(documentEventService).publishDocumentStatusEvent(
                any(Document.class),
                eq(DocumentStatus.PENDING),
                eq(DocumentStatus.REJECTED),
                eq(comment)
        );
        
        verify(documentCommentService).createRejectionComment(any(Document.class), eq(appUser), eq(comment));
    }

    @Test
    @DisplayName("Should NOT trigger event when status remains PENDING")
    void shouldNotTriggerEventWhenStatusRemainingPending() {
        // Given
        String status = "PENDING";

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentStatusService.updateDocumentStatus(1L, status, null, "student@university.edu");

        // Then
        verify(documentEventService, never()).publishDocumentStatusEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should NOT trigger event when status doesn't change")
    void shouldNotTriggerEventWhenStatusNotChanged() {
        // Given - document already approved
        document.setDocumentStatus(DocumentStatus.APPROVED);
        String status = "APPROVED";

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentStatusService.updateDocumentStatus(1L, status, null, "student@university.edu");

        // Then
        verify(documentEventService, never()).publishDocumentStatusEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should include document details in event")
    void shouldIncludeDocumentDetailsInEvent() {
        // Given
        String status = "APPROVED";

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentStatusService.updateDocumentStatus(1L, status, null, "student@university.edu");

        // Then
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentEventService).publishDocumentStatusEvent(docCaptor.capture(), any(), any(), any());

        Document capturedDoc = docCaptor.getValue();
        assertEquals("id_document.pdf", capturedDoc.getDocumentName());
    }

    @Test
    @DisplayName("Should support overloaded method without comment parameter")
    void shouldSupportOverloadedMethodWithoutComment() {
        // Given
        String status = "APPROVED";

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When - calling method with null comment (simulating overload)
        documentStatusService.updateDocumentStatus(1L, status, null, "student@university.edu");

        // Then
        verify(documentEventService).publishDocumentStatusEvent(
                any(Document.class),
                eq(DocumentStatus.PENDING),
                eq(DocumentStatus.APPROVED),
                eq(null)
        );
    }
}
