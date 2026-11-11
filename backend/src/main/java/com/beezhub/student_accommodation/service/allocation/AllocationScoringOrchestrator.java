package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.dto.RescoreResult;
import com.beezhub.student_accommodation.model.dto.ScoredRoom;
import com.beezhub.student_accommodation.model.dto.WaitingListEntryResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.WaitingListRepository;
import com.beezhub.student_accommodation.service.allocation.ScoringStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationScoringOrchestrator {

    private final ScoringStrategy allocationScoringService;
    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomRepository roomRepository;
    private final WaitingListRepository waitingListRepository;
    private final ApplicantPriorityScorer applicantPriorityScorer;
    private final BuildingRepository buildingRepository;
    private final Clock clock;

    public Map<Long, List<ScoredRoom>> buildScoringMatrix(List<WaitingListEntry> waitingList) {
        Map<Long, List<ScoredRoom>> scoringMatrix = new HashMap<>();
        LocalDate today = LocalDate.now(clock);

        for (WaitingListEntry entry : waitingList) {
            Student student = entry.getStudent();
            Application app = entry.getApplication();
            StayPeriod stay = StayPeriod.of(app);

            // Never offer a stay that has already ended (GH-496).
            if (stay.hasEnded(today)) {
                log.info("action=scoringSkipped reason=stayEnded waitingListEntryId={}", entry.getId());
                continue;
            }

            // Skip a student who already holds a bed for overlapping dates. A 2026 resident is
            // still matched for a 2027 stay.
            if (roomAllocationRepository.existsLiveAllocationForStudentDuring(
                    student.getId(), stay.from(), stay.until())) {
                continue;
            }

            Building preferredBuilding = app != null ? app.getPreferredBuilding() : null;

            List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(
                    student, app, preferredBuilding);

            if (!scoredRooms.isEmpty()) {
                scoringMatrix.put(student.getId(), scoredRooms);
            }
        }

        return scoringMatrix;
    }

    /** Free beds by room and stay for one batch run, starting from the database count. */
    public BedLedger newBedLedger(List<Room> rooms) {
        return new BedLedger(rooms, roomAllocationRepository);
    }

    /** The transaction is what lets the tenant filter scope this to the tenant in context. */
    @Transactional(readOnly = true)
    public List<Room> getAvailableRoomsForBatch() {
        return roomRepository.findRoomsOpenForOffers();
    }

    /**
     * Read in one transaction so the tenant filter applies: a tenant admin sees only their own queue,
     * ranked by their own rule. Platform roles have no filter and see every tenant.
     */
    @Transactional(readOnly = true)
    public List<WaitingListEntryResponse> getWaitingListWithScores(Long buildingId) {
        log.info("Getting waiting list with scores for building {}", buildingId);

        // Ordered by the same scorer the batch job uses, not by the repository method's createdAt.
        // The admin screen showing one order while the job serves another is GH-140, and arrival
        // alone stopped being the order when the weights started counting (GH-225).
        List<WaitingListEntry> entries =
                new ArrayList<>(waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING"));
        Map<Long, BigDecimal> priorities = applicantPriorityScorer.priorityByEntryId(
                entries, applicantPriorityScorer.tenantRule(), LocalDateTime.now());
        entries.sort(applicantPriorityScorer.queueOrder(priorities));

        List<WaitingListEntryResponse> responses = new ArrayList<>();

        Building targetBuilding = buildingId != null ? buildingRepository.findById(buildingId).orElse(null) : null;

        AtomicInteger position = new AtomicInteger(1);

        for (WaitingListEntry entry : entries) {
            Student student = entry.getStudent();
            Application application = entry.getApplication();

            WaitingListEntryResponse response = WaitingListEntryResponse.builder()
                    .id(entry.getId())
                    .studentId(student.getId())
                    .studentNumber(student.getStudentNumber())
                    .studentName(student.getAppUser().getFirstName() + " " + student.getAppUser().getLastName())
                    .studentEmail(student.getAppUser().getEmail())
                    .studentGender(student.getGender())
                    .status(entry.getStatus())
                    .createdAt(entry.getCreatedAt())
                    .position(position.getAndIncrement())
                    .build();

            if (application != null) {
                response.setApplicationId(application.getId());
                response.setApplicationCode(application.getApplicationCode());
                response.setApplicationStatus(application.getStatus().name());

                if (application.getPreferredBuilding() != null) {
                    response.setPreferredBuildingId(application.getPreferredBuilding().getId());
                    response.setPreferredBuildingName(application.getPreferredBuilding().getName());
                }
            }

            Building effectiveBuilding = targetBuilding != null ? targetBuilding
                    : (application != null ? application.getPreferredBuilding() : null);
            List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(student, application,
                    effectiveBuilding);

            if (!scoredRooms.isEmpty()) {
                ScoredRoom bestRoom = scoredRooms.get(0);
                response.setBestRoomScore(bestRoom.getTotalScore());
                response.setBestMatchRoomId(bestRoom.getRoomId());
                response.setBestMatchRoomNumber(bestRoom.getRoomNumber());
                response.setPriorityScore(bestRoom.getTotalScore());
            } else {
                response.setPriorityScore(BigDecimal.ZERO);
            }

            responses.add(response);
        }

        // Deliberately not re-sorted or renumbered by score. The position assigned during the
        // FIFO walk above is the order the batch job actually consumes the queue in; ranking the
        // display by score meant the student shown at position 1 could lose the last free room
        // to an earlier, lower-scoring applicant (GH-140).
        //
        // priorityScore and bestRoomScore stay on the response as informational columns. They
        // describe how well a student matches the available rooms, not when they will be served.

        log.info("Returning {} waiting list entries with scores", responses.size());
        return responses;
    }

    /**
     * Rescores all waiting list entries using the latest allocation rules.
     * <p>
     * NOTE: The computed scores are transient (not persisted on WaitingListEntry)
     * because the entity has no score/priority column. Scores are recalculated
     * on-the-fly via {@link #getWaitingListWithScores}. This method validates
     * that scoring succeeds for all entries but does not persist results.
     */
    public RescoreResult rescoreWaitingList() {
        log.info("Rescoring all waiting list entries using latest rules");

        List<WaitingListEntry> entries = waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING");

        if (entries.isEmpty()) {
            log.info("No waiting list entries to rescore");
            return RescoreResult.builder()
                    .success(true)
                    .rescored(0)
                    .message("No waiting list entries to rescore")
                    .build();
        }

        int rescored = 0;
        for (WaitingListEntry entry : entries) {
            try {
                Student student = entry.getStudent();
                Application application = entry.getApplication();
                Building preferredBuilding = application != null ? application.getPreferredBuilding() : null;

                allocationScoringService.scoreAvailableRooms(
                        student, application, preferredBuilding);

                rescored++;
            } catch (Exception e) {
                log.error("Failed to rescore entry {} for student {}: {}",
                        entry.getId(), entry.getStudent().getId(), e.getMessage());
            }
        }

        log.info("Rescored {} waiting list entries", rescored);

        return RescoreResult.builder()
                .success(true)
                .rescored(rescored)
                .message("Successfully rescored " + rescored + " waiting list entries")
                .build();
    }
}
