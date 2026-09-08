package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.AssignTenantRequestDto;
import com.beezhub.student_accommodation.model.dto.UnassignedAdminResponseDto;
import com.beezhub.student_accommodation.service.UnassignedAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Repairs admin accounts stranded on the platform tenant (GH-212).
 *
 * <p>Platform-admin only, and deliberately outside {@code /api/admin/tenants/{tenantId}/admins}:
 * these accounts have no tenant yet, which is the whole point of them.
 */
@RestController
@RequestMapping("/api/admin/unassigned-admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Unassigned Admin Repair",
        description = "Admin accounts stranded on the platform tenant, and their reassignment")
public class UnassignedAdminController {

    private final UnassignedAdminService unassignedAdminService;

    @Operation(summary = "List admin accounts still sitting on the platform tenant")
    @ApiResponse(responseCode = "200", description = "Account list returned")
    @GetMapping
    public ResponseEntity<List<UnassignedAdminResponseDto>> listUnassignedAdmins() {
        return ResponseEntity.ok(unassignedAdminService.listUnassignedAdmins());
    }

    @Operation(summary = "Assign a stranded admin account to a property company",
            description = "Sets the account's tenant, unlocks it and revokes the tokens it already "
                    + "holds, which still carry the platform tenant claim.")
    @ApiResponse(responseCode = "200", description = "Account assigned and unlocked")
    @ApiResponse(responseCode = "404", description = "User or tenant not found")
    @ApiResponse(responseCode = "409", description = "Account already belongs to a company")
    @PostMapping("/{userId}/assign")
    public ResponseEntity<UnassignedAdminResponseDto> assignTenant(
            @PathVariable Long userId,
            @Valid @RequestBody AssignTenantRequestDto request) {
        return ResponseEntity.ok(unassignedAdminService.assignTenant(userId, request.tenantId()));
    }
}
