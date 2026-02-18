package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentCommentNotFoundException;
import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.dto.DocumentCommentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentComment;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.model.event.DocumentCommentEvent;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentCommentRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentCommentServiceTest {

    @Mock
    private DocumentCommentRepository documentCommentRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DocumentCommentService documentCommentService;

    private AppUser adminUser;
    private AppUser studentUser;
    private Document document;

    @BeforeEach
    void setUp() {
        adminUser = AppUser.builder().id(1L).email("admin@test.com").firstName("Ad").lastName("Min").userRole(UserRole.ADMIN).build();
        studentUser = AppUser.builder().id(2L).email("student@test.com").userRole(UserRole.STUDENT).build();
        
        Student student = new Student();
        student.setAppUser(studentUser);
        
        document = Document.builder().id(100L).student(student).build();
    }

    @Test
    void addComment_asAdmin_success() {
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        // addComment calls addCommentByAdminId
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        
        DocumentComment savedComment = DocumentComment.builder()
                .id(10L)
                .document(document)
                .admin(adminUser)
                .commentText("Test comment")
                .build();
        when(documentCommentRepository.save(any(DocumentComment.class))).thenReturn(savedComment);

        DocumentCommentResponse response = documentCommentService.addComment(100L, "admin@test.com", "Test comment");

        assertNotNull(response);
        assertEquals("Test comment", response.getCommentText());
        verify(eventPublisher).publishEvent(any(DocumentCommentEvent.class));
    }

    @Test
    void addComment_asNonAdmin_throwsUnauthorized() {
        when(appUserRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(studentUser));

        assertThrows(UnauthorizedAccessException.class, () -> 
            documentCommentService.addComment(100L, "student@test.com", "comment"));
    }

    @Test
    void getCommentsByDocument_adminCanView() {
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(documentCommentRepository.findByDocument_IdOrderByCreatedAtDesc(100L)).thenReturn(List.of());

        assertDoesNotThrow(() -> documentCommentService.getCommentsByDocument(100L, "admin@test.com"));
    }

    @Test
    void getCommentsByDocument_ownerCanView() {
        when(appUserRepository.findByEmail("student@test.com")).thenReturn(Optional.of(studentUser));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(documentCommentRepository.findByDocument_IdOrderByCreatedAtDesc(100L)).thenReturn(List.of());

        assertDoesNotThrow(() -> documentCommentService.getCommentsByDocument(100L, "student@test.com"));
    }

    @Test
    void getCommentsByDocument_unauthorizedUser_throws() {
        AppUser otherUser = AppUser.builder().id(3L).email("other@test.com").userRole(UserRole.STUDENT).build();
        when(appUserRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        assertThrows(UnauthorizedAccessException.class, () -> 
            documentCommentService.getCommentsByDocument(100L, "other@test.com"));
    }

    @Test
    void deleteComment_ownerAdminCanDelete() {
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        
        DocumentComment comment = DocumentComment.builder().id(50L).admin(adminUser).build();
        when(documentCommentRepository.findById(50L)).thenReturn(Optional.of(comment));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        documentCommentService.deleteComment(50L, "admin@test.com");
        verify(documentCommentRepository).delete(comment);
    }
    
    @Test
    void createRejectionComment_validComment_saves() {
        documentCommentService.createRejectionComment(document, adminUser, "Rejected because...");
        verify(documentCommentRepository).save(any(DocumentComment.class));
    }

    @Test
    void createRejectionComment_emptyComment_doesNothing() {
        documentCommentService.createRejectionComment(document, adminUser, "");
        verify(documentCommentRepository, never()).save(any());
        
        documentCommentService.createRejectionComment(document, adminUser, null);
        verify(documentCommentRepository, never()).save(any());
    }
}
