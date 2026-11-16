package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.ApplicationNotApprovedException;
import com.beezhub.student_accommodation.exceptions.ApplicationNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.dto.RequiredDocumentsChecklistResponse;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest;
import com.beezhub.student_accommodation.model.dto.ReviewProgressRequest.DocumentUpdate;
import com.beezhub.student_accommodation.model.dto.ReviewProgressResponse;
import com.beezhub.student_accommodation.model.dto.DocumentWithCommentsResponse;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.DocumentTypeService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationDocumentServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentTypeRepository documentTypeRepository;
    @Mock
    private DocumentCommentRepository documentCommentRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private DocumentTypeService documentTypeService;
    @Mock
    private com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    @InjectMocks
    private ApplicationDocumentService service;

    private Application application;
    private Student student;
    private AppUser adminUser;
    private AppUser studentUser;
    private DocumentType docType1;
    private DocumentType docType2;
    private Document document1;

    @BeforeEach
    void setUp() {
        studentUser = new AppUser();
        studentUser.setId(20L);
        studentUser.setEmail("student@test.com");
        studentUser.setUserRole(UserRole.STUDENT);

        student = new Student();
        student.setId(5L);
        student.setAppUser(studentUser);

        application = new Application();
        application.setId(1L);
        application.setTenantId(42L);
        application.setStudent(student);
        application.setStatus(ApplicationStatus.UNDER_REVIEW);

        adminUser = new AppUser();
        adminUser.setId(10L);
        adminUser.setEmail("admin@test.com");
        adminUser.setUserRole(UserRole.PLATFORM_ADMIN);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");

        docType1 = new DocumentType();
        docType1.setId(100L);
        docType1.setTenantId(0L);
        docType1.setTypeName("ID");
        docType1.setIsRequired(true);

        docType2 = new DocumentType();
        docType2.setId(200L);
        docType2.setTenantId(0L);
        docType2.setTypeName("PROOF_OF_RESIDENCE");
        docType2.setIsRequired(true);

        document1 = new Document();
        document1.setId(1000L);
        document1.setDocumentType(docType1);
        document1.setDocumentStatus(DocumentStatus.APPROVED);
        document1.setStudent(student);
    }

    @Test
    void getRequiredDocumentsChecklist_ApplicationExists_ReturnsChecklist() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(documentTypeService.getEffectiveDocumentTypes(42L)).thenReturn(List.of(docType1, docType2));
        when(documentRepository.findAllByStudent_Id(5L)).thenReturn(List.of(document1));

        RequiredDocumentsChecklistResponse response = service.getRequiredDocumentsChecklist(1L);

        assertThat(response.getTotalRequired()).isEqualTo(2);
        assertThat(response.getApproved()).isEqualTo(1);
        assertThat(response.getNotUploaded()).isEqualTo(1);
        assertThat(response.getDocuments()).hasSize(2);
        assertThat(response.getAllApproved()).isFalse();
    }

    /**
     * GH-443: a student can now upload against a company's own required type, offered on the
     * Documents page. That upload must tick the company's checklist item.
     */
    @Test
    void getRequiredDocumentsChecklist_documentOfCompanyOwnType_countsAsUploaded() {
        DocumentType bursary = new DocumentType();
        bursary.setId(300L);
        bursary.setTenantId(42L);
        bursary.setTypeName("Bursary Letter");
        bursary.setIsRequired(true);
        Document bursaryLetter = new Document();
        bursaryLetter.setId(3000L);
        bursaryLetter.setDocumentType(bursary);
        bursaryLetter.setDocumentStatus(DocumentStatus.PENDING);
        bursaryLetter.setStudent(student);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(documentTypeService.getEffectiveDocumentTypes(42L)).thenReturn(List.of(docType1, bursary));
        when(documentRepository.findAllByStudent_Id(5L)).thenReturn(List.of(document1, bursaryLetter));

        RequiredDocumentsChecklistResponse response = service.getRequiredDocumentsChecklist(1L);

        assertThat(response.getNotUploaded()).isZero();
        assertThat(response.getPending()).isEqualTo(1);
        assertThat(response.getDocuments())
                .filteredOn(item -> "Bursary Letter".equals(item.getType()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getStatus()).isEqualTo("PENDING");
                    assertThat(item.getDocumentId()).isEqualTo(3000L);
                });
    }

    @Test
    void getRequiredDocumentsChecklist_ApplicationNotFound_ThrowsException() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ApplicationNotFoundException.class, () -> service.getRequiredDocumentsChecklist(1L));
    }

    @Test
    void saveReviewProgress_ValidRequest_UpdatesDocumentAndAddsComment() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        DocumentUpdate update = new DocumentUpdate();
        update.setDocumentId(1000L);
        update.setNewStatus("REJECTED");
        update.setComment("Blurry image");
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update));

        when(documentRepository.findById(1000L)).thenReturn(Optional.of(document1));

        ReviewProgressResponse response = service.saveReviewProgress(1L, request, "admin@test.com");

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getDocumentsUpdated()).isEqualTo(1);
        assertThat(response.getCommentsAdded()).isEqualTo(1);
        verify(documentRepository).save(document1);
        assertThat(document1.getDocumentStatus()).isEqualTo(DocumentStatus.REJECTED);
        verify(documentCommentRepository).save(any(DocumentComment.class));
    }

    /**
     * GH-231: this endpoint and the single-document status endpoint are two halves of one review
     * screen, but disagreed on which application statuses permit review. This one accepted any
     * status at all, including terminal ones, while the other required exactly APPROVED. They now
     * accept the same set: every live status, and nothing terminal.
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = ApplicationStatus.class,
            names = {"REJECTED", "EXPIRED", "CANCELLED"})
    void saveReviewProgress_terminalApplication_isRefused(ApplicationStatus terminal) {
        application.setStatus(terminal);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update(1000L, "REJECTED", null)));

        assertThrows(ApplicationNotApprovedException.class,
                () -> service.saveReviewProgress(1L, request, "admin@test.com"));

        verify(documentRepository, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
            value = ApplicationStatus.class,
            names = {"PENDING", "UNDER_REVIEW", "CHANGES_REQUESTED", "APPROVED", "AWAITING_ALLOCATION",
                    "ALLOCATED", "CHECKED_IN"})
    void saveReviewProgress_liveApplication_isAllowedAtEveryLiveStatus(ApplicationStatus live) {
        application.setStatus(live);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(documentRepository.findById(1000L)).thenReturn(Optional.of(document1));
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update(1000L, "REJECTED", null)));

        ReviewProgressResponse response = service.saveReviewProgress(1L, request, "admin@test.com");

        assertThat(response.getDocumentsUpdated()).isEqualTo(1);
    }

    private static DocumentUpdate update(Long documentId, String newStatus, String comment) {
        DocumentUpdate update = new DocumentUpdate();
        update.setDocumentId(documentId);
        update.setNewStatus(newStatus);
        update.setComment(comment);
        return update;
    }

    @Test
    void saveReviewProgress_InvalidStatus_ThrowsException() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        DocumentUpdate update = new DocumentUpdate();
        update.setDocumentId(1000L);
        update.setNewStatus("INVALID");
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update));

        when(documentRepository.findById(1000L)).thenReturn(Optional.of(document1));

        assertThrows(InvalidStatusException.class, () -> service.saveReviewProgress(1L, request, "admin@test.com"));
    }

    @Test
    void saveReviewProgress_NotAdmin_ThrowsException() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(appUserRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));

        ReviewProgressRequest request = new ReviewProgressRequest();

        assertThrows(UnauthorizedAccessException.class, () -> service.saveReviewProgress(1L, request, "student@test.com"));
    }

    @Test
    void saveReviewProgress_DocumentNotBelongingToStudent_ThrowsException() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        // Document belongs to a different student
        Student otherStudent = new Student();
        otherStudent.setId(99L);
        Document otherDoc = new Document();
        otherDoc.setId(1000L);
        otherDoc.setStudent(otherStudent);

        DocumentUpdate update = new DocumentUpdate();
        update.setDocumentId(1000L);
        ReviewProgressRequest request = new ReviewProgressRequest();
        request.setDocumentUpdates(List.of(update));

        when(documentRepository.findById(1000L)).thenReturn(Optional.of(otherDoc));

        assertThrows(UnauthorizedAccessException.class, () -> service.saveReviewProgress(1L, request, "admin@test.com"));
    }

    @Test
    void areAllRequiredDocumentsApproved_AllApproved_ReturnsTrue() {
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(documentTypeService.getEffectiveDocumentTypes(42L)).thenReturn(List.of(docType1));
        when(documentRepository.findAllByStudent_Id(5L)).thenReturn(List.of(document1));

        boolean result = service.areAllRequiredDocumentsApproved(1L);

        assertThat(result).isTrue();
    }

    @Test
    void areAllRequiredDocumentsApproved_NotAllApproved_ReturnsFalse() {
        document1.setDocumentStatus(DocumentStatus.PENDING);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(documentTypeService.getEffectiveDocumentTypes(42L)).thenReturn(List.of(docType1));
        when(documentRepository.findAllByStudent_Id(5L)).thenReturn(List.of(document1));

        boolean result = service.areAllRequiredDocumentsApproved(1L);

        assertThat(result).isFalse();
    }

    @Test
    void getDocumentsWithComments_ReturnsList() {
        DocumentComment comment = new DocumentComment();
        comment.setId(50L);
        comment.setAdmin(adminUser);
        comment.setCommentText("Test comment");

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(documentRepository.findAllByStudent_Id(5L)).thenReturn(List.of(document1));
        when(documentCommentRepository.findByDocument_IdOrderByCreatedAtDesc(1000L)).thenReturn(List.of(comment));

        List<DocumentWithCommentsResponse> responses = service.getDocumentsWithComments(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getComments()).hasSize(1);
        assertThat(responses.get(0).getComments().get(0).getText()).isEqualTo("Test comment");
    }
}
