package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.InstitutionRequest;
import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institution")
@RequiredArgsConstructor
@Tag(name = "Institution", description = "Endpoints for managing institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping("/all")
    @Operation(summary = "Get all institutions (unpaginated)", description = "Returns a list of all institutions without pagination")
    public ResponseEntity<List<InstitutionResponse>> getAllInstitutions() {
        List<InstitutionResponse> institutions = institutionService.getAllInstitutions();
        return ResponseEntity.ok(institutions);
    }

    @GetMapping
    @Operation(summary = "Get all institutions", description = "Returns a list of all registered institutions. Supports pagination with query params: page, size, sort")
    public ResponseEntity<PageResponse<InstitutionResponse>> getInstitutions(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        PageResponse<InstitutionResponse> page = institutionService.getAllInstitutions(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/search")
    @Operation(summary = "Search institutions", description = "Search and filter institutions by name, address, or contact email")
    public ResponseEntity<PageResponse<InstitutionResponse>> searchInstitutions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String contactEmail,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        PageResponse<InstitutionResponse> page = institutionService.searchInstitutions(name, address, contactEmail, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Get institution by ID", description = "Returns a single institution by its ID")
    public ResponseEntity<InstitutionResponse> getInstitutionById(@PathVariable Long id) {
        return ResponseEntity.ok(institutionService.getInstitutionById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create institution", description = "Creates a new institution. Admin only.")
    public ResponseEntity<InstitutionResponse> createInstitution(@Valid @RequestBody InstitutionRequest request) {
        InstitutionResponse response = institutionService.createInstitution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update institution", description = "Updates an existing institution. Admin only.")
    public ResponseEntity<InstitutionResponse> updateInstitution(@PathVariable Long id,
                                                                 @Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(institutionService.updateInstitution(id, request));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Delete institution", description = "Deletes an institution. Admin only.")
    public ResponseEntity<Void> deleteInstitution(@PathVariable Long id) {
        institutionService.deleteInstitution(id);
        return ResponseEntity.noContent().build();
    }
}
