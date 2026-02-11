package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.service.AcademicYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Years", description = "Academic year management endpoints")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all academic years", description = "Returns a list of all academic years")
    public ResponseEntity<List<AcademicYearResponse>> getAllAcademicYears() {
        return ResponseEntity.ok(academicYearService.getAllAcademicYears());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get academic year by ID", description = "Returns a single academic year")
    public ResponseEntity<AcademicYearResponse> getAcademicYearById(
            @Parameter(description = "ID of the academic year", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(academicYearService.getAcademicYearById(id));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current academic year", description = "Returns the currently active academic year")
    public ResponseEntity<AcademicYearResponse> getCurrentAcademicYear() {
        return ResponseEntity.ok(academicYearService.getCurrentAcademicYear());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create academic year", description = "Creates a new academic year")
    public ResponseEntity<AcademicYearResponse> createAcademicYear(
            @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicYearService.createAcademicYear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update academic year", description = "Updates an existing academic year")
    public ResponseEntity<AcademicYearResponse> updateAcademicYear(
            @Parameter(description = "ID of the academic year", required = true) @PathVariable Long id,
            @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.ok(academicYearService.updateAcademicYear(id, request));
    }

    @PostMapping("/{id}/set-current")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set academic year as current", description = "Marks an academic year as the current active year")
    public ResponseEntity<AcademicYearResponse> setCurrentAcademicYear(
            @Parameter(description = "ID of the academic year", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(academicYearService.setCurrentAcademicYear(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete academic year", description = "Deletes an academic year if no applications are linked")
    public ResponseEntity<Void> deleteAcademicYear(
            @Parameter(description = "ID of the academic year", required = true) @PathVariable Long id) {
        academicYearService.deleteAcademicYear(id);
        return ResponseEntity.noContent().build();
    }
}
