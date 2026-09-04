package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.AccommodationTypeRequest;
import com.beezhub.student_accommodation.model.dto.AccommodationTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.service.AccommodationTypeService;
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
@RequestMapping("/api/accommodation-types")
@RequiredArgsConstructor
@Tag(name = "Accommodation Types", description = "Endpoints for managing accommodation types")
public class AccommodationTypeController {

    private final AccommodationTypeService accommodationTypeService;

    @GetMapping("/active")
    @Operation(summary = "Get active accommodation types", description = "Returns all active accommodation types for student forms")
    public ResponseEntity<List<AccommodationTypeResponse>> getActiveTypes() {
        return ResponseEntity.ok(accommodationTypeService.getAllActive());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get all accommodation types", description = "Returns paginated list of all accommodation types. Admin only.")
    public ResponseEntity<PageResponse<AccommodationTypeResponse>> getAllTypes(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(accommodationTypeService.getAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get accommodation type by ID", description = "Returns a single accommodation type. Admin only.")
    public ResponseEntity<AccommodationTypeResponse> getTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(accommodationTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create accommodation type", description = "Creates a new accommodation type. Admin only.")
    public ResponseEntity<AccommodationTypeResponse> createType(@Valid @RequestBody AccommodationTypeRequest request) {
        AccommodationTypeResponse response = accommodationTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update accommodation type", description = "Updates an existing accommodation type. Admin only.")
    public ResponseEntity<AccommodationTypeResponse> updateType(
            @PathVariable Long id,
            @Valid @RequestBody AccommodationTypeRequest request) {
        return ResponseEntity.ok(accommodationTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete accommodation type", description = "Deletes an accommodation type. Admin only.")
    public ResponseEntity<Void> deleteType(@PathVariable Long id) {
        accommodationTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
