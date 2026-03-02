package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest.DocumentUpdate;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationDocumentServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private DocumentCommentRepository documentCommentRepository;
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ApplicationDocumentService service;

    private Application application;
    private AppUser adminUser;
    private AppUser studentUser;
    private DocumentType docType1;
    private DocumentType docType2;
    private Document document1;
    private ApplicationDocument appDoc1;

    @BeforeEach
    void setUp() {
        application = new Application();
        application.setId(1L);

        adminUser = new AppUser();
        adminUser.setId(10L);
        adminUser.setEmail("admin@test.com");
        adminUser.setUserRole(UserRole.PLATFORM_ADMIN);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");

        studentUser = new AppUser();
        studentUser.setId(20L);
        studentUser.setEmail("student@test.com");
        studentUser.setUserRole(UserRole.STUDENT);

        docType1 = new DocumentType();
        docType1.setId(100L);
        docType1.setTypeName("ID");
        docType1.setIsRequired(true);

        docType2 = new DocumentType();
        docType2.setId(200L);
        docType2.setTypeName("PROOF_OF_RESIDENCE");
        docType2.setIsRequired(true);

        document1 = new Document();
        document1.setId(1000L);
        document1.setDocumentType(docType1);
        document1.setDocumentStatus(DocumentStatus.APPROVED);

        appDoc1 = new ApplicationDocument();
        appDoc1.setApplication(application);
        appDoc1.setDocument(document1);
    }

    @Test
    void getRequiredDocumentsChecklist_ApplicationExists_ReturnsChecklist() {
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(documentTypeRepository.findByIsRequiredTrue()).thenReturn(List.of(docType1, docType2));
        when(applicationDocumentRepository.findByApplication_Id(1L)).thenReturn(List.of(appDoc1));

        RequiredDocumentsChecklistResponse response = service.getRequiredDocumentsChecklist(1L);

        assertThat(response.getTotalRequired()).isEqualTo(2);
        assertThat(response.getApproved()).isEqualTo(1);
        assertThat(response.getNotUploaded()).isEqualTo(1);
        assertThat(response.getDocuments()).hasSize(2);
        assertThat(response.getAllApproved()).isFalse();
    }

    @Test
    void getRequiredDocumentsChecklist_ApplicationNotFound_ThrowsException() {
        when(applicationRepository.existsById(1L)).thenReturn(false);

        assertThrows(ApplicationNotFoundException.class, () -> service.getRequiredDocumentsChecklist(1L));
    }

    @Test
    void saveReviewProgress_ValidRequest_UpdatesDocumentAndAddsComment() {
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        
        DocumentUpdate update = new DocumentUpdate();
        update.setDocumentId(1000L);
        update.setNewStatus("REJECTED");
        update.setComment("Blurry image");
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update));

        when(documentRepository.findById(1000L)).thenReturn(Optional.of(document1));
        when(applicationDocumentRepository.existsByApplication_IdAndDocument_Id(1L, 1000L)).thenReturn(true);

        ReviewProgressResponse response = service.saveReviewProgress(1L, request, "admin@test.com");

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getDocumentsUpdated()).isEqualTo(1);
        assertThat(response.getCommentsAdded()).isEqualTo(1);
        verify(documentRepository).save(document1);
        assertThat(document1.getDocumentStatus()).isEqualTo(DocumentStatus.REJECTED);
        verify(documentCommentRepository).save(any(DocumentComment.class));
    }

    @Test
    void saveReviewProgress_InvalidStatus_ThrowsException() {
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        
        DocumentUpdate update = new DocumentUpdate();
        update.setDocumentId(1000L);
        update.setNewStatus("INVALID");
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update));

        when(documentRepository.findById(1000L)).thenReturn(Optional.of(document1));
        when(applicationDocumentRepository.existsByApplication_IdAndDocument_Id(1L, 1000L)).thenReturn(true);

        assertThrows(InvalidStatusException.class, () -> service.saveReviewProgress(1L, request, "admin@test.com"));
    }

    @Test
    void saveReviewProgress_NotAdmin_ThrowsException() {
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(appUserRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        
        ReviewProgressRequest request = new ReviewProgressRequest();

        assertThrows(UnauthorizedAccessException.class, () -> service.saveReviewProgress(1L, request, "student@test.com"));
    }

    @Test
    void saveReviewProgress_DocumentNotBelongingToApp_ThrowsException() {
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        
        DocumentUpdate update = new DocumentUpdate();
        update.setDocumentId(1000L);
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update));

        when(documentRepository.findById(1000L)).thenReturn(Optional.of(document1));
        when(applicationDocumentRepository.existsByApplication_IdAndDocument_Id(1L, 1000L)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () -> service.saveReviewProgress(1L, request, "admin@test.com"));
    }

    @Test
    void areAllRequiredDocumentsApproved_AllApproved_ReturnsTrue() {
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(documentTypeRepository.findByIsRequiredTrue()).thenReturn(List.of(docType1));
        when(applicationDocumentRepository.findByApplication_Id(1L)).thenReturn(List.of(appDoc1));

        boolean result = service.areAllRequiredDocumentsApproved(1L);

        assertThat(result).isTrue();
    }

    @Test
    void areAllRequiredDocumentsApproved_NotAllApproved_ReturnsFalse() {
        document1.setDocumentStatus(DocumentStatus.PENDING);
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(documentTypeRepository.findByIsRequiredTrue()).thenReturn(List.of(docType1));
        when(applicationDocumentRepository.findByApplication_Id(1L)).thenReturn(List.of(appDoc1));

        boolean result = service.areAllRequiredDocumentsApproved(1L);

        assertThat(result).isFalse();
    }

    @Test
    void getDocumentsWithComments_ReturnsList() {
        DocumentComment comment = new DocumentComment();
        comment.setId(50L);
        comment.setAdmin(adminUser);
        comment.setCommentText("Test comment");
        
        when(applicationDocumentRepository.findByApplication_Id(1L)).thenReturn(List.of(appDoc1));
        when(documentCommentRepository.findByDocument_IdOrderByCreatedAtDesc(1000L)).thenReturn(List.of(comment));

        List<DocumentWithCommentsResponse> responses = service.getDocumentsWithComments(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getComments()).hasSize(1);
        assertThat(responses.get(0).getComments().get(0).getText()).isEqualTo("Test comment");
    }
}