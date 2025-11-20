package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/room-allocation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Allocation", description = "Endpoints for managing room assignments to students")
public class RoomAllocationController {

    private final RoomAllocationService roomAllocationService;

    @GetMapping
    @Operation(summary = "Get all room allocations", description = "Returns a paginated list of all room allocations. Supports pagination with query params: page, size, sort")
    public ResponseEntity<PageResponse<RoomAllocationResponse>> getAllRoomAllocations(
            @PageableDefault(size = 20, sort = "allocationDate") Pageable pageable) {
        log.info("Fetching all room allocations with pagination");
        PageResponse<RoomAllocationResponse> page = roomAllocationService.getAllRoomAllocations(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all room allocations without pagination", description = "Returns a list of all room allocations without pagination")
    public ResponseEntity<List<RoomAllocationResponse>> getAllRoomAllocationsNoPagination() {
        log.info("Fetching all room allocations without pagination");
        List<RoomAllocationResponse> allocations = roomAllocationService.getAllRoomAllocations();
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/search")
    @Operation(summary = "Search room allocations", description = "Search and filter room allocations by student, room, building, or date range")
    public ResponseEntity<PageResponse<RoomAllocationResponse>> searchRoomAllocations(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "allocationDate") Pageable pageable) {
        log.info("Searching room allocations with filters - studentId: {}, roomId: {}, buildingId: {}, fromDate: {}, toDate: {}",
                studentId, roomId, buildingId, fromDate, toDate);
        PageResponse<RoomAllocationResponse> page = roomAllocationService.searchRoomAllocations(
                studentId, roomId, buildingId, fromDate, toDate, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room allocation by ID", description = "Returns a single room allocation by its ID")
    public ResponseEntity<RoomAllocationResponse> getRoomAllocationById(@PathVariable Long id) {
        log.info("Fetching room allocation with id: {}", id);
        return ResponseEntity.ok(roomAllocationService.getRoomAllocationById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create room allocation", description = "Creates a new room allocation (assigns a student to a room). Admin only.")
    public ResponseEntity<RoomAllocationResponse> createRoomAllocation(@Valid @RequestBody RoomAllocationRequest request) {
        log.info("Creating new room allocation for student: {} to room: {}", request.getStudentId(), request.getRoomId());
        RoomAllocationResponse response = roomAllocationService.createRoomAllocation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update room allocation", description = "Updates an existing room allocation. Admin only.")
    public ResponseEntity<RoomAllocationResponse> updateRoomAllocation(@PathVariable Long id,
                                                                        @Valid @RequestBody RoomAllocationRequest request) {
        log.info("Updating room allocation with id: {}", id);
        return ResponseEntity.ok(roomAllocationService.updateRoomAllocation(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete room allocation", description = "Deletes a room allocation (removes student from room). Admin only.")
    public ResponseEntity<Void> deleteRoomAllocation(@PathVariable Long id) {
        log.info("Deleting room allocation with id: {}", id);
        roomAllocationService.deleteRoomAllocation(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/end")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "End room allocation", description = "Ends an active room allocation by setting status to COMPLETED. Admin only.")
    public ResponseEntity<RoomAllocationResponse> endRoomAllocation(@PathVariable Long id) {
        log.info("Ending room allocation with id: {}", id);
        RoomAllocationResponse response = roomAllocationService.endAllocation(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}/active")
    @Operation(summary = "Get active allocation by student ID", description = "Returns the active room allocation for a specific student")
    public ResponseEntity<RoomAllocationResponse> getActiveAllocationByStudentId(@PathVariable Long studentId) {
        log.info("Fetching active allocation for student: {}", studentId);
        RoomAllocationResponse response = roomAllocationService.getActiveAllocationByStudentId(studentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/allocate-by-application")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Allocate room by application", description = "Allocates a room to a student based on an approved application. Validates business rules automatically. Admin only.")
    public ResponseEntity<RoomAllocationResponse> allocateRoomByApplication(
            @Valid @RequestBody AllocateByApplicationRequest request) {
        log.info("Allocating room for application: {} to room: {}", request.getApplicationId(), request.getRoomId());
        RoomAllocationResponse response = roomAllocationService.allocateRoomByApplication(
                request.getApplicationId(), request.getRoomId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get allocations by status", description = "Returns paginated room allocations filtered by status (ACTIVE, COMPLETED, CANCELLED)")
    public ResponseEntity<PageResponse<RoomAllocationResponse>> getAllocationsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20, sort = "allocationDate") Pageable pageable) {
        log.info("Fetching allocations with status: {}", status);
        PageResponse<RoomAllocationResponse> page = roomAllocationService.getAllocationsByStatus(status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/room/{roomId}/availability")
    @Operation(summary = "Check room availability", description = "Returns detailed availability information for a specific room")
    public ResponseEntity<RoomAvailabilityResponse> getRoomAvailability(@PathVariable Long roomId) {
        log.info("Checking availability for room: {}", roomId);
        RoomAvailabilityResponse response = roomAllocationService.getRoomAvailability(roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}/is-available")
    @Operation(summary = "Simple room availability check", description = "Returns true if room has available capacity, false otherwise")
    public ResponseEntity<Boolean> isRoomAvailable(@PathVariable Long roomId) {
        log.info("Simple availability check for room: {}", roomId);
        boolean available = roomAllocationService.isRoomAvailable(roomId);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/student/{studentId}/eligibility")
    @Operation(summary = "Check student allocation eligibility", description = "Validates if a student is eligible for room allocation and returns the reason")
    public ResponseEntity<AllocationEligibilityResponse> checkStudentEligibility(@PathVariable Long studentId) {
        log.info("Checking eligibility for student: {}", studentId);
        AllocationEligibilityResponse response = roomAllocationService.checkStudentEligibility(studentId);
        return ResponseEntity.ok(response);
    }
}