package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentTypeRequest;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/document-type")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Type", description = "Endpoints for managing document types")
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @GetMapping
    @Operation(summary = "Get all document types", description = "Returns a list of all available document types. Supports pagination with query params: page, size, sort")
    public ResponseEntity<?> getAllDocumentTypes(
            @RequestParam(required = false) Boolean paginated,
            @PageableDefault(size = 20, sort = "typeName") Pageable pageable) {
        log.info("Fetching all document types");
        if (Boolean.TRUE.equals(paginated)) {
            PageResponse<DocumentTypeResponse> page = documentTypeService.getAllDocumentTypes(pageable);
            return ResponseEntity.ok(page);
        }
        List<DocumentTypeResponse> allDocumentTypes = documentTypeService.getAllDocumentTypes();
        return ResponseEntity.ok(allDocumentTypes);
    }

    @GetMapping("/search")
    @Operation(summary = "Search document types", description = "Search and filter document types by name or required flag")
    public ResponseEntity<PageResponse<DocumentTypeResponse>> searchDocumentTypes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isRequired,
            @PageableDefault(size = 20, sort = "typeName") Pageable pageable) {
        log.info("Searching document types with filters - name: {}, isRequired: {}", name, isRequired);
        PageResponse<DocumentTypeResponse> page = documentTypeService.searchDocumentTypes(name, isRequired, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/required")
    @Operation(summary = "Get required document types", description = "Returns a list of required document types for student registration")
    public ResponseEntity<List<DocumentTypeResponse>> getRequiredDocumentTypes() {
        log.info("Fetching required document types");
        List<DocumentTypeResponse> requiredDocumentTypes = documentTypeService.getRequiredDocumentTypes();
        return ResponseEntity.ok(requiredDocumentTypes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document type by ID", description = "Returns a single document type by its ID")
    public ResponseEntity<DocumentTypeResponse> getDocumentTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(documentTypeService.getDocumentTypeById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create document type", description = "Creates a new document type. Admin only.")
    public ResponseEntity<DocumentTypeResponse> createDocumentType(@Valid @RequestBody DocumentTypeRequest request) {
        DocumentTypeResponse response = documentTypeService.createDocumentType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update document type", description = "Updates an existing document type. Admin only.")
    public ResponseEntity<DocumentTypeResponse> updateDocumentType(@PathVariable Long id,
                                                                   @Valid @RequestBody DocumentTypeRequest request) {
        return ResponseEntity.ok(documentTypeService.updateDocumentType(id, request));
    }

    @PatchMapping("/{id}/required")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update required flag", description = "Updates the required flag for a document type. Admin only.")
    public ResponseEntity<DocumentTypeResponse> updateRequiredFlag(@PathVariable Long id,
                                                                   @RequestParam("required") boolean required) {
        return ResponseEntity.ok(documentTypeService.updateRequiredFlag(id, required));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete document type", description = "Deletes a document type. Admin only.")
    public ResponseEntity<Void> deleteDocumentType(@PathVariable Long id) {
        documentTypeService.deleteDocumentType(id);
        return ResponseEntity.noContent().build();
    }
}
