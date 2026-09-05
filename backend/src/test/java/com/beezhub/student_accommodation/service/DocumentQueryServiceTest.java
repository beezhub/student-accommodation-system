package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentDownloadResponse;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.service.document.FileStorageService;
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

import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentQueryServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AuthService authService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private com.beezhub.student_accommodation.service.document.DocumentAccessGuard documentAccessGuard;

    @InjectMocks
    private DocumentQueryService documentQueryService;

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
    void getDocumentInfo_success_returnsPresignedUrl() {
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document1));
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(fileStorageService.generatePresignedUrl(anyString(), any(Duration.class))).thenReturn("http://presigned");

        var resp = documentQueryService.getDocumentInfo(1L, "user@test.com");
        assertNotNull(resp);
        assertEquals("http://presigned", resp.getFileUrl());
    }

    @Test
    void getDocumentInfo_unauthorized_throwsUnauthorizedAccessException() {
        AppUser other = AppUser.builder()
                .id(3L)
                .email("other@example.com")
                .userRole(UserRole.STUDENT)
                .build();
        when(appUserRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document1));
        doThrow(new com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException(
                "Unauthorized access to document"))
                .when(documentAccessGuard).assertCanAccessDocument(document1, other);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentQueryService.getDocumentInfo(1L, "other@example.com"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void searchDocuments_withNoFilters_returnsAllDocuments() {
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        Page<DocumentResponse> result = documentQueryService.searchDocuments(
                null, null, null, null, null, null, null, null,
                pageable, "admin@test.com"
        );

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchDocuments_withDocumentNameFilter_returnsMatchingDocuments() {
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(List.of(document1));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);

        Page<DocumentResponse> result = documentQueryService.searchDocuments(
                "passport", null, null, null, null, null, null, null,
                pageable, "admin@test.com"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("passport.pdf", result.getContent().get(0).getDocumentName());
    }

    @Test
    void searchDocuments_withStatusFilter_returnsMatchingDocuments() {
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        Page<Document> documentsPage = new PageImpl<>(List.of(document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        Page<DocumentResponse> result = documentQueryService.searchDocuments(
                null, null, null, null, "APPROVED", null, null, null,
                pageable, "admin@test.com"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMyDocuments_withNoFilters_returnsUserDocuments() {
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Page<Document> documentsPage = new PageImpl<>(Arrays.asList(document1, document2));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);
        when(documentMapper.toDocumentResponse(document2)).thenReturn(response2);

        Page<DocumentResponse> result = documentQueryService.getMyDocuments(
                null, null, null, null, null, null,
                pageable
        );

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getMyDocuments_withDocumentNameFilter_returnsMatchingDocuments() {
        when(authService.getCurrentUser()).thenReturn(regularUser);
        Page<Document> documentsPage = new PageImpl<>(List.of(document1));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);

        Page<DocumentResponse> result = documentQueryService.getMyDocuments(
                "passport", null, null, null, null, null,
                pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("passport.pdf", result.getContent().get(0).getDocumentName());
    }

    @Test
    void searchDocuments_withInvalidUser_throwsUserNotFoundException() {
        when(appUserRepository.findByEmail("invalid@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                documentQueryService.searchDocuments(
                        null, null, null, null, null, null, null, null,
                        pageable, "invalid@test.com"
                )
        );
        verify(documentRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllDocumentsByUserId_unauthorizedUser_throwsUnauthorizedAccessException() {
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        // userId mismatch and not admin (requesting for user 999)
        doThrow(new com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException(
                "Unauthorized access to documents for user ID: 999"))
                .when(documentAccessGuard).assertCanAccessDocumentsOfUser(999L, regularUser);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> documentQueryService.getAllDocumentsByUserId(999L, "user@test.com"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void downloadDocument_success_returnsResourceWithFilename() {
        InputStream stream = new ByteArrayInputStream("data".getBytes());
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document1));
        when(fileStorageService.download(document1.getDocumentPath())).thenReturn(stream);
        when(fileStorageService.getContentType(document1.getDocumentPath()))
                .thenReturn("application/octet-stream");

        var result = documentQueryService.downloadDocument(1L, "user@test.com");

        assertNotNull(result);
        assertEquals("passport.pdf", result.getFilename());
        assertEquals("application/octet-stream", result.getContentType());
        assertNotNull(result.getResource());
    }

    @Test
    void downloadDocument_documentNotFound_throwsDocumentNotFoundException() {
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class,
                () -> documentQueryService.downloadDocument(99L, "user@test.com"));
    }

    @Test
    void getAllDocumentsByUserId_ownDocuments_returnsList() {
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        // regularUser.getId() == 2L — request for own docs
        when(documentRepository.findAllByStudent_AppUser_Id(2L)).thenReturn(List.of(document1, document2));
        when(documentMapper.toDocumentResponseList(any())).thenReturn(List.of(response1, response2));

        List<DocumentResponse> result = documentQueryService.getAllDocumentsByUserId(2L, "user@test.com");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getAllDocumentsByUserId_adminQueryingAnyUser_returnsList() {
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        // adminUser.getId() == 1L, querying userId 2 — allowed because admin
        when(documentRepository.findAllByStudent_AppUser_Id(2L)).thenReturn(List.of(document1));
        when(documentMapper.toDocumentResponseList(any())).thenReturn(List.of(response1));

        List<DocumentResponse> result = documentQueryService.getAllDocumentsByUserId(2L, "admin@test.com");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchDocuments_nonAdminUser_addsUserRestriction() {
        when(appUserRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        Page<Document> documentsPage = new PageImpl<>(List.of(document1));
        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentsPage);
        when(documentMapper.toDocumentResponse(document1)).thenReturn(response1);

        Page<DocumentResponse> result = documentQueryService.searchDocuments(
                null, null, null, null, null, null, null, null,
                pageable, "user@test.com"
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        // spec.and(belongsToUser) is applied — repository was still called
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));
    }
}
