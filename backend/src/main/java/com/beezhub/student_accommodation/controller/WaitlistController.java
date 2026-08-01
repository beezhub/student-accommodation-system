package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.WaitlistPositionDto;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student-facing waiting-list transparency endpoint. Position, queue depth,
 * and estimated wait time are computed on demand — see {@link WaitingListService}.
 */
@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
@Tag(name = "Waitlist", description = "Student waiting-list position and queue transparency")
public class WaitlistController {

    private final WaitingListService waitingListService;

    @GetMapping("/position")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get my waitlist position", description = "Returns the authenticated student's current waiting-list position, total queue depth, and estimated wait time. Returns 404 if the student has no active waiting-list entry.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Waitlist position retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Student is not currently on the waiting list"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<WaitlistPositionDto> getMyWaitlistPosition() {
        return ResponseEntity.ok(waitingListService.getMyWaitlistPosition());
    }
}
