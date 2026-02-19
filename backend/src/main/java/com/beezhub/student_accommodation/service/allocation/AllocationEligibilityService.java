package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.dto.AllocationEligibilityResponse;
import com.beezhub.student_accommodation.model.dto.EligibleRoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.AllocationScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final AllocationScoringService allocationScoringService;

    public AllocationEligibilityResponse checkStudentEligibility(Long studentId) {
        log.info("Checking allocation eligibility for student: {}", studentId);

        studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        AllocationEligibilityResponse response = new AllocationEligibilityResponse();
        response.setStudentId(studentId);
        response.setEligible(true);

        boolean hasActiveAllocation = roomAllocationRepository.existsActiveAllocationByStudentId(studentId);
        response.setHasActiveAllocation(hasActiveAllocation);

        if (hasActiveAllocation) {
            response.setEligible(false);
            response.setReason("Student already has an active room allocation");
            log.info("Student {} is not eligible: already has active allocation", studentId);
            return response;
        }

        boolean hasApprovedApplication = applicationRepository.existsByStudent_IdAndStatus(
                studentId,
                com.beezhub.student_accommodation.model.enums.ApplicationStatus.APPROVED);
        response.setHasApprovedApplication(hasApprovedApplication);

        response.setReason("Student is eligible for room allocation");
        log.info("Student {} is eligible for room allocation", studentId);
        return response;
    }

    public List<EligibleRoomResponse> getEligibleRoomsForStudent(Long studentId, Long buildingId) {
        log.info("Getting eligible rooms for student: {}, building: {}", studentId, buildingId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + studentId));

        Building building = null;
        if (buildingId != null) {
            building = buildingRepository.findById(buildingId).orElse(null);
        }

        List<Room> eligibleRooms = allocationScoringService.getEligibleRoomsForStudent(student, building);

        List<EligibleRoomResponse> responses = eligibleRooms.stream()
                .map(this::mapToEligibleRoomResponse)
                .collect(Collectors.toList());

        log.info("Found {} eligible rooms for student {}", responses.size(), studentId);
        return responses;
    }

    private EligibleRoomResponse mapToEligibleRoomResponse(Room room) {
        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
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
