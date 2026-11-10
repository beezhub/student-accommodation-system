package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.CompanyApplyOptionsResponse;
import com.beezhub.student_accommodation.service.application.ApplyOptionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * What the application form offers a student (GH-495). Students only: each read is about the
 * caller and their institution.
 */
@Tag(name = "Apply", description = "Buildings and intakes a student can apply for")
@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
public class ApplyOptionsController {

    private final ApplyOptionsService applyOptionsService;

    /**
     * Not paginated: the form shows every building the student can pick from, as
     * {@code GET /api/building/by-institution/{id}}, which it replaces on the form, did.
     */
    @Operation(summary = "Buildings a student can apply to",
            description = "Buildings of any company that has a year for the institution, or an All institutions "
                    + "year, and that list the institution or list none. Defaults to the caller's institution.")
    @GetMapping("/buildings")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<BuildingResponse>> getBuildings(
            @Parameter(description = "Institution to check; empty for the one on the profile")
            @RequestParam(required = false) Long institutionId) {
        return ResponseEntity.ok(applyOptionsService.getBuildingsForInstitution(institutionId));
    }

    @Operation(summary = "Open application windows for the student",
            description = "Companies that can take the student's institution with an intake open today that the "
                    + "student has not applied for. Not paginated: at most one entry per company.")
    @GetMapping("/open-windows")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CompanyApplyOptionsResponse>> getOpenWindows() {
        return ResponseEntity.ok(applyOptionsService.getOpenWindows());
    }

    @Operation(summary = "What a student can apply for at a building's company",
            description = "Open intakes for the institution, leaving out years the student already applied for "
                    + "there, which are linked instead; the next window when none is open; missing documents.")
    @GetMapping("/buildings/{buildingId}/intakes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CompanyApplyOptionsResponse> getIntakes(
            @PathVariable("buildingId") Long buildingId,
            @Parameter(description = "Institution to check; empty for the one on the profile")
            @RequestParam(required = false) Long institutionId) {
        return ResponseEntity.ok(applyOptionsService.getOptionsForBuilding(buildingId, institutionId));
    }
}
