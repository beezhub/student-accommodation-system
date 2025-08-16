package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.beezhub.student_accommodation.model.dto.PageDto;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room", description = "Room Management APIs")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Create Room", description = "Create a new room")
    @ApiResponse(responseCode = "201", description = "Room created successfully")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest roomRequest) {
        log.info("Creating room: {}", roomRequest.getName());
        RoomResponse response = roomService.createRoom(roomRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Room", description = "Get room by id")
    @ApiResponse(responseCode = "200", description = "Room retrieved successfully")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping
    @Operation(summary = "List Rooms", description = "List rooms with pagination")
    public ResponseEntity<PageDto<RoomResponse>> listRooms(Pageable pageable) {
        PageDto<RoomResponse> page = roomService.getAllRooms(pageable);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Room", description = "Update a room by id")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest roomRequest) {
        RoomResponse response = roomService.updateRoom(id, roomRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Room", description = "Delete a room by id")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    @Operation(summary = "Find rooms by availability", description = "Find rooms that are available or not")
    public ResponseEntity<List<RoomResponse>> findByAvailability(@RequestParam boolean available) {
        return ResponseEntity.ok(roomService.getRoomsByAvailability(available));
    }

    @GetMapping("/capacity")
    @Operation(summary = "Find rooms by minimum capacity", description = "Find rooms with capacity greater than or equal to provided value")
    public ResponseEntity<List<RoomResponse>> findByCapacity(@RequestParam(name = "min") int minCapacity) {
        return ResponseEntity.ok(roomService.getRoomsByCapacity(minCapacity));
    }
}
