package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.beezhub.student_accommodation.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "Document upload and management APIs")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload multiple documents", description = "Allows authenticated users to upload multiple documents")
    @ApiResponse(responseCode = "200", description = "Documents uploaded successfully")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/upload-multiple",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json"
    )
    public ResponseEntity<List<DocumentResponse>> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("documentTypes") List<Long> documentType,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.uploadDocuments(files, authentication.getName(), documentType));
    }

    @Operation(summary = "Upload single document", description = "Allows authenticated users to upload a single document")
    @ApiResponse(responseCode = "200", description = "Document uploaded successfully")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/upload-single",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json"
    )
    public ResponseEntity<String> uploadSingleDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") Long documentTypeId,
            Authentication authentication
    ) {
        documentService.uploadSingleDocument(file, authentication.getName(), documentTypeId);
        return ResponseEntity.ok("File uploaded successfully");
    }

    @Operation(summary = "Get document info (with presigned URL)", description = "Get document details with presigned download URL")
    @ApiResponse(responseCode = "200", description = "Document info returned")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info/{documentId}")
    public ResponseEntity<DocumentResponse> getDocumentUrl(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok().body(documentService.getDocumentInfo(documentId, authentication.getName()));
    }

    @Operation(summary = "Download document", description = "Download document file (owner or admin only)")
    @ApiResponse(responseCode = "200", description = "Document stream returned")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        var documentDownloadResponse = documentService.downloadDocument(documentId, authentication.getName());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + documentDownloadResponse.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(documentDownloadResponse.getContentType()))
                .body(documentDownloadResponse.getResource());
    }

    @Operation(summary = "Get all documents for a user (admin or owner)", description = "Retrieve all documents for a specific user")
    @ApiResponse(responseCode = "200", description = "Document list returned")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info/all/{userId}")
    public ResponseEntity<List<DocumentResponse>> getAllDocumentsByUserId(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.getAllDocumentsByUserId(userId, authentication.getName()));
    }

    @Operation(
            summary = "Get my documents with pagination and filtering",
            description = "Retrieve current user's documents with optional filters and pagination"
    )
    @ApiResponse(responseCode = "200", description = "Documents for current user returned")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<PageResponse<DocumentResponse>> getMyDocuments(
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false) Long documentTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponse> documentsPage = documentService.getMyDocuments(
                documentName, documentTypeId, status, createdAfter, createdBefore,
                pageable
        );

        PageResponse<DocumentResponse> response = PageResponse.<DocumentResponse>builder()
                .content(documentsPage.getContent())
                .pageNumber(documentsPage.getNumber())
                .pageSize(documentsPage.getSize())
                .totalElements(documentsPage.getTotalElements())
                .totalPages(documentsPage.getTotalPages())
                .first(documentsPage.isFirst())
                .last(documentsPage.isLast())
                .empty(documentsPage.isEmpty())
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update document status (admin)", description = "Admin-only endpoint to update document verification status")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update-status/{documentId}")
    public ResponseEntity<DocumentResponse> updateDocumentStatus(
            @PathVariable Long documentId,
            @RequestParam String status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(documentService.updateDocumentStatus(documentId, status, authentication.getName()));
    }

    @Operation(summary = "Delete document", description = "Delete a document (owner or admin only)")
    @ApiResponse(responseCode = "204", description = "Document deleted")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/delete/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication
    ) {
        documentService.deleteDocument(documentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Search documents with pagination and filters (admin)",
            description = "Admin endpoint to search all documents with filters: name, studentId, documentTypeId, status, date range"
    )
    @ApiResponse(responseCode = "200", description = "Search results returned")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<DocumentResponse>> searchDocuments(
            @RequestParam(required = false) String documentName,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long documentTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            Authentication authentication
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponse> documentsPage = documentService.searchDocuments(
                documentName, studentId, documentTypeId, status, createdAfter, createdBefore,
                pageable, authentication.getName()
        );

        PageResponse<DocumentResponse> response = PageResponse.<DocumentResponse>builder()
                .content(documentsPage.getContent())
                .pageNumber(documentsPage.getNumber())
                .pageSize(documentsPage.getSize())
                .totalElements(documentsPage.getTotalElements())
                .totalPages(documentsPage.getTotalPages())
                .first(documentsPage.isFirst())
                .last(documentsPage.isLast())
                .empty(documentsPage.isEmpty())
                .build();

        return ResponseEntity.ok(response);
    }
}
