package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.BuildingImageContentResponse;
import com.beezhub.student_accommodation.model.dto.BuildingImageResponse;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.service.BuildingImageService;
import com.beezhub.student_accommodation.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/building")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Building", description = "Endpoints for managing accommodation buildings")
public class BuildingController {

    private final BuildingService buildingService;
    private final BuildingImageService buildingImageService;
    private final com.beezhub.student_accommodation.service.geocoding.GeocodingService geocodingService;

    /**
     * Whether address lookup is available, so the building form can disable its autocomplete
     * rather than leave an administrator typing into a field that returns nothing (GH-152).
     *
     * <p>Deliberately carries no key. This used to return {@code "dummy-api-key-for-development"},
     * and the fix is not to put a real key there: a geocoding key handed to the browser is a
     * geocoding key anyone can read and spend. The lookup happens server-side through
     * {@code /api/geocoding/search}, so the key never leaves the backend.
     */
    @GetMapping("/maps-config")
    @Operation(summary = "Whether address lookup is configured")
    public ResponseEntity<java.util.Map<String, Object>> getMapsConfig() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("enabled", geocodingService.isConfigured());
        return ResponseEntity.ok(config);
    }

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

    @GetMapping("/by-institution/{institutionId}")
    @Operation(summary = "Get buildings by institution", description = "Returns all buildings that serve a specific institution")
    public ResponseEntity<List<BuildingResponse>> getBuildingsByInstitution(@PathVariable Long institutionId) {
        log.info("Fetching buildings for institution: {}", institutionId);
        List<BuildingResponse> buildings = buildingService.getBuildingsByInstitution(institutionId);
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

    @GetMapping("/browse")
    @Operation(summary = "Browse buildings with filters",
            description = "Public paginated building browse with optional filters for price range, availability, institution, and text search")
    public ResponseEntity<PageResponse<BuildingResponse>> browseBuildings(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 12, sort = "name") Pageable pageable) {
        log.info("Browsing buildings - minPrice: {}, maxPrice: {}, available: {}, institutionId: {}, search: {}",
                minPrice, maxPrice, available, institutionId, search);
        PageResponse<BuildingResponse> page = buildingService.browseBuildings(
                minPrice, maxPrice, available, institutionId, search, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get building by ID", description = "Returns a single building by its ID")
    public ResponseEntity<BuildingResponse> getBuildingById(@PathVariable Long id) {
        log.info("Fetching building with id: {}", id);
        return ResponseEntity.ok(buildingService.getBuildingById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create building", description = "Creates a new accommodation building. Admin only.")
    public ResponseEntity<BuildingResponse> createBuilding(@Valid @RequestBody BuildingRequest request) {
        log.info("Creating new building: {}", request.getName());
        BuildingResponse response = buildingService.createBuilding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update building", description = "Updates an existing building. Admin only.")
    public ResponseEntity<BuildingResponse> updateBuilding(@PathVariable Long id,
            @Valid @RequestBody BuildingRequest request) {
        log.info("Updating building with id: {}", id);
        return ResponseEntity.ok(buildingService.updateBuilding(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete building", description = "Deletes a building. Admin only.")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
        log.info("Deleting building with id: {}", id);
        buildingService.deleteBuilding(id);
        return ResponseEntity.noContent().build();
    }

    // Building Image Endpoints

    @PostMapping("/{buildingId}/images")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Upload building images", description = "Upload one or more images for a building. Admin only.")
    public ResponseEntity<List<BuildingImageResponse>> uploadBuildingImages(
            @PathVariable Long buildingId,
            @RequestParam("files") List<MultipartFile> files) {
        log.info("Uploading {} images for building {}", files.size(), buildingId);
        List<BuildingImageResponse> responses = buildingImageService.uploadImages(buildingId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/{buildingId}/images")
    @Operation(summary = "Get building images", description = "Returns all images for a building")
    public ResponseEntity<List<BuildingImageResponse>> getBuildingImages(@PathVariable Long buildingId) {
        log.info("Fetching images for building {}", buildingId);
        List<BuildingImageResponse> images = buildingImageService.getImages(buildingId);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{buildingId}/images/{imageId}/content")
    @Operation(summary = "Get building image content",
            description = "Streams the image bytes through the backend. Public, like the rest of building browsing: "
                    + "MinIO itself is never reachable from a browser.")
    public ResponseEntity<Resource> getBuildingImageContent(
            @PathVariable Long buildingId,
            @PathVariable Long imageId) {
        BuildingImageContentResponse content = buildingImageService.getImageContent(buildingId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.getContentType()))
                // Building photos change rarely and only through an admin action that mints a new
                // imageId, so a day-long cache is safe and saves re-streaming the same bytes on
                // every browse-grid render.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.getResource());
    }

    @DeleteMapping("/{buildingId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete building image", description = "Deletes a building image. Admin only.")
    public ResponseEntity<Void> deleteBuildingImage(
            @PathVariable Long buildingId,
            @PathVariable Long imageId) {
        log.info("Deleting image {} from building {}", imageId, buildingId);
        buildingImageService.deleteImage(buildingId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{buildingId}/images/{imageId}/primary")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Set primary image", description = "Sets an image as the primary image for a building. Admin only.")
    public ResponseEntity<BuildingImageResponse> setPrimaryImage(
            @PathVariable Long buildingId,
            @PathVariable Long imageId) {
        log.info("Setting image {} as primary for building {}", imageId, buildingId);
        BuildingImageResponse response = buildingImageService.setPrimaryImage(buildingId, imageId);
        return ResponseEntity.ok(response);
    }
}
