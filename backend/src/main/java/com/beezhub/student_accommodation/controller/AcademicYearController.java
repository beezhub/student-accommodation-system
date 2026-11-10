package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.service.AcademicYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Academic years belong to each company, per institution it serves (GH-494). A company admin
 * manages their own company's years; the current year follows the dates, so there is nothing to
 * set (GH-439).
 */
@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Years", description = "Academic year management endpoints")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List academic years", description = "The caller's company's years, newest first")
    public ResponseEntity<PageResponse<AcademicYearResponse>> getAllAcademicYears(
            @PageableDefault(size = 50, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(academicYearService.getAllAcademicYears(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get academic year by ID", description = "Returns a single academic year")
    public ResponseEntity<AcademicYearResponse> getAcademicYearById(
            @Parameter(description = "ID of the academic year", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(academicYearService.getAcademicYearById(id));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current academic year",
            description = "The year whose dates contain today, or the next one between two years, "
                    + "for the institution given or \"All institutions\"")
    public ResponseEntity<AcademicYearResponse> getCurrentAcademicYear(
            @RequestParam(required = false) Long institutionId) {
        return ResponseEntity.ok(academicYearService.getCurrentAcademicYear(institutionId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create academic year", description = "Creates a year for the caller's company")
    public ResponseEntity<AcademicYearResponse> createAcademicYear(
            @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicYearService.createAcademicYear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update academic year", description = "Updates one of the company's years")
    public ResponseEntity<AcademicYearResponse> updateAcademicYear(
            @Parameter(description = "ID of the academic year", required = true) @PathVariable Long id,
            @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.ok(academicYearService.updateAcademicYear(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete academic year", description = "Deletes a year that has no applications")
    public ResponseEntity<Void> deleteAcademicYear(
            @Parameter(description = "ID of the academic year", required = true) @PathVariable Long id) {
        academicYearService.deleteAcademicYear(id);
        return ResponseEntity.noContent().build();
    }
}
