package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.dto.AllocationEligibilityResponse;
import com.beezhub.student_accommodation.model.dto.EligibleRoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.LegacyScoringStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationEligibilityService {

    private final StudentRepository studentRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;
    private final BuildingRepository buildingRepository;
    private final RoomAttributesRepository roomAttributesRepository;
    private final LegacyScoringStrategy allocationScoringService;
    private final AllocationAccessGuard allocationAccessGuard;
    private final StayCapacityGuard stayCapacityGuard;

    /** Applications a room can be offered for. */
    private static final java.util.Set<ApplicationStatus> OFFERABLE =
            java.util.EnumSet.of(ApplicationStatus.APPROVED, ApplicationStatus.AWAITING_ALLOCATION);

    public AllocationEligibilityResponse checkStudentEligibility(Long studentId) {
        log.info("Checking allocation eligibility for student: {}", studentId);
        allocationAccessGuard.assertCanAccessStudent(studentId);

        studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        AllocationEligibilityResponse response = new AllocationEligibilityResponse();
        response.setStudentId(studentId);
        response.setEligible(true);

        // Only an allocation for overlapping dates stands in the way (GH-496): a 2026 resident is
        // eligible for a 2027 room. Stays that have ended are never offered.
        List<StayPeriod> stays = offerableStays(studentId);
        boolean hasActiveAllocation = stays.isEmpty()
                ? holdsBedDuring(studentId, StayPeriod.UNBOUNDED)
                : stays.stream().allMatch(stay -> holdsBedDuring(studentId, stay));
        response.setHasActiveAllocation(hasActiveAllocation);

        if (hasActiveAllocation) {
            response.setEligible(false);
            response.setReason("Student already has an active room allocation");
            log.info("Student {} is not eligible: already has active allocation", studentId);
            return response;
        }

        boolean hasApprovedApplication = applicationRepository.existsByStudent_IdAndStatus(
                studentId, ApplicationStatus.APPROVED);
        response.setHasApprovedApplication(hasApprovedApplication);

        response.setReason("Student is eligible for room allocation");
        log.info("Student {} is eligible for room allocation", studentId);
        return response;
    }

    /** The room checks read lazy fields of the student, so the student must stay in the session. */
    @Transactional(readOnly = true)
    public List<EligibleRoomResponse> getEligibleRoomsForStudent(Long studentId, Long buildingId) {
        log.info("Getting eligible rooms for student: {}, building: {}", studentId, buildingId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        Building building = null;
        if (buildingId != null) {
            building = buildingRepository.findById(buildingId).orElse(null);
        }

        // For the stay the student is waiting for, the earliest one that has not ended.
        StayPeriod stay = offerableStays(studentId).stream().findFirst().orElse(StayPeriod.UNBOUNDED);
        List<Room> eligibleRooms = allocationScoringService.getEligibleRoomsForStudent(student, building, stay);

        List<EligibleRoomResponse> responses = eligibleRooms.stream()
                .map(room -> mapToEligibleRoomResponse(room, stay))
                .collect(Collectors.toList());

        log.info("Found {} eligible rooms for student {}", responses.size(), studentId);
        return responses;
    }

    /**
     * The stays of the student's applications that a room can be offered for, earliest first,
     * leaving out any that have ended.
     */
    private List<StayPeriod> offerableStays(Long studentId) {
        java.time.LocalDate today = stayCapacityGuard.today();
        return applicationRepository.findByStudent_Id(studentId).stream()
                .filter(app -> OFFERABLE.contains(app.getStatus()))
                .map(StayPeriod::of)
                .filter(stay -> !stay.hasEnded(today))
                .sorted(java.util.Comparator.comparing(StayPeriod::from))
                .collect(Collectors.toList());
    }

    private boolean holdsBedDuring(Long studentId, StayPeriod stay) {
        return roomAllocationRepository.existsLiveAllocationForStudentDuring(studentId, stay.from(), stay.until());
    }

    private EligibleRoomResponse mapToEligibleRoomResponse(Room room, StayPeriod stay) {
        long activeAllocations = stayCapacityGuard.bedsTaken(room, stay);
        int availableSpots = room.getCapacity() - (int) activeAllocations;

        // Get room attributes via repository lookup
        RoomAttributes roomAttrs = roomAttributesRepository.findByRoom_Id(room.getId()).orElse(null);

        return EligibleRoomResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .capacity(room.getCapacity())
                .currentOccupancy((int) activeAllocations)
                .availableSpots(Math.max(0, availableSpots))
                .status(room.getStatus())
                .buildingId(room.getBuilding() != null ? room.getBuilding().getId() : null)
                .buildingName(room.getBuilding() != null ? room.getBuilding().getName() : null)
                .buildingAddress(room.getBuilding() != null ? room.getBuilding().getAddress() : null)
                .genderRestriction(roomAttrs != null && roomAttrs.getGenderRestriction() != null
                        ? roomAttrs.getGenderRestriction().name()
                        : null)
                .accessibilityCompliant(roomAttrs != null ? roomAttrs.getAccessibilityCompliant() : null)
                .minYearOfStudy(roomAttrs != null ? roomAttrs.getMinYearOfStudy() : null)
                .maxYearOfStudy(roomAttrs != null ? roomAttrs.getMaxYearOfStudy() : null)
                .build();
    }
}
