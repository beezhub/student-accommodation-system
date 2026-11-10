package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.service.allocation.ReconfirmationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Still coming?" before move-in (GH-496). Kept apart from {@code RoomAllocationController}, which
 * is already over the controller size limit.
 */
@RestController
@RequestMapping("/api/room-allocation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Allocation", description = "Endpoints for managing room assignments to students")
public class AllocationReconfirmController {

    private final ReconfirmationService reconfirmationService;

    @PostMapping("/{id}/reconfirm")
    @PreAuthorize("hasAnyRole('STUDENT', 'PLATFORM_ADMIN')")
    @Operation(summary = "Reconfirm an accepted place",
            description = "The student confirms they are still coming, so the place keeps its bed. "
                    + "A student can only reconfirm their own place.")
    public ResponseEntity<RoomAllocationResponse> reconfirm(@PathVariable Long id) {
        log.info("action=reconfirmRequested allocationId={}", id);
        return ResponseEntity.ok(reconfirmationService.reconfirm(id));
    }
}
