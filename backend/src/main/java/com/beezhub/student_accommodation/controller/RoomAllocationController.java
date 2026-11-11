package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room-allocation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Allocation", description = "Endpoints for managing room assignments to students")
public class RoomAllocationController {

        private final RoomAllocationService roomAllocationService;
        private final AppUserRepository appUserRepository;

        @GetMapping
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Get all room allocations", description = "Returns a paginated list of all room allocations. Supports pagination with query params: page, size, sort")
        public ResponseEntity<PageResponse<RoomAllocationResponse>> getAllRoomAllocations(
                        @PageableDefault(size = 20, sort = "allocationDate") Pageable pageable) {
                log.info("Fetching all room allocations with pagination");
                PageResponse<RoomAllocationResponse> page = roomAllocationService.getAllRoomAllocations(pageable);
                return ResponseEntity.ok(page);
        }

        @GetMapping("/all")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Get all room allocations without pagination", description = "Returns a list of all room allocations without pagination")
        public ResponseEntity<List<RoomAllocationResponse>> getAllRoomAllocationsNoPagination() {
                log.info("Fetching all room allocations without pagination");
                List<RoomAllocationResponse> allocations = roomAllocationService.getAllRoomAllocations();
                return ResponseEntity.ok(allocations);
        }

        @GetMapping("/search")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Search room allocations", description = "Search and filter room allocations by student name, room number, building, status, or date range")
        public ResponseEntity<PageResponse<RoomAllocationResponse>> searchRoomAllocations(
                        @RequestParam(required = false) Long studentId,
                        @RequestParam(required = false) Long roomId,
                        @RequestParam(required = false) Long buildingId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                        @RequestParam(required = false) String studentName,
                        @RequestParam(required = false) String roomNumber,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) Long institutionId,
                        @RequestParam(required = false) Long intakeId,
                        @PageableDefault(size = 20, sort = "allocationDate") Pageable pageable) {
                log.info("Searching room allocations with filters - studentId: {}, roomId: {}, buildingId: {}, fromDate: {}, toDate: {}, studentName: {}, roomNumber: {}, status: {}, institutionId: {}, intakeId: {}",
                                studentId, roomId, buildingId, fromDate, toDate, studentName, roomNumber, status,
                                institutionId, intakeId);
                PageResponse<RoomAllocationResponse> page = roomAllocationService.searchRoomAllocations(
                                studentId, roomId, buildingId, fromDate, toDate, studentName, roomNumber, status,
                                institutionId, intakeId, pageable);
                return ResponseEntity.ok(page);
        }

        @GetMapping("/{id}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get room allocation by ID", description = "Returns a single room allocation by its ID")
        public ResponseEntity<RoomAllocationResponse> getRoomAllocationById(@PathVariable Long id) {
                log.info("Fetching room allocation with id: {}", id);
                return ResponseEntity.ok(roomAllocationService.getRoomAllocationById(id));
        }

        @PostMapping
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Create room allocation", description = "Creates a new room allocation (assigns a student to a room). Admin only.")
        public ResponseEntity<RoomAllocationResponse> createRoomAllocation(
                        @Valid @RequestBody RoomAllocationRequest request) {
                log.info("Creating new room allocation for student: {} to room: {}", request.getStudentId(),
                                request.getRoomId());
                RoomAllocationResponse response = roomAllocationService.createRoomAllocation(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Update room allocation", description = "Updates an existing room allocation. Admin only.")
        public ResponseEntity<RoomAllocationResponse> updateRoomAllocation(@PathVariable Long id,
                        @Valid @RequestBody RoomAllocationRequest request) {
                log.info("Updating room allocation with id: {}", id);
                return ResponseEntity.ok(roomAllocationService.updateRoomAllocation(id, request));
        }

        @PostMapping("/{id}/accept")
        @PreAuthorize("hasAnyRole('STUDENT', 'PLATFORM_ADMIN')")
        @Operation(summary = "Accept allocation offer", description = "Accepts an offered room allocation. Student or Admin.")
        public ResponseEntity<RoomAllocationResponse> acceptOffer(@PathVariable Long id,
                        @Valid @RequestBody AcceptOfferRequest request) {
                log.info("Accepting allocation offer with id: {}", id);
                return ResponseEntity.ok(roomAllocationService.acceptOffer(id, request));
        }

        @PostMapping("/{id}/decline")
        @PreAuthorize("hasAnyRole('STUDENT', 'PLATFORM_ADMIN')")
        @Operation(summary = "Decline allocation offer", description = "Declines an offered room allocation with required reason. Student or Admin.")
        public ResponseEntity<RoomAllocationResponse> declineOffer(@PathVariable Long id,
                        @Valid @RequestBody DeclineOfferRequest request) {
                log.info("Declining allocation offer with id: {} for reason: {}", id, request.getDeclineReason());
                return ResponseEntity.ok(roomAllocationService.declineOffer(id, request));
        }

        @PostMapping("/{id}/extend-offer")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(
                summary = "Extend an offer's response deadline",
                description = "Moves the deadline on an outstanding offer, so a student who missed it can "
                              + "still accept. Admin only, and recorded against the allocation with the "
                              + "reason given. The new deadline must be in the future and no later than a "
                              + "fresh offer would give. Only while the offer is still OFFERED: once the "
                              + "expiry sweep has run the room is released and the student must be "
                              + "allocated a room again."
        )
        public ResponseEntity<RoomAllocationResponse> extendOffer(
                        @PathVariable Long id,
                        @Valid @RequestBody ExtendOfferRequest request) {
                log.info("Extending offer deadline for allocation {} to {}", id, request.newExpiryDate());
                return ResponseEntity.ok(roomAllocationService.extendOfferDeadline(
                                id, request.newExpiryDate(), request.reason()));
        }

        @PostMapping("/{id}/check-in")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(
                summary = "Check in student", 
                description = "Confirms student physical arrival and activates the allocation. " +
                              "Sets allocation status to ACTIVE and application status to CHECKED_IN. " +
                              "This should be done when the student arrives and receives room keys. Admin only. " +
                              "Refused once the check-in deadline has passed; pass overrideDeadline=true to " +
                              "check the student in anyway, which is recorded against the allocation."
        )
        public ResponseEntity<RoomAllocationResponse> checkIn(
                        @PathVariable Long id,
                        @RequestParam(name = "overrideDeadline", defaultValue = "false") boolean overrideDeadline) {
                log.info("Checking in allocation with id: {} overrideDeadline={}", id, overrideDeadline);
                return ResponseEntity.ok(roomAllocationService.confirmCheckIn(id, overrideDeadline));
        }

        @PostMapping("/{id}/check-out")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Check out student", description = "Confirms student departure and completes the allocation. Admin only.")
        public ResponseEntity<RoomAllocationResponse> checkOut(@PathVariable Long id) {
                log.info("Checking out allocation with id: {}", id);
                return ResponseEntity.ok(roomAllocationService.confirmCheckOut(id));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Delete room allocation", description = "Deletes a room allocation (removes student from room). Admin only.")
        public ResponseEntity<Void> deleteRoomAllocation(@PathVariable Long id) {
                log.info("Deleting room allocation with id: {}", id);
                roomAllocationService.deleteRoomAllocation(id);
                return ResponseEntity.noContent().build();
        }

        @PatchMapping("/{id}/end")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "End room allocation", description = "Ends an active room allocation by setting status to COMPLETED. Admin only.")
        public ResponseEntity<RoomAllocationResponse> endRoomAllocation(@PathVariable Long id) {
                log.info("Ending room allocation with id: {}", id);
                RoomAllocationResponse response = roomAllocationService.endAllocation(id);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/student/{studentId}/active")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get active allocation by student ID", description = "Returns the active room allocation for a specific student")
        public ResponseEntity<RoomAllocationResponse> getActiveAllocationByStudentId(@PathVariable Long studentId) {
                log.info("Fetching active allocation for student: {}", studentId);
                RoomAllocationResponse response = roomAllocationService.getActiveAllocationByStudentId(studentId);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/allocate-by-application")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Allocate room by application", description = "Allocates a room to a student based on an approved application. Validates business rules automatically. Admin only.")
        public ResponseEntity<RoomAllocationResponse> allocateRoomByApplication(
                        @Valid @RequestBody AllocateByApplicationRequest request) {
                log.info("Allocating room for application: {} to room: {}", request.getApplicationId(),
                                request.getRoomId());
                RoomAllocationResponse response = roomAllocationService.allocateRoomByApplication(
                                request.getApplicationId(), request.getRoomId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/status/{status}")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Get allocations by status", description = "Returns paginated room allocations filtered by status (ACTIVE, COMPLETED, CANCELLED)")
        public ResponseEntity<PageResponse<RoomAllocationResponse>> getAllocationsByStatus(
                        @PathVariable String status,
                        @PageableDefault(size = 20, sort = "allocationDate") Pageable pageable) {
                log.info("Fetching allocations with status: {}", status);
                PageResponse<RoomAllocationResponse> page = roomAllocationService.getAllocationsByStatus(status,
                                pageable);
                return ResponseEntity.ok(page);
        }

        @GetMapping("/room/{roomId}/availability")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Check room availability", description = "Returns detailed availability information for a specific room")
        public ResponseEntity<RoomAvailabilityResponse> getRoomAvailability(@PathVariable Long roomId) {
                log.info("Checking availability for room: {}", roomId);
                RoomAvailabilityResponse response = roomAllocationService.getRoomAvailability(roomId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/room/{roomId}/is-available")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Simple room availability check", description = "Returns true if room has available capacity, false otherwise")
        public ResponseEntity<Boolean> isRoomAvailable(@PathVariable Long roomId) {
                log.info("Simple availability check for room: {}", roomId);
                boolean available = roomAllocationService.isRoomAvailable(roomId);
                return ResponseEntity.ok(available);
        }

        @GetMapping("/student/{studentId}/eligibility")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Check student allocation eligibility", description = "Validates if a student is eligible for room allocation and returns the reason")
        public ResponseEntity<AllocationEligibilityResponse> checkStudentEligibility(@PathVariable Long studentId) {
                log.info("Checking eligibility for student: {}", studentId);
                AllocationEligibilityResponse response = roomAllocationService.checkStudentEligibility(studentId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/admin/student/{studentId}/eligible-rooms")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Get eligible rooms for student", description = "Returns rooms that meet all hard constraints for a student (gender, accessibility, year of study, institution)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Eligible rooms retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Student not found")
        })
        public ResponseEntity<List<EligibleRoomResponse>> getEligibleRoomsForStudent(
                        @Parameter(description = "Student ID", required = true) @PathVariable Long studentId,
                        @Parameter(description = "Filter by building ID") @RequestParam(required = false) Long buildingId) {
                log.info("Getting eligible rooms for student: {}, building: {}", studentId, buildingId);
                List<EligibleRoomResponse> response = roomAllocationService.getEligibleRoomsForStudent(studentId,
                                buildingId);
                return ResponseEntity.ok(response);
        }

        // ==================== Audit History ====================

        @GetMapping("/{id}/history")
        @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
        @Operation(summary = "Get allocation status history",
                description = "Returns the full audit history for a room allocation, ordered by most recent first")
        @ApiResponse(responseCode = "200", description = "Audit history retrieved successfully")
        public ResponseEntity<List<AllocationAuditLogResponse>> getAllocationHistory(
                        @Parameter(description = "Allocation ID") @PathVariable Long id) {
                log.info("Fetching audit history for allocation: {}", id);
                List<AllocationAuditLogResponse> history = roomAllocationService.getAllocationHistory(id);
                return ResponseEntity.ok(history);
        }

        // ==================== Helper Methods ====================

        private Long getAdminId(Authentication authentication) {
                return appUserRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new UserNotFoundException("Admin user not found"))
                                .getId();
        }
}