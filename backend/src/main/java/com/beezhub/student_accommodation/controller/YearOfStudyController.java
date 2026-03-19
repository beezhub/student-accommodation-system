package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.YearOfStudyRequest;
import com.beezhub.student_accommodation.model.dto.YearOfStudyResponse;
import com.beezhub.student_accommodation.service.YearOfStudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/year-of-study")
@RequiredArgsConstructor
@Tag(name = "Year Of Study", description = "Endpoints for managing years of study")
public class YearOfStudyController {
    private final YearOfStudyService yearOfStudyService;

    @GetMapping
    @Operation(summary = "Get all years of study", description = "Returns a list of all configured years of study. Supports pagination with query params: page, size, sort")
    public ResponseEntity<PageResponse<YearOfStudyResponse>> getAllYearsOfStudy(
            @PageableDefault(size = 20, sort = "yearOfStudy") Pageable pageable) {
            PageResponse<YearOfStudyResponse> page = yearOfStudyService.getAllYearsOfStudy(pageable);
            return ResponseEntity.ok(page);
    }

    @GetMapping("/search")
    @Operation(summary = "Search years of study", description = "Search and filter years of study by year value")
    public ResponseEntity<PageResponse<YearOfStudyResponse>> searchYearsOfStudy(
            @RequestParam(required = false) Integer year,
            @PageableDefault(size = 20, sort = "yearOfStudy") Pageable pageable) {
        PageResponse<YearOfStudyResponse> page = yearOfStudyService.searchYearsOfStudy(year, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get year of study by ID", description = "Returns a single year of study by its ID")
    public ResponseEntity<YearOfStudyResponse> getYearOfStudyById(@PathVariable Long id) {
        return ResponseEntity.ok(yearOfStudyService.getYearOfStudyById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create year of study", description = "Creates a new year of study. Admin only.")
    public ResponseEntity<YearOfStudyResponse> createYearOfStudy(@Valid @RequestBody YearOfStudyRequest request) {
        YearOfStudyResponse response = yearOfStudyService.createYearOfStudy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update year of study", description = "Updates an existing year of study. Admin only.")
    public ResponseEntity<YearOfStudyResponse> updateYearOfStudy(@PathVariable Long id,
                                                                 @Valid @RequestBody YearOfStudyRequest request) {
        return ResponseEntity.ok(yearOfStudyService.updateYearOfStudy(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete year of study", description = "Deletes a year of study. Admin only.")
    public ResponseEntity<Void> deleteYearOfStudy(@PathVariable Long id) {
        yearOfStudyService.deleteYearOfStudy(id);
        return ResponseEntity.noContent().build();
    }
}
