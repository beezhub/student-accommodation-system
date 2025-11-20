package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/building")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Building", description = "Endpoints for managing accommodation buildings")
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    @Operation(summary = "Get all buildings", description = "Returns a paginated list of all accommodation buildings. Supports pagination with query params: page, size, sort")
    public ResponseEntity<PageResponse<BuildingResponse>> getAllBuildings(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        log.info("Fetching all buildings with pagination");
        PageResponse<BuildingResponse> page = buildingService.getAllBuildings(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all buildings without pagination", description = "Returns a list of all accommodation buildings without pagination")
    public ResponseEntity<List<BuildingResponse>> getAllBuildingsNoPagination() {
        log.info("Fetching all buildings without pagination");
        List<BuildingResponse> buildings = buildingService.getAllBuildings();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/search")
    @Operation(summary = "Search buildings", description = "Search and filter buildings by name or address")
    public ResponseEntity<PageResponse<BuildingResponse>> searchBuildings(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        log.info("Searching buildings with filters - name: {}, address: {}", name, address);
        PageResponse<BuildingResponse> page = buildingService.searchBuildings(name, address, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get building by ID", description = "Returns a single building by its ID")
    public ResponseEntity<BuildingResponse> getBuildingById(@PathVariable Long id) {
        log.info("Fetching building with id: {}", id);
        return ResponseEntity.ok(buildingService.getBuildingById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create building", description = "Creates a new accommodation building. Admin only.")
    public ResponseEntity<BuildingResponse> createBuilding(@Valid @RequestBody BuildingRequest request) {
        log.info("Creating new building: {}", request.getName());
        BuildingResponse response = buildingService.createBuilding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update building", description = "Updates an existing building. Admin only.")
    public ResponseEntity<BuildingResponse> updateBuilding(@PathVariable Long id,
                                                            @Valid @RequestBody BuildingRequest request) {
        log.info("Updating building with id: {}", id);
        return ResponseEntity.ok(buildingService.updateBuilding(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete building", description = "Deletes a building. Admin only.")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
        log.info("Deleting building with id: {}", id);
        buildingService.deleteBuilding(id);
        return ResponseEntity.noContent().build();
    }
}
