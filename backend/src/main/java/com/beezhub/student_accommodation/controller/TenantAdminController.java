package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.TenantAdminRequestDto;
import com.beezhub.student_accommodation.model.dto.TenantAdminResponseDto;
import com.beezhub.student_accommodation.service.TenantAdminService;
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
@RequestMapping("/api/admin/tenants/{tenantId}/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Tenant Admin Management", description = "Platform admin endpoints for managing tenant administrators")
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @Operation(summary = "List admin users for a tenant")
    @ApiResponse(responseCode = "200", description = "Tenant admin list returned")
    @ApiResponse(responseCode = "404", description = "Tenant not found")
    @GetMapping
    public ResponseEntity<List<TenantAdminResponseDto>> listTenantAdmins(@PathVariable Long tenantId) {
        return ResponseEntity.ok(tenantAdminService.listTenantAdmins(tenantId));
    }

    @Operation(summary = "Create a tenant admin user")
    @ApiResponse(responseCode = "201", description = "Tenant admin created")
    @ApiResponse(responseCode = "404", description = "Tenant not found")
    @ApiResponse(responseCode = "409", description = "Email already in use or tenant is inactive")
    @PostMapping
    public ResponseEntity<TenantAdminResponseDto> createTenantAdmin(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantAdminRequestDto request) {
        return ResponseEntity.status(201).body(tenantAdminService.createTenantAdmin(tenantId, request));
    }

    @Operation(summary = "Deactivate a tenant admin user")
    @ApiResponse(responseCode = "200", description = "Tenant admin deactivated")
    @ApiResponse(responseCode = "404", description = "Tenant or user not found")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivateTenantAdmin(
            @PathVariable Long tenantId,
            @PathVariable Long userId) {
        tenantAdminService.deactivateTenantAdmin(tenantId, userId);
        return ResponseEntity.ok().build();
    }
}
