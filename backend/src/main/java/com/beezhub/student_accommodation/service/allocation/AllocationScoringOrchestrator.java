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
import com.beezhub.student_accommodation.service.AllocationScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationScoringOrchestrator {

    private final AllocationScoringService allocationScoringService;
    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomRepository roomRepository;
    private final WaitingListRepository waitingListRepository;
    private final BuildingRepository buildingRepository;

    public Map<Long, List<ScoredRoom>> buildScoringMatrix(List<WaitingListEntry> waitingList) {
        Map<Long, List<ScoredRoom>> scoringMatrix = new HashMap<>();

        for (WaitingListEntry entry : waitingList) {
            Student student = entry.getStudent();

            // Skip if already has active allocation
            if (roomAllocationRepository.existsActiveAllocationByStudentId(student.getId())) {
                continue;
            }

            Application app = entry.getApplication();
            Building preferredBuilding = app != null ? app.getPreferredBuilding() : null;

            List<ScoredRoom> scoredRooms = allocationScoringService.scoreAvailableRooms(
                    student, app, preferredBuilding);

            if (!scoredRooms.isEmpty()) {
                scoringMatrix.put(student.getId(), scoredRooms);
            }
        }

        return scoringMatrix;
    }

    public Map<Long, Integer> buildRoomCapacityMap(List<Room> rooms) {
        Map<Long, Integer> capacityMap = new HashMap<>();

        for (Room room : rooms) {
            long activeCount = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
            int availableSpots = room.getCapacity() - (int) activeCount;
            if (availableSpots > 0) {
                capacityMap.put(room.getId(), availableSpots);
            }
        }

        return capacityMap;
    }

    public List<Room> getAvailableRoomsForBatch() {
        List<Room> allRooms = roomRepository.findAll();
        List<Room> availableRooms = new ArrayList<>();

        for (Room room : allRooms) {
            long activeCount = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
            if (activeCount < room.getCapacity()) {
                availableRooms.add(room);
            }
        }

        return availableRooms;
    }

    public List<WaitingListEntryResponse> getWaitingListWithScores(Long buildingId) {
        log.info("Getting waiting list with scores for building {}", buildingId);

        List<WaitingListEntry> entries = waitingListRepository.findByStatusOrderByCreatedAtAsc("WAITING");
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

        responses.sort(Comparator.comparing(WaitingListEntryResponse::getPriorityScore).reversed());

        for (int i = 0; i < responses.size(); i++) {
            responses.get(i).setPosition(i + 1);
        }

        log.info("Returning {} waiting list entries with scores", responses.size());
        return responses;
    }

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
