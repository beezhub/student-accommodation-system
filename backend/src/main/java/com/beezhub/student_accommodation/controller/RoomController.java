package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.service.RoomService;
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
@RequestMapping("/api/room")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room", description = "Endpoints for managing accommodation rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "Get all rooms", description = "Returns a paginated list of all accommodation rooms. Supports pagination with query params: page, size, sort")
    public ResponseEntity<PageResponse<RoomResponse>> getAllRooms(
            @PageableDefault(size = 20, sort = "roomNumber") Pageable pageable) {
        log.info("Fetching all rooms with pagination");
        PageResponse<RoomResponse> page = roomService.getAllRooms(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all rooms without pagination", description = "Returns a list of all accommodation rooms without pagination")
    public ResponseEntity<List<RoomResponse>> getAllRoomsNoPagination() {
        log.info("Fetching all rooms without pagination");
        List<RoomResponse> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/search")
    @Operation(summary = "Search rooms", description = "Search and filter rooms by room number, building, or capacity range")
    public ResponseEntity<PageResponse<RoomResponse>> searchRooms(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity,
            @PageableDefault(size = 20, sort = "roomNumber") Pageable pageable) {
        log.info("Searching rooms with filters - roomNumber: {}, buildingId: {}, minCapacity: {}, maxCapacity: {}",
                roomNumber, buildingId, minCapacity, maxCapacity);
        PageResponse<RoomResponse> page = roomService.searchRooms(roomNumber, buildingId, minCapacity, maxCapacity, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Returns a single room by its ID")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        log.info("Fetching room with id: {}", id);
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create room", description = "Creates a new accommodation room. Admin only.")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        log.info("Creating new room: {}", request.getRoomNumber());
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update room", description = "Updates an existing room. Admin only.")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id,
                                                     @Valid @RequestBody RoomRequest request) {
        log.info("Updating room with id: {}", id);
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete room", description = "Deletes a room. Admin only.")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        log.info("Deleting room with id: {}", id);
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
