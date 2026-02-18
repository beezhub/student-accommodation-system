package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.DocumentDownloadResponse;
import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentQueryService documentQueryService;
    @Mock
    private DocumentLifecycleService documentLifecycleService;
    @Mock
    private DocumentStatusService documentStatusService;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void uploadDocuments_delegates() {
        List<MultipartFile> files = List.of();
        List<Long> ids = List.of();
        documentService.uploadDocuments(files, "user", ids);
        verify(documentLifecycleService).uploadDocuments(files, "user", ids);
    }

    @Test
    void getDocumentInfo_delegates() {
        documentService.getDocumentInfo(1L, "user");
        verify(documentQueryService).getDocumentInfo(1L, "user");
    }

    @Test
    void uploadSingleDocument_delegates() {
        MultipartFile file = mock(MultipartFile.class);
        documentService.uploadSingleDocument(file, "user", 1L);
        verify(documentLifecycleService).uploadSingleDocument(file, "user", 1L);
    }

    @Test
    void downloadDocument_delegates() {
        documentService.downloadDocument(1L, "user");
        verify(documentQueryService).downloadDocument(1L, "user");
    }

    @Test
    void getAllDocumentsByUserId_delegates() {
        documentService.getAllDocumentsByUserId(1L, "user");
        verify(documentQueryService).getAllDocumentsByUserId(1L, "user");
    }

    @Test
    void updateDocumentStatus_delegates() {
        documentService.updateDocumentStatus(1L, "status", "user");
        verify(documentStatusService).updateDocumentStatus(1L, "status", null, "user");
    }

    @Test
    void updateDocumentStatusWithComment_delegates() {
        documentService.updateDocumentStatus(1L, "status", "comment", "user");
        verify(documentStatusService).updateDocumentStatus(1L, "status", "comment", "user");
    }

    @Test
    void deleteDocument_delegates() {
        documentService.deleteDocument(1L, "user");
        verify(documentLifecycleService).deleteDocument(1L, "user");
    }

    @Test
    void searchDocuments_delegates() {
        documentService.searchDocuments(null, null, null, null, null, null, null, null, Pageable.unpaged(), "user");
        verify(documentQueryService).searchDocuments(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getMyDocuments_delegates() {
        documentService.getMyDocuments(null, null, null, null, null, null, Pageable.unpaged());
        verify(documentQueryService).getMyDocuments(any(), any(), any(), any(), any(), any(), any());
    }
}
