package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.StagedAllocationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.StagedAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StagedAllocationMapper {

    private final ObjectMapper objectMapper;
    private final RoomAllocationRepository roomAllocationRepository;

    public StagedAllocationResponse toResponse(StagedAllocation staged) {
        Student student = staged.getStudent();
        Room room = staged.getRoom();
        Application application = staged.getApplication();

        long currentOccupancy = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());

        return StagedAllocationResponse.builder()
                .id(staged.getId())
                .status(staged.getStatus())
                .score(staged.getScore())
                .warnings(deserializeWarnings(staged.getWarnings()))
                .batchId(staged.getBatchId())
                .createdAt(staged.getCreatedAt())
                .updatedAt(staged.getUpdatedAt())
                .confirmedAt(staged.getConfirmedAt())
                .studentId(student.getId())
                .studentNumber(student.getStudentNumber())
                .studentFirstName(student.getAppUser().getFirstName())
                .studentLastName(student.getAppUser().getLastName())
                .studentEmail(student.getAppUser().getEmail())
                .studentGender(student.getGender())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomCapacity(room.getCapacity())
                .currentOccupancy((int) currentOccupancy)
                .buildingId(room.getBuilding() != null ? room.getBuilding().getId() : null)
                .buildingName(room.getBuilding() != null ? room.getBuilding().getName() : null)
                .applicationId(application != null ? application.getId() : null)
                .applicationCode(application != null ? application.getApplicationCode() : null)
                .applicationStatus(application != null ? application.getStatus().name() : null)
                .proposedById(staged.getProposedBy() != null ? staged.getProposedBy().getId() : null)
                .proposedByName(staged.getProposedBy() != null
                        ? staged.getProposedBy().getFirstName() + " " + staged.getProposedBy().getLastName()
                        : null)
                .build();
    }

    public String serializeWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(warnings);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize warnings", e);
            return String.join("; ", warnings);
        }
    }

    private List<String> deserializeWarnings(String warningsJson) {
        if (warningsJson == null || warningsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(warningsJson, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize warnings", e);
            return List.of(warningsJson);
        }
    }
}
