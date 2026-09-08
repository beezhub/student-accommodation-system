package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.BuildingAttributesRequest;
import com.beezhub.student_accommodation.model.dto.BuildingAttributesResponse;
import com.beezhub.student_accommodation.model.dto.RoomAttributesRequest;
import com.beezhub.student_accommodation.model.dto.RoomAttributesResponse;
import com.beezhub.student_accommodation.service.BuildingAttributesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Building & Room Attributes", description = "Admin endpoints for managing building and room attributes for allocation")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class BuildingAttributesController {

    private final BuildingAttributesService buildingAttributesService;

    // ==================== Building Attributes Endpoints ====================

    @Operation(
            summary = "Get building attributes",
            description = "Retrieves the attributes for a specific building including gender restriction, " +
                          "accessibility features, desirability score, and mixed gender policy."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @GetMapping("/buildings/{buildingId}/attributes")
    public ResponseEntity<BuildingAttributesResponse> getBuildingAttributes(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId) {
        BuildingAttributesResponse response = buildingAttributesService.getBuildingAttributes(buildingId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update building attributes",
            description = "Creates or updates the attributes for a specific building. " +
                          "All fields are optional - only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid enum value or validation failed"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @PutMapping("/buildings/{buildingId}/attributes")
    public ResponseEntity<BuildingAttributesResponse> updateBuildingAttributes(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Valid @RequestBody BuildingAttributesRequest request) {
        // Override buildingId from path
        request.setBuildingId(buildingId);
        BuildingAttributesResponse response = buildingAttributesService.updateBuildingAttributes(buildingId, request);
        return ResponseEntity.ok(response);
    }

    // ==================== Room Attributes Endpoints ====================

    @Operation(
            summary = "Get room attributes",
            description = "Retrieves the attributes for a specific room including accessibility compliance, " +
                          "gender restriction override, and desirability override."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @GetMapping("/rooms/{roomId}/attributes")
    public ResponseEntity<RoomAttributesResponse> getRoomAttributes(
            @Parameter(description = "ID of the room", required = true)
            @PathVariable Long roomId) {
        RoomAttributesResponse response = buildingAttributesService.getRoomAttributes(roomId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update room attributes",
            description = "Creates or updates the attributes for a specific room. " +
                          "All fields are optional - only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid enum value or validation failed"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @PutMapping("/rooms/{roomId}/attributes")
    public ResponseEntity<RoomAttributesResponse> updateRoomAttributes(
            @Parameter(description = "ID of the room", required = true)
            @PathVariable Long roomId,
            @Valid @RequestBody RoomAttributesRequest request) {
        // Override roomId from path
        request.setRoomId(roomId);
        RoomAttributesResponse response = buildingAttributesService.updateRoomAttributes(roomId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all room attributes for a building",
            description = "Retrieves the attributes for all rooms in a specific building that have custom attributes set."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room attributes retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Building not found")
    })
    @GetMapping("/buildings/{buildingId}/rooms/attributes")
    public ResponseEntity<List<RoomAttributesResponse>> getRoomAttributesByBuilding(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId) {
        List<RoomAttributesResponse> response = buildingAttributesService.getRoomAttributesByBuilding(buildingId);
        return ResponseEntity.ok(response);
    }
}
