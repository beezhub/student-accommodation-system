package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/document-type")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Type", description = "Endpoints for managing document types")
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @GetMapping
    @Operation(summary = "Get all document types", description = "Returns a list of all available document types")
    public ResponseEntity<List<DocumentTypeResponse>> getAllDocumentTypes() {
        log.info("Fetching all document types");
        List<DocumentTypeResponse> allDocumentTypes = documentTypeService.getAllDocumentTypes();
        return ResponseEntity.ok(allDocumentTypes);
    }

    @GetMapping("/required")
    @Operation(summary = "Get required document types", description = "Returns a list of required document types for student registration")
    public ResponseEntity<List<DocumentTypeResponse>> getRequiredDocumentTypes() {
        log.info("Fetching required document types");
        List<DocumentTypeResponse> requiredDocumentTypes = documentTypeService.getRequiredDocumentTypes();
        return ResponseEntity.ok(requiredDocumentTypes);
    }

}
