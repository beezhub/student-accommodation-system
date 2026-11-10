package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.AcademicYearCopyRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearPatternRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.dto.IntakeRequest;
import com.beezhub.student_accommodation.model.dto.IntakeResponse;
import com.beezhub.student_accommodation.service.IntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Intakes inside a company's academic years (GH-494): create a year from a pattern, copy last
 * year, and create, edit, delete or close intakes. Every load by id goes through
 * {@code TenantOwnershipGuard}, so another company's year or intake reads as not found.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
@Tag(name = "Intakes", description = "Academic year patterns and intakes")
public class IntakeController {

    private final IntakeService intakeService;

    @PostMapping("/api/academic-years/from-pattern")
    @Operation(summary = "Create a year from a pattern",
            description = "Creates a year with Annual, Semester or Trimester intakes and their windows filled in")
    public ResponseEntity<AcademicYearResponse> createFromPattern(
            @Valid @RequestBody AcademicYearPatternRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(intakeService.createFromPattern(request));
    }

    @PostMapping("/api/academic-years/{id}/copy")
    @Operation(summary = "Copy a year", description = "Copies a year and its intakes with every date a year later")
    public ResponseEntity<AcademicYearResponse> copyYear(
            @PathVariable Long id, @Valid @RequestBody(required = false) AcademicYearCopyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(intakeService.copyYear(id, request));
    }

    @PostMapping("/api/academic-years/{id}/intakes")
    @Operation(summary = "Add an intake", description = "Adds an intake inside a year")
    public ResponseEntity<IntakeResponse> createIntake(
            @PathVariable Long id, @Valid @RequestBody IntakeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(intakeService.createIntake(id, request));
    }

    @PutMapping("/api/intakes/{id}")
    @Operation(summary = "Edit an intake",
            description = "Returns how many applications and allocations it has; they keep their intake and dates")
    public ResponseEntity<IntakeResponse> updateIntake(
            @PathVariable Long id, @Valid @RequestBody IntakeRequest request) {
        return ResponseEntity.ok(intakeService.updateIntake(id, request));
    }

    @DeleteMapping("/api/intakes/{id}")
    @Operation(summary = "Delete an intake", description = "Refused when it has applications")
    public ResponseEntity<Void> deleteIntake(@PathVariable Long id) {
        intakeService.deleteIntake(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/intakes/{id}/close-applications")
    @Operation(summary = "Close applications now", description = "Sets the intake's close date to today")
    public ResponseEntity<IntakeResponse> closeApplicationsNow(@PathVariable Long id) {
        return ResponseEntity.ok(intakeService.closeApplicationsNow(id));
    }
}
