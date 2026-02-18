package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.DocumentDownloadResponse;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentQueryService documentQueryService;
    private final DocumentLifecycleService documentLifecycleService;
    private final DocumentStatusService documentStatusService;

    public List<DocumentResponse> uploadDocuments(List<MultipartFile> files, String email, List<Long> documentTypeIds) {
        return documentLifecycleService.uploadDocuments(files, email, documentTypeIds);
    }

    public DocumentResponse getDocumentInfo(Long documentId, String username) {
        return documentQueryService.getDocumentInfo(documentId, username);
    }

    public void uploadSingleDocument(MultipartFile file, String username, Long documentTypeId) {
        documentLifecycleService.uploadSingleDocument(file, username, documentTypeId);
    }

    public DocumentDownloadResponse downloadDocument(Long documentId, String username) {
        return documentQueryService.downloadDocument(documentId, username);
    }

    public List<DocumentResponse> getAllDocumentsByUserId(Long userId, String username) {
        return documentQueryService.getAllDocumentsByUserId(userId, username);
    }

    public DocumentResponse updateDocumentStatus(Long documentId, String status, String username) {
        return documentStatusService.updateDocumentStatus(documentId, status, null, username);
    }

    public DocumentResponse updateDocumentStatus(Long documentId, String status, String comment, String username) {
        return documentStatusService.updateDocumentStatus(documentId, status, comment, username);
    }

    public void deleteDocument(Long documentId, String username) {
        documentLifecycleService.deleteDocument(documentId, username);
    }

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
        return documentQueryService.searchDocuments(
                documentName, studentId, studentName, documentTypeId, status, applicationStatus, createdAfter,
                createdBefore, pageable, username);
    }

    public Page<DocumentResponse> getMyDocuments(
            String documentName,
            Long documentTypeId,
            String status,
            String applicationStatus,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable) {
        return documentQueryService.getMyDocuments(
                documentName, documentTypeId, status, applicationStatus, createdAfter,
                createdBefore, pageable);
    }
}
