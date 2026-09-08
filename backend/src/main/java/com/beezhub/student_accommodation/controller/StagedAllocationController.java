package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.exceptions.UserNotFoundException;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.RoomAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing staged allocations and batch allocation
 * workflows.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/allocation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Staged Allocation Management", description = "Batch allocation proposal and confirmation workflows")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
public class StagedAllocationController {

        private final RoomAllocationService roomAllocationService;
        private final AppUserRepository appUserRepository;

        /**
         * Proposes allocation for a batch of students.
         * Creates staged allocations that require admin confirmation.
         *
         * @param request BatchAllocationRequest with student IDs and batch ID
         * @param auth    Authentication context
         * @return BatchAllocationProposalResponse with proposals and warnings
         */
        @PostMapping("/propose-batch")
        @Operation(summary = "Propose batch allocation", description = "Creates proposals for allocating multiple students to rooms. Max 50 students per batch.")
        public ResponseEntity<BatchAllocationProposalResponse> proposeBatchAllocation(
                        @Valid @RequestBody BatchAllocationRequest request,
                        Authentication auth) {

                log.info("Admin {} proposing batch allocation for {} students, batch: {}",
                                auth.getName(), request.getStudentIds().size(), request.getBatchId());

                Long adminId = extractAdminId(auth);
                BatchAllocationProposalResponse response = roomAllocationService.proposeAllocation(request, adminId);

                log.info("Batch proposal created: {} successful, {} failed",
                                response.getSuccessfulProposals(), response.getFailedProposals());

                return ResponseEntity.ok(response);
        }

        /**
         * Confirms a single staged allocation.
         *
         * @param stagedAllocationId ID of the staged allocation
         * @param auth               Authentication context
         * @return RoomAllocationResponse for the confirmed allocation
         */
        @PostMapping("/staged/{stagedAllocationId}/confirm")
        @Operation(summary = "Confirm staged allocation", description = "Confirms a proposed allocation and creates the actual room allocation")
        public ResponseEntity<RoomAllocationResponse> confirmStagedAllocation(
                        @PathVariable Long stagedAllocationId,
                        Authentication auth) {

                log.info("Admin {} confirming staged allocation {}", auth.getName(), stagedAllocationId);

                Long adminId = extractAdminId(auth);
                RoomAllocationResponse response = roomAllocationService.confirmAllocation(stagedAllocationId, adminId);

                log.info("Staged allocation {} confirmed successfully", stagedAllocationId);
                return ResponseEntity.ok(response);
        }

        /**
         * Rejects a staged allocation.
         *
         * @param stagedAllocationId ID of the staged allocation
         * @param request            RejectionRequest with reason
         * @param auth               Authentication context
         * @return Success message
         */
        @PostMapping("/staged/{stagedAllocationId}/reject")
        @Operation(summary = "Reject staged allocation", description = "Rejects a proposed allocation with a reason")
        public ResponseEntity<MessageResponse> rejectStagedAllocation(
                        @PathVariable Long stagedAllocationId,
                        @Valid @RequestBody RejectAllocationRequest request,
                        Authentication auth) {

                log.info("Admin {} rejecting staged allocation {}: {}",
                                auth.getName(), stagedAllocationId, request.getReason());

                Long adminId = extractAdminId(auth);
                roomAllocationService.rejectAllocation(stagedAllocationId, request.getReason(), adminId);

                return ResponseEntity.ok(new MessageResponse("Staged allocation rejected successfully"));
        }

        /**
         * Confirms all proposed allocations in a batch.
         *
         * @param batchId Batch identifier
         * @param auth    Authentication context
         * @return BatchConfirmationResult with statistics
         */
        @PostMapping("/batch/{batchId}/confirm-all")
        @Operation(summary = "Confirm entire batch", description = "Confirms all proposed allocations in a batch")
        public ResponseEntity<BatchConfirmationResult> confirmBatchAllocations(
                        @PathVariable String batchId,
                        Authentication auth) {

                log.info("Admin {} confirming all allocations in batch {}", auth.getName(), batchId);

                Long adminId = extractAdminId(auth);
                BatchConfirmationResult result = roomAllocationService.confirmBatchAllocationsWithResult(batchId,
                                adminId);

                log.info("Batch {} confirmed: {} successful, {} failed",
                                batchId, result.getSuccessCount(), result.getFailureCount());

                return ResponseEntity.ok(result);
        }

        /**
         * Queries staged allocations with optional filters.
         *
         * @param batchId  Optional batch ID filter
         * @param status   Optional status filter
         * @param pageable Pagination parameters
         * @return Page of StagedAllocationResponse
         */
        @GetMapping("/staged")
        @Operation(summary = "List staged allocations", description = "Retrieves staged allocations with optional filters")
        public ResponseEntity<PageResponse<StagedAllocationResponse>> getStagedAllocations(
                        @RequestParam(required = false) String batchId,
                        @RequestParam(required = false) StagedAllocationStatus status,
                        @PageableDefault(size = 20) Pageable pageable) {

                log.info("Fetching staged allocations - batchId: {}, status: {}", batchId, status);

                PageResponse<StagedAllocationResponse> response = roomAllocationService.getStagedAllocations(batchId,
                                status,
                                pageable);

                log.info("Found {} staged allocations", response.getTotalElements());
                return ResponseEntity.ok(response);
        }

        /**
         * Moves a student from one room to another.
         *
         * @param allocationId ID of the room allocation
         * @param request      MoveStudentRequest with new room and reason
         * @param auth         Authentication context
         * @return Updated RoomAllocationResponse
         */
        @PostMapping("/{allocationId}/move")
        @Operation(summary = "Move student to different room", description = "Moves a student from their current room to a new room")
        public ResponseEntity<RoomAllocationResponse> moveStudent(
                        @PathVariable Long allocationId,
                        @Valid @RequestBody MoveAllocationRequest request,
                        Authentication auth) {

                log.info("Admin {} moving allocation {} to room {}: {}",
                                auth.getName(), allocationId, request.getNewRoomId(), request.getReason());

                Long adminId = extractAdminId(auth);
                RoomAllocationResponse response = roomAllocationService.moveStudent(
                                allocationId,
                                request.getNewRoomId(),
                                request.getReason(),
                                adminId);

                log.info("Student moved successfully to room {}", request.getNewRoomId());
                return ResponseEntity.ok(response);
        }

        /**
         * Overrides constraints to force allocate a student to a room.
         *
         * @param request ConstraintOverrideRequest with student, room, and constraints
         *                bypassed
         * @param auth    Authentication context
         * @return StagedAllocationResponse for the override proposal
         */
        @PostMapping("/override")
        @Operation(summary = "Override constraints for allocation", description = "Bypasses hard constraints to propose allocation (requires admin approval)")
        public ResponseEntity<StagedAllocationResponse> overrideConstraints(
                        @Valid @RequestBody OverrideAllocationRequest request,
                        Authentication auth) {

                log.info("Admin {} overriding constraints for student {} to room {}: {}",
                                auth.getName(), request.getStudentId(), request.getRoomId(), request.getReason());

                Long adminId = extractAdminId(auth);
                StagedAllocationResponse response = roomAllocationService.overrideConstraints(
                                request.getStudentId(),
                                request.getRoomId(),
                                request.getReason(),
                                adminId,
                                request.getBypassConstraints());

                log.info("Constraint override proposal created for student {}", request.getStudentId());
                return ResponseEntity.ok(response);
        }

        /**
         * Adds a student to the waiting list.
         *
         * @param request WaitingListEnqueueRequest with student and application IDs
         * @return Success message
         */
        @PostMapping("/waiting-list/enqueue")
        @Operation(summary = "Add student to waiting list", description = "Adds a student to the waiting list for allocation")
        public ResponseEntity<MessageResponse> enqueueWaitingList(
                        @Valid @RequestBody WaitingListEnqueueRequest request) {
                MessageResponse response = roomAllocationService.enqueueWaitingList(
                                request.getStudentId(),
                                request.getApplicationId());
                return ResponseEntity.ok(response);
        }

        /**
         * Extracts admin user ID from authentication context.
         */
        private Long extractAdminId(Authentication auth) {
                AppUser user = appUserRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new UserNotFoundException("Admin user not found: " + auth.getName()));
                return user.getId();
        }

        /**
         * Gets the waiting list with priority scores.
         *
         * <p>Ordered by queue position, which is the order the batch job consumes the list in.
         * The scores are informational: they describe how well a student matches the available
         * rooms, not when that student will be served. Ranking the display by score meant the
         * student shown at position 1 could lose the last free room to an earlier applicant
         * (GH-140).
         *
         * @param buildingId Optional building filter
         * @param sortBy     Display order: "queue" (default) or "score". Position always stays the
         *                   queue position, so it can never contradict the job.
         * @return List of waiting list entries with scores
         */
        @GetMapping("/waiting-list")
        @Operation(summary = "Get waiting list with scores", description = "Returns the waiting list with priority scores calculated using the latest allocation rules. Ordered by queue position, which is the order the allocation job serves. Pass sortBy=score to view by score; the position column still shows the queue position.")
        public ResponseEntity<java.util.List<WaitingListEntryResponse>> getWaitingListWithScores(
                        @RequestParam(required = false) Long buildingId,
                        @RequestParam(required = false, defaultValue = "queue") String sortBy) {

                log.info("Getting waiting list with scores for buildingId: {} sortBy: {}", buildingId, sortBy);
                java.util.List<WaitingListEntryResponse> response = roomAllocationService
                                .getWaitingListWithScores(buildingId);

                if ("score".equalsIgnoreCase(sortBy)) {
                        // A view, not a re-ranking. Each row keeps the queue position it was given,
                        // so sorting by score cannot make the screen disagree with the job.
                        response = new java.util.ArrayList<>(response);
                        response.sort(java.util.Comparator
                                        .comparing(WaitingListEntryResponse::getPriorityScore,
                                                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
                }
                return ResponseEntity.ok(response);
        }

        /**
         * Rescores waiting list using current allocation rules.
         *
         * @return RescoreResult with number of rescored entries
         */
        @PostMapping("/waiting-list/rescore")
        @Operation(summary = "Rescore waiting list", description = "Rescores all waiting list entries using the latest allocation rules. This is an idempotent operation.")
        public ResponseEntity<RescoreResult> rescoreWaitingList() {
                log.info("Rescoring waiting list");
                RescoreResult result = roomAllocationService.rescoreWaitingList();
                return ResponseEntity.ok(result);
        }
}
