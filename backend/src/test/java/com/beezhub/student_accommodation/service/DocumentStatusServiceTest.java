package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ApplicationNotApprovedException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private com.beezhub.student_accommodation.service.document.DocumentAccessGuard documentAccessGuard;

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
    }

    @Test
    void updateDocumentStatus_approvedStatus_publishesDocumentStatusEvent() {
        // Given
        String status = "APPROVED";
        String comment = null;

        when(applicationDocumentCoordinator.findReviewableApplication(anyLong(), any())).thenReturn(application);
        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
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
    void updateDocumentStatus_rejectedStatusWithComment_createsRejectionComment() {
        // Given
        String status = "REJECTED";
        String comment = "Document is blurry";

        when(applicationDocumentCoordinator.findReviewableApplication(anyLong(), any())).thenReturn(application);
        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
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
    void updateDocumentStatus_pendingStatus_doesNotPublishEvent() {
        // Given
        String status = "PENDING";

        when(applicationDocumentCoordinator.findReviewableApplication(anyLong(), any())).thenReturn(application);
        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentStatusService.updateDocumentStatus(1L, status, null, "student@university.edu");

        // Then
        verify(documentEventService, never()).publishDocumentStatusEvent(any(), any(), any(), any());
    }

    @Test
    void updateDocumentStatus_statusUnchanged_doesNotPublishEvent() {
        // Given - document already approved
        document.setDocumentStatus(DocumentStatus.APPROVED);
        String status = "APPROVED";

        when(applicationDocumentCoordinator.findReviewableApplication(anyLong(), any())).thenReturn(application);
        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        when(documentMapper.toDocumentResponse(any(Document.class))).thenReturn(documentResponse);

        // When
        documentStatusService.updateDocumentStatus(1L, status, null, "student@university.edu");

        // Then
        verify(documentEventService, never()).publishDocumentStatusEvent(any(), any(), any(), any());
    }

    @Test
    void updateDocumentStatus_approvedStatus_passesDocumentToEventPublisher() {
        // Given
        String status = "APPROVED";

        when(applicationDocumentCoordinator.findReviewableApplication(anyLong(), any())).thenReturn(application);
        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
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
    void updateDocumentStatus_noApprovedApplication_throwsApplicationNotApprovedException() {
        when(appUserRepository.findByEmail("student@university.edu")).thenReturn(Optional.of(appUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(applicationDocumentCoordinator.findReviewableApplication(anyLong(), any()))
            .thenThrow(new ApplicationNotApprovedException("Documents can only be reviewed for approved applications"));

        assertThrows(ApplicationNotApprovedException.class,
            () -> documentStatusService.updateDocumentStatus(1L, "APPROVED", null, "student@university.edu"));

        verify(documentRepository, never()).save(any());
    }
}
