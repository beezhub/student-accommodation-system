package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentNotFoundException;
import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentMapper;
import com.beezhub.student_accommodation.model.dto.DocumentDownloadResponse;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import com.beezhub.student_accommodation.specification.DocumentSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentQueryService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final AppUserRepository appUserRepository;
    private final AuthService authService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentInfo(Long documentId, String username) {
        var user = getUserByEmail(username);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        assertDocumentAccess(document, user);

        String objectPath = document.getDocumentPath();
        var documentResponse = documentMapper.toDocumentResponse(document);
        // Generate presigned URL valid for 1 hour
        documentResponse.setFileUrl(fileStorageService.generatePresignedUrl(objectPath, Duration.ofHours(1)));
        return documentResponse;
    }

    @Transactional(readOnly = true)
    public DocumentDownloadResponse downloadDocument(Long documentId, String username) {
        var user = getUserByEmail(username);
        var document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));

        assertDocumentAccess(document, user);

        String objectPath = document.getDocumentPath();

        InputStream response = fileStorageService.download(objectPath);
        var inputStreamResource = new InputStreamResource(response);
        return DocumentDownloadResponse.builder()
                .filename(document.getDocumentName())
                .resource(inputStreamResource)
                .contentType("application/octet-stream")
                .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocumentsByUserId(Long userId, String username) {
        var user = getUserByEmail(username);
        if (!user.getId().equals(userId) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to documents for user ID: " + userId);
        }

        List<Document> documents = documentRepository.findAllByStudent_AppUser_Id(userId);
        return documentMapper.toDocumentResponseList(documents);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocuments(
            String documentName,
            Long studentId,
            String studentName,
            Long documentTypeId,
            String status,
            String applicationStatus,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable,
            String username) {
        var user = getUserByEmail(username);
        Specification<Document> spec = DocumentSpecification.filterBy(
                documentName, studentId, studentName, documentTypeId, status, applicationStatus, createdAfter,
                createdBefore);

        // If not admin, restrict to user's own documents
        if (!user.getUserRole().isAdmin()) {
            spec = spec.and(DocumentSpecification.belongsToUser(user.getId()));
        }

        Page<Document> documentsPage = documentRepository.findAll(spec, pageable);
        return documentsPage.map(documentMapper::toDocumentResponse);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getMyDocuments(
            String documentName,
            Long documentTypeId,
            String status,
            String applicationStatus,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable) {
        var currentUser = authService.getCurrentUser();
        Specification<Document> spec = DocumentSpecification.belongsToUser(currentUser.getId())
                .and(DocumentSpecification.filterBy(
                        documentName, null, null, documentTypeId, status, applicationStatus, createdAfter,
                        createdBefore));

        Page<Document> documentsPage = documentRepository.findAll(spec, pageable);
        return documentsPage.map(documentMapper::toDocumentResponse);
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Student not found"));
    }

    private void assertDocumentAccess(Document document, AppUser user) {
        if (!document.getStudent().getAppUser().equals(user) && !user.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to document");
        }
    }
}
