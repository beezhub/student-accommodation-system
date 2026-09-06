package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationMethod;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException;
import com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.allocation.ScoringStrategy;
import com.beezhub.student_accommodation.config.FF4jConfig;
import com.beezhub.student_accommodation.service.FeatureFlagService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for automatic room allocation for approved applications.
 *
 * Extracted from RoomAllocationService (Strangler Pattern) to keep the facade lean.
 * Called when an application transitions to APPROVED status and the auto-allocation
 * feature flag is enabled.
 *
 * <p>That flag — FF4j {@code auto-allocation}, toggled at {@code /api/admin/features} — is the one
 * switch for auto-allocation. The batch path reads the same one. There was a second, unconnected
 * {@code auto_allocation_enabled} system setting gating this method; it has been retired (GH-207).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoAllocationService {

    private final FeatureFlagService featureFlagService;
    private final ApplicationDocumentService applicationDocumentService;
    private final ScoringStrategy allocationScoringService;
    private final AllocationLifecycleService allocationLifecycleService;
    private final ApplicationRepository applicationRepository;
    private final UserNotificationService userNotificationService;

    /**
     * Attempts to automatically allocate a room for an approved application.
     *
     * Flow:
     * 1. Check if auto-allocation feature is enabled
     * 2. Validate all required documents are approved
     * 3. Score available rooms using AllocationScoringService
     * 4. Create room allocation for the best-matching room
     * 5. Fall back to AWAITING_ALLOCATION status if no rooms available
     *
     * @param application The approved application to allocate
     */
    @Transactional
    public void attemptAutoAllocation(Application application) {
        log.info("Attempting auto-allocation for application {}", application.getId());

        // The same switch the batch path reads. This used to be the `auto_allocation_enabled`
        // system setting, an entirely separate toggle on a separate screen, so a platform admin
        // who enabled the feature flag still saw per-application allocation skipped and nothing
        // said why (GH-207).
        if (!featureFlagService.isEnabled(FF4jConfig.FEATURE_AUTO_ALLOCATION)) {
            log.info("action=autoAllocationSkipped reason=featureFlagDisabled applicationId={}",
                    application.getId());
            return;
        }

        if (!applicationDocumentService.areAllRequiredDocumentsApproved(application.getId())) {
            log.info("Documents not yet approved for application {}, setting status to AWAITING_ALLOCATION",
                    application.getId());
            application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
            applicationRepository.save(application);
            return;
        }

        Student student = application.getStudent();
        Building preferredBuilding = application.getPreferredBuilding();

        List<ScoredRoom> scoredRooms = allocationScoringService
                .scoreAvailableRooms(student, application, preferredBuilding);

        if (scoredRooms.isEmpty()) {
            setAwaitingAndEnqueueWaitingList(application, student);
            return;
        }

        ScoredRoom bestRoom = scoredRooms.get(0);
        log.info("Best match for application {}: Room {} with score {}", application.getId(),
                bestRoom.getRoomNumber(), bestRoom.getTotalScore());

        allocateBestRoom(application, student, bestRoom);
    }

    // ==================== Private Helpers ====================

    private void allocateBestRoom(Application application, Student student, ScoredRoom bestRoom) {
        RoomAllocationRequest allocationRequest = new RoomAllocationRequest();
        allocationRequest.setStudentId(student.getId());
        allocationRequest.setRoomId(bestRoom.getRoomId());
        allocationRequest.setApplicationId(application.getId());

        try {
            allocationLifecycleService.createRoomAllocation(allocationRequest);
            application.setAllocationMethod(AllocationMethod.AUTO);
            applicationRepository.save(application);
            log.info("Auto-allocated room {} to student {} for application {} (score {})",
                    bestRoom.getRoomNumber(), student.getId(), application.getId(),
                    bestRoom.getTotalScore());

        } catch (RoomCapacityExceededException e) {
            log.warn("Auto-allocation race condition for application {}: {}. Enqueueing waiting list.",
                    application.getId(), e.getMessage());
            setAwaitingAndEnqueueWaitingList(application, student);

        } catch (StudentAlreadyAllocatedException e) {
            // Student already has an active offer pending acceptance — do NOT re-queue.
            log.info("Student {} already has an active allocation; skipping auto-allocation for application {}",
                    student.getId(), application.getId());

        } catch (Exception e) {
            log.error("Unexpected error during auto-allocation for application {}, requires manual allocation",
                    application.getId(), e);
            application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
            applicationRepository.save(application);
        }
    }

    private void setAwaitingAndEnqueueWaitingList(Application application, Student student) {
        application.setStatus(ApplicationStatus.AWAITING_ALLOCATION);
        applicationRepository.save(application);
        log.info("No rooms available for application {}, adding student {} to waiting list",
                application.getId(), student.getId());

        try {
            allocationLifecycleService.enqueueWaitingList(student.getId(), application.getId());
            userNotificationService.createWaitingListNotification(
                    student.getAppUser().getId(), application.getId());
        } catch (Exception e) {
            log.error("Failed to enqueue student {} on waiting list for application {}",
                    student.getId(), application.getId(), e);
        }
    }
}
