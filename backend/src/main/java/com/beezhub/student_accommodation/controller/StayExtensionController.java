package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.dto.StayExtensionResponse;
import com.beezhub.student_accommodation.service.allocation.StayExtensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Stay for Semester 2" (GH-498). Kept apart from {@code RoomAllocationController}, which is already
 * over the controller size limit.
 */
@RestController
@RequestMapping("/api/room-allocation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Allocation", description = "Endpoints for managing room assignments to students")
public class StayExtensionController {

    private final StayExtensionService stayExtensionService;

    @GetMapping("/{id}/stay-extension")
    @PreAuthorize("hasAnyRole('STUDENT', 'PLATFORM_ADMIN')")
    @Operation(summary = "Can I stay for the next intake?",
            description = "Whether the student can stay on in this room for the next intake of the same type "
                    + "in the same year, and that intake. A student can only ask about their own place.")
    public ResponseEntity<StayExtensionResponse> getStayExtension(@PathVariable Long id) {
        return ResponseEntity.ok(stayExtensionService.getOption(id));
    }

    @PostMapping("/{id}/stay-extension")
    @PreAuthorize("hasAnyRole('STUDENT', 'PLATFORM_ADMIN')")
    @Operation(summary = "Stay for the next intake",
            description = "Moves the planned check-out to the end of the next intake, straight away, when the "
                    + "bed is still free and the documents are in order. Refused with 409 otherwise.")
    public ResponseEntity<RoomAllocationResponse> extendStay(@PathVariable Long id) {
        log.info("action=stayExtensionRequested allocationId={}", id);
        return ResponseEntity.ok(stayExtensionService.extend(id));
    }
}
