package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentTypeRequest;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/document-types")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Platform Document Types", description = "PLATFORM_ADMIN endpoints for managing platform-level (tenant_id=0) document types")
public class PlatformDocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create platform document type", description = "Creates a document type at tenant_id=0 (platform default). PLATFORM_ADMIN only.")
    public ResponseEntity<DocumentTypeResponse> createDocumentType(@Valid @RequestBody DocumentTypeRequest request) {
        DocumentTypeResponse response = documentTypeService.createDocumentType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update platform document type", description = "Updates a platform-level document type. PLATFORM_ADMIN only.")
    public ResponseEntity<DocumentTypeResponse> updateDocumentType(@PathVariable Long id,
                                                                   @Valid @RequestBody DocumentTypeRequest request) {
        return ResponseEntity.ok(documentTypeService.updateDocumentType(id, request));
    }

    @PatchMapping("/{id}/required")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update required flag on platform document type", description = "PLATFORM_ADMIN only.")
    public ResponseEntity<DocumentTypeResponse> updateRequiredFlag(@PathVariable Long id,
                                                                   @RequestParam("required") boolean required) {
        return ResponseEntity.ok(documentTypeService.updateRequiredFlag(id, required));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Delete platform document type", description = "Deletes a platform-level document type. PLATFORM_ADMIN only.")
    public ResponseEntity<Void> deleteDocumentType(@PathVariable Long id) {
        documentTypeService.deleteDocumentType(id);
        return ResponseEntity.noContent().build();
    }
}
