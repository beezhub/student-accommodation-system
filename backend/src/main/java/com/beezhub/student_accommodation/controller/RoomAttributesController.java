package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.RoomAttributesRequest;
import com.beezhub.student_accommodation.model.dto.RoomAttributesResponse;
import com.beezhub.student_accommodation.service.RoomAttributesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Room Attributes", description = "Admin endpoints for managing room attributes (gender restrictions, accessibility, etc.)")
@RestController
@RequestMapping("/api/admin/room-attributes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoomAttributesController {

    private final RoomAttributesService roomAttributesService;

    @Operation(
            summary = "Get room attributes by room ID",
            description = "Retrieves attributes for a specific room. Returns default values if no attributes are configured."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @GetMapping("/room/{roomId}")
    public ResponseEntity<RoomAttributesResponse> getAttributesByRoomId(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId) {
        RoomAttributesResponse response = roomAttributesService.getAttributesByRoomId(roomId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all room attributes for a building",
            description = "Retrieves attributes for all rooms in a building that have custom attributes configured."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes retrieved successfully")
    })
    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<RoomAttributesResponse>> getAttributesByBuildingId(
            @Parameter(description = "Building ID", required = true)
            @PathVariable Long buildingId) {
        List<RoomAttributesResponse> response = roomAttributesService.getAttributesByBuildingId(buildingId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Save room attributes",
            description = "Creates or updates attributes for a room. Use this to set gender restrictions, accessibility compliance, and year of study restrictions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    @PostMapping
    public ResponseEntity<RoomAttributesResponse> saveAttributes(
            @Valid @RequestBody RoomAttributesRequest request,
            Authentication authentication) {
        RoomAttributesResponse response = roomAttributesService.saveAttributes(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Bulk save room attributes",
            description = "Creates or updates attributes for multiple rooms at once."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/bulk")
    public ResponseEntity<List<RoomAttributesResponse>> bulkSaveAttributes(
            @Valid @RequestBody List<RoomAttributesRequest> requests,
            Authentication authentication) {
        List<RoomAttributesResponse> response = roomAttributesService.bulkSaveAttributes(requests, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete room attributes",
            description = "Removes custom attributes for a room, resetting it to default values."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attributes deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Room attributes not found")
    })
    @DeleteMapping("/room/{roomId}")
    public ResponseEntity<Map<String, Boolean>> deleteAttributes(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            Authentication authentication) {
        roomAttributesService.deleteAttributes(roomId, authentication.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
