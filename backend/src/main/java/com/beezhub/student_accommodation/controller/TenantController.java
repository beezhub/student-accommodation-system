package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.TenantRequestDto;
import com.beezhub.student_accommodation.model.dto.TenantResponseDto;
import com.beezhub.student_accommodation.model.dto.TenantStatusDto;
import com.beezhub.student_accommodation.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Tenant Management", description = "Platform admin tenant management endpoints")
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "List tenants",
            description = "Every tenant by default, so the administration screen can still show "
                    + "and reactivate deactivated ones. Pass activeOnly=true for the list a "
                    + "dropdown should offer when assigning something to a company.")
    @ApiResponse(responseCode = "200", description = "Tenant list returned")
    @GetMapping
    public ResponseEntity<List<TenantResponseDto>> getAllTenants(
            // Defaults to false so every existing caller keeps the list it had (GH-267).
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(tenantService.getAllTenants(activeOnly));
    }

    @Operation(summary = "Get tenant by ID")
    @ApiResponse(responseCode = "200", description = "Tenant found")
    @ApiResponse(responseCode = "404", description = "Tenant not found")
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponseDto> getTenantById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @Operation(summary = "Create a new tenant")
    @ApiResponse(responseCode = "201", description = "Tenant created")
    @ApiResponse(responseCode = "409", description = "Tenant name already exists")
    @PostMapping
    public ResponseEntity<TenantResponseDto> createTenant(@Valid @RequestBody TenantRequestDto request) {
        TenantResponseDto created = tenantService.createTenant(request);
        return ResponseEntity.status(201).body(created);
    }

    @Operation(summary = "Update an existing tenant")
    @ApiResponse(responseCode = "200", description = "Tenant updated")
    @ApiResponse(responseCode = "400", description = "Platform tenant cannot be modified")
    @ApiResponse(responseCode = "404", description = "Tenant not found")
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponseDto> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody TenantRequestDto request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @Operation(summary = "Toggle tenant active status")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "400", description = "Platform tenant cannot be deactivated")
    @ApiResponse(responseCode = "404", description = "Tenant not found")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TenantResponseDto> toggleTenantStatus(
            @PathVariable Long id,
            @Valid @RequestBody TenantStatusDto statusDto) {
        return ResponseEntity.ok(tenantService.toggleTenantStatus(id, statusDto));
    }
}
