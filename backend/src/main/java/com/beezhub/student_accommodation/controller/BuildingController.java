package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
@Tag(name = "Building", description = "Endpoints for managing buildings")
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    @Operation(summary = "Create building")
    public ResponseEntity<BuildingResponse> create(@Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.ok(buildingService.createBuilding(request));
    }

    @GetMapping
    @Operation(summary = "Get all buildings")
    public ResponseEntity<List<BuildingResponse>> getAll() {
        return ResponseEntity.ok(buildingService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get building by id")
    public ResponseEntity<BuildingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(buildingService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update building by id")
    public ResponseEntity<BuildingResponse> update(@PathVariable Long id, @Valid @RequestBody BuildingRequest request) {
        return ResponseEntity.ok(buildingService.updateBuilding(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete building by id")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.noContent().build();
    }
}
