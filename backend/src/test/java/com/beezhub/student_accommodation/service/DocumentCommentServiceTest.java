package com.beezhub.student_accommodation.service;

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
import org.mockito.ArgumentCaptor;
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
    DocumentCommentRepository documentCommentRepository;
    @Mock
    DocumentRepository documentRepository;
    @Mock
    AppUserRepository appUserRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    DocumentCommentService documentCommentService;

    private AppUser admin;
    private AppUser studentUser;
    private Document document;

    @BeforeEach
    void setUp() {
        admin = AppUser.builder()
                .id(1L)
                .email("admin@example.com")
                .userRole(UserRole.ADMIN)
                .firstName("Admin")
                .lastName("User")
                .build();

        studentUser = AppUser.builder()
                .id(2L)
                .email("student@example.com")
                .userRole(UserRole.STUDENT)
                .build();

        Student student = new Student();
        student.setAppUser(studentUser);

        document = new Document();
        document.setId(100L);
        document.setStudent(student);
    }

    @Test
    void addComment_asAdmin_success() {
        when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        
        DocumentComment savedComment = DocumentComment.builder()
                .id(10L)
                .document(document)
                .admin(admin)
                .commentText("Test comment")
                .build();
        when(documentCommentRepository.save(any(DocumentComment.class))).thenReturn(savedComment);

        DocumentCommentResponse response = documentCommentService.addComment(100L, "admin@example.com", "Test comment");

        assertNotNull(response);
        assertEquals("Test comment", response.getCommentText());
        verify(eventPublisher).publishEvent(any(DocumentCommentEvent.class));
    }

    @Test
    void addComment_notAdmin_throws() {
        AppUser regularUser = AppUser.builder().id(3L).userRole(UserRole.STUDENT).email("regular@example.com").build();
        when(appUserRepository.findByEmail("regular@example.com")).thenReturn(Optional.of(regularUser));
        when(appUserRepository.findById(3L)).thenReturn(Optional.of(regularUser));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));

        assertThrows(UnauthorizedAccessException.class, () -> 
            documentCommentService.addComment(100L, "regular@example.com", "text"));
    }

    @Test
    void getComments_asOwner_success() {
        when(appUserRepository.findByEmail("student@example.com")).thenReturn(Optional.of(studentUser));
        when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        
        when(documentCommentRepository.findByDocument_IdOrderByCreatedAtDesc(100L)).thenReturn(List.of());

        List<DocumentCommentResponse> comments = documentCommentService.getCommentsByDocument(100L, "student@example.com");
        assertNotNull(comments);
    }

    @Test
    void deleteComment_asAdmin_success() {
        DocumentComment comment = DocumentComment.builder().id(10L).document(document).admin(admin).build();
        when(appUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(documentCommentRepository.findById(10L)).thenReturn(Optional.of(comment));

        documentCommentService.deleteComment(10L, "admin@example.com");
        verify(documentCommentRepository).delete(comment);
    }
}
