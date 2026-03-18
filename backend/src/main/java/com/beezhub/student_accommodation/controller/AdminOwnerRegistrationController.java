package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.OwnerRegistrationSummaryDto;
import com.beezhub.student_accommodation.model.dto.RejectOwnerRegistrationRequest;
import com.beezhub.student_accommodation.service.OwnerRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/owner-registrations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin — Owner Registrations", description = "Admin review of building owner registration requests")
public class AdminOwnerRegistrationController {

    private final OwnerRegistrationService ownerRegistrationService;

    @Operation(summary = "List all pending owner registration requests")
    @GetMapping
    public List<OwnerRegistrationSummaryDto> getPendingRegistrations() {
        return ownerRegistrationService.getPendingRegistrations();
    }

    @Operation(summary = "Approve a building owner registration")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        ownerRegistrationService.approve(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reject a building owner registration with optional reason")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @RequestBody(required = false) RejectOwnerRegistrationRequest request) {
        String reason = request != null ? request.getReason() : null;
        ownerRegistrationService.reject(id, reason);
        return ResponseEntity.ok().build();
    }
}
