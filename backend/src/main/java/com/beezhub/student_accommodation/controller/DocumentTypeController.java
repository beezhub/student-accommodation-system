package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentTypeRequest;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/document-type")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Type", description = "Endpoints for managing document types. List endpoints return a merged view: platform defaults (tenant_id=0) plus the current tenant's overrides, with tenant rows winning on matching type names (shadow semantics). Each response includes isPlatformDefault=true for platform-level types.")
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @GetMapping
    @Operation(summary = "Get all document types", description = "Returns a list of all available document types. Supports pagination with query params: page, size, sort")
    public ResponseEntity<PageResponse<DocumentTypeResponse>> getAllDocumentTypes(
            @PageableDefault(size = 20, sort = "typeName") Pageable pageable) {
        log.info("Fetching all document types");
        PageResponse<DocumentTypeResponse> page = documentTypeService.getAllDocumentTypes(pageable);
        return ResponseEntity.ok(page);
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
    @Operation(summary = "Get required document types",
            description = "Returns required document types using the tenant's merged view (platform defaults + tenant overrides, tenant wins on same name). " +
                    "Pass ?tenantId= to get the effective required types for a specific tenant (e.g. application target tenant). " +
                    "Omitting tenantId returns platform defaults only.")
    public ResponseEntity<List<DocumentTypeResponse>> getRequiredDocumentTypes(
            @RequestParam(required = false) Long tenantId) {
        log.info("Fetching required document types for tenantId={}", tenantId);
        List<DocumentTypeResponse> requiredDocumentTypes = documentTypeService.getRequiredDocumentTypes(tenantId);
        return ResponseEntity.ok(requiredDocumentTypes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document type by ID", description = "Returns a single document type by its ID")
    public ResponseEntity<DocumentTypeResponse> getDocumentTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(documentTypeService.getDocumentTypeById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create document type",
            description = "Creates a document type scoped to the caller's tenant. TENANT_ADMIN creates a tenant-scoped type (tenant_id set from their account). PLATFORM_ADMIN creates a platform default (tenant_id=0). Use /api/platform/document-types for explicit platform management.")
    public ResponseEntity<DocumentTypeResponse> createDocumentType(@Valid @RequestBody DocumentTypeRequest request) {
        DocumentTypeResponse response = documentTypeService.createDocumentType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update document type",
            description = "Updates a document type. TENANT_ADMIN may only update types belonging to their own tenant (tenant_id != 0). Attempting to update a platform default (isPlatformDefault=true) as TENANT_ADMIN returns 403.")
    public ResponseEntity<DocumentTypeResponse> updateDocumentType(@PathVariable Long id,
                                                                   @Valid @RequestBody DocumentTypeRequest request) {
        return ResponseEntity.ok(documentTypeService.updateDocumentType(id, request));
    }

    @PatchMapping("/{id}/required")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update required flag", description = "Updates the required flag for a document type. Admin only.")
    public ResponseEntity<DocumentTypeResponse> updateRequiredFlag(@PathVariable Long id,
                                                                   @RequestParam("required") boolean required) {
        return ResponseEntity.ok(documentTypeService.updateRequiredFlag(id, required));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete document type",
            description = "Deletes a document type. TENANT_ADMIN may only delete types belonging to their own tenant. Attempting to delete a platform default (isPlatformDefault=true) as TENANT_ADMIN returns 403.")
    public ResponseEntity<Void> deleteDocumentType(@PathVariable Long id) {
        documentTypeService.deleteDocumentType(id);
        return ResponseEntity.noContent().build();
    }
}
