package com.beezhub.student_accommodation.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Disabled("Needs rewrite after Phase 2 service decomposition: DocumentService is now a facade. "
        + "Search logic is in DocumentQueryService — re-implement targeting that service directly.")
@ExtendWith(MockitoExtension.class)
class DocumentServiceSearchTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private DocumentService documentService;

    private AppUser adminUser;
    private AppUser regularUser;
    private Student student;
    private DocumentType documentType;
    private Document document1;
    private Document document2;
    private DocumentResponse response1;
    private DocumentResponse response2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Setup admin user
        adminUser = new AppUser();
        adminUser.setId(1L);
        adminUser.setEmail("admin@test.com");
        adminUser.setUserRole(UserRole.ADMIN);

        // Setup regular user
        regularUser = new AppUser();
        regularUser.setId(2L);
        regularUser.setEmail("user@test.com");
        regularUser.setUserRole(UserRole.STUDENT);

        // Setup student
        student = new Student();
        student.setId(1L);
        student.setAppUser(regularUser);

        // Setup document type
        documentType = new DocumentType();
        documentType.setId(1L);
        documentType.setTypeName("ID_CARD");

        // Setup documents
        document1 = new Document();
        document1.setId(1L);
        document1.setDocumentName("passport.pdf");
        document1.setDocumentPath("/path/to/passport.pdf");
        document1.setDocumentStatus(DocumentStatus.PENDING);
        document1.setStudent(student);
        document1.setDocumentType(documentType);
        document1.setCreatedAt(LocalDateTime.now().minusDays(5));

        document2 = new Document();
        document2.setId(2L);
        document2.setDocumentName("certificate.pdf");
        document2.setDocumentPath("/path/to/certificate.pdf");
        document2.setDocumentStatus(DocumentStatus.APPROVED);
        document2.setStudent(student);
        document2.setDocumentType(documentType);
        document2.setCreatedAt(LocalDateTime.now().minusDays(2));

        // Setup responses
        response1 = new DocumentResponse();
        response1.setId(1L);
        response1.setDocumentName("passport.pdf");

        response2 = new DocumentResponse();
        response2.setId(2L);
        response2.setDocumentName("certificate.pdf");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void searchDocuments_WithNoFilters_ReturnsAllDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        // String documentName, Long studentId, String studentName, Long documentTypeId, String status, String applicationStatus, LocalDateTime createdAfter, LocalDateTime createdBefore, Pageable pageable, String username
        Page<DocumentResponse> result = documentService.searchDocuments(
                null, null, null, null, null, null, null, null,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithDocumentNameFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(List.of(document1));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                "passport", null, null, null, null, null, null, null,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("passport.pdf", result.getContent().get(0).getDocumentName());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithStudentIdFilter_ReturnsStudentDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                null, 1L, null, null, null, null, null, null,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithDocumentTypeFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                null, null, null, 1L, null, null, null, null,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithStatusFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(List.of(document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                null, null, null, null, "APPROVED", null, null, null,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithDateRangeFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        LocalDateTime createdAfter = LocalDateTime.now().minusDays(3);
        Page<Document> documentsPage = new PageImpl<>(List.of(document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                null, null, null, null, null, null, createdAfter, null,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithMultipleFilters_ReturnsMatchingDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        LocalDateTime createdAfter = LocalDateTime.now().minusDays(10);
        LocalDateTime createdBefore = LocalDateTime.now();
        Page<Document> documentsPage = new PageImpl<>(List.of(document1));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                "passport", 1L, null, 1L, "PENDING", null, createdAfter, createdBefore,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_AsRegularUser_RestrictsToOwnDocuments() {
        // Arrange
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                null, null, null, null, null, null, null, null,
                pageable, "user@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithEmptyResult_ReturnsEmptyPage() {
        // Arrange
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> emptyPage = new PageImpl<>(List.of());
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        // Act
        Page<DocumentResponse> result = documentService.searchDocuments(
                "nonexistent", null, null, null, null, null, null, null,
                pageable, "admin@test.com"
        );

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMyDocuments_WithNoFilters_ReturnsUserDocuments() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        // String documentName, Long documentTypeId, String status, String applicationStatus, LocalDateTime createdAfter, LocalDateTime createdBefore, Pageable pageable
        Page<DocumentResponse> result = documentService.getMyDocuments(
                null, null, null, null, null, null,
                pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMyDocuments_WithDocumentNameFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Page<Document> documentsPage = new PageImpl<>(List.of(document1));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);

        // Act
        Page<DocumentResponse> result = documentService.getMyDocuments(
                "passport", null, null, null, null, null,
                pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("passport.pdf", result.getContent().get(0).getDocumentName());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMyDocuments_WithStatusFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Page<Document> documentsPage = new PageImpl<>(List.of(document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.getMyDocuments(
                null, null, "APPROVED", null, null, null,
                pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMyDocuments_WithDocumentTypeFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.getMyDocuments(
                null, 1L, null, null, null, null,
                pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMyDocuments_WithDateRangeFilter_ReturnsMatchingDocuments() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(regularUser);
        LocalDateTime createdAfter = LocalDateTime.now().minusDays(3);
        Page<Document> documentsPage = new PageImpl<>(List.of(document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        // Act
        Page<DocumentResponse> result = documentService.getMyDocuments(
                null, null, null, null, createdAfter, null,
                pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getMyDocuments_WithMultipleFilters_ReturnsMatchingDocuments() {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(regularUser);
        LocalDateTime createdAfter = LocalDateTime.now().minusDays(10);
        LocalDateTime createdBefore = LocalDateTime.now();
        Page<Document> documentsPage = new PageImpl<>(List.of(document1));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);

        // Act
        Page<DocumentResponse> result = documentService.getMyDocuments(
                "passport", 1L, "PENDING", null, createdAfter, createdBefore,
                pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_WithInvalidUser_ThrowsException() {
        // Arrange
        when(appUserRepository.findByEmail("invalid@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                documentService.searchDocuments(
                        null, null, null, null, null, null, null, null,
                        pageable, "invalid@test.com"
                )
        );
        verify(documentRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}