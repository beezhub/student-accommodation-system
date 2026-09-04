package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.model.dto.BuildingAttributesRequest;
import com.beezhub.student_accommodation.model.dto.BuildingAttributesResponse;
import com.beezhub.student_accommodation.model.dto.RoomAttributesRequest;
import com.beezhub.student_accommodation.model.dto.RoomAttributesResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingAttributes;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.BuildingAttributesRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAttributesRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BuildingAttributesService {

    private final BuildingAttributesRepository buildingAttributesRepository;
    private final RoomAttributesRepository roomAttributesRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;

    // ==================== Building Attributes ====================

    /**
     * Gets building attributes for a specific building.
     *
     * @param buildingId The building ID
     * @return BuildingAttributesResponse or null if not found
     */
    public BuildingAttributesResponse getBuildingAttributes(Long buildingId) {
        log.info("Getting attributes for building {}", buildingId);

        // Verify building exists
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with ID: " + buildingId));

        BuildingAttributes attributes = buildingAttributesRepository.findByBuilding_Id(buildingId)
                .orElse(null);

        if (attributes == null) {
            log.info("No attributes found for building {}", buildingId);
            // Return a response with building info but no attributes
            return BuildingAttributesResponse.builder()
                    .buildingId(building.getId())
                    .buildingName(building.getName())
                    .genderRestriction(GenderRestriction.ANY)
                    .allowMixedGender(false)
                    .build();
        }

        return mapToBuildingAttributesResponse(attributes);
    }

    /**
     * Creates or updates building attributes.
     *
     * @param buildingId The building ID
     * @param request    The attributes request
     * @return BuildingAttributesResponse
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "BUILDING_ATTRIBUTES", description = "Updated attributes for building {0}", entityIdParamIndex = 0)
    public BuildingAttributesResponse updateBuildingAttributes(Long buildingId, BuildingAttributesRequest request) {
        log.info("Updating attributes for building {}", buildingId);

        // Verify building exists
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with ID: " + buildingId));

        // Find existing or create new
        BuildingAttributes attributes = buildingAttributesRepository.findByBuilding_Id(buildingId)
                .orElse(BuildingAttributes.builder()
                        .building(building)
                        .build());

        // Update fields
        if (request.getGenderRestriction() != null) {
            attributes.setGenderRestriction(request.getGenderRestriction());
        }
        if (request.getAccessibilityFeatures() != null) {
            attributes.setAccessibilityFeatures(request.getAccessibilityFeatures());
        }
        if (request.getDesirabilityScore() != null) {
            attributes.setDesirabilityScore(request.getDesirabilityScore());
        }
        if (request.getAllowMixedGender() != null) {
            attributes.setAllowMixedGender(request.getAllowMixedGender());
        }

        BuildingAttributes savedAttributes = buildingAttributesRepository.save(attributes);
        log.info("Saved attributes for building {} with ID {}", buildingId, savedAttributes.getId());

        return mapToBuildingAttributesResponse(savedAttributes);
    }

    // ==================== Room Attributes ====================

    /**
     * Gets room attributes for a specific room.
     *
     * @param roomId The room ID
     * @return RoomAttributesResponse or default if not found
     */
    public RoomAttributesResponse getRoomAttributes(Long roomId) {
        log.info("Getting attributes for room {}", roomId);

        // Verify room exists
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with ID: " + roomId));

        RoomAttributes attributes = roomAttributesRepository.findByRoom_Id(roomId)
                .orElse(null);

        if (attributes == null) {
            log.info("No attributes found for room {}", roomId);
            // Return a response with room info but no attributes
            return RoomAttributesResponse.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .buildingId(room.getBuilding().getId())
                    .buildingName(room.getBuilding().getName())
                    .accessibilityCompliant(false)
                    .build();
        }

        return mapToRoomAttributesResponse(attributes);
    }

    /**
     * Creates or updates room attributes.
     *
     * @param roomId  The room ID
     * @param request The attributes request
     * @return RoomAttributesResponse
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "ROOM_ATTRIBUTES", description = "Updated attributes for room {0}", entityIdParamIndex = 0)
    public RoomAttributesResponse updateRoomAttributes(Long roomId, RoomAttributesRequest request) {
        log.info("Updating attributes for room {}", roomId);

        // Verify room exists
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with ID: " + roomId));

        // Find existing or create new
        RoomAttributes attributes = roomAttributesRepository.findByRoom_Id(roomId)
                .orElse(RoomAttributes.builder()
                        .room(room)
                        .build());

        // Update fields
        if (request.getAccessibilityCompliant() != null) {
            attributes.setAccessibilityCompliant(request.getAccessibilityCompliant());
        }
        if (request.getGenderRestriction() != null) {
            attributes.setGenderRestriction(request.getGenderRestriction());
        }
        // Year of study restrictions
        attributes.setMinYearOfStudy(request.getMinYearOfStudy());
        attributes.setMaxYearOfStudy(request.getMaxYearOfStudy());

        RoomAttributes savedAttributes = roomAttributesRepository.save(attributes);
        log.info("Saved attributes for room {} with ID {}", roomId, savedAttributes.getId());

        return mapToRoomAttributesResponse(savedAttributes);
    }

    /**
     * Gets all room attributes for a building.
     *
     * @param buildingId The building ID
     * @return List of RoomAttributesResponse
     */
    public List<RoomAttributesResponse> getRoomAttributesByBuilding(Long buildingId) {
        log.info("Getting room attributes for building {}", buildingId);

        // Verify building exists
        if (!buildingRepository.existsById(buildingId)) {
            throw new BuildingNotFoundException("Building not found with ID: " + buildingId);
        }

        List<RoomAttributes> attributes = roomAttributesRepository.findByRoom_Building_Id(buildingId);
        log.info("Found {} room attributes for building {}", attributes.size(), buildingId);

        return attributes.stream()
                .map(this::mapToRoomAttributesResponse)
                .collect(Collectors.toList());
    }

    // ==================== Mappers ====================

    private BuildingAttributesResponse mapToBuildingAttributesResponse(BuildingAttributes attributes) {
        return BuildingAttributesResponse.builder()
                .id(attributes.getId())
                .buildingId(attributes.getBuilding().getId())
                .buildingName(attributes.getBuilding().getName())
                .genderRestriction(attributes.getGenderRestriction())
                .accessibilityFeatures(attributes.getAccessibilityFeatures())
                .desirabilityScore(attributes.getDesirabilityScore())
                .allowMixedGender(attributes.getAllowMixedGender())
                .createdAt(attributes.getCreatedAt())
                .updatedAt(attributes.getUpdatedAt())
                .build();
    }

    private RoomAttributesResponse mapToRoomAttributesResponse(RoomAttributes attributes) {
        Room room = attributes.getRoom();
        return RoomAttributesResponse.builder()
                .id(attributes.getId())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .buildingId(room.getBuilding().getId())
                .buildingName(room.getBuilding().getName())
                .accessibilityCompliant(attributes.getAccessibilityCompliant())
                .genderRestriction(attributes.getGenderRestriction())
                .minYearOfStudy(attributes.getMinYearOfStudy())
                .maxYearOfStudy(attributes.getMaxYearOfStudy())
                .createdAt(attributes.getCreatedAt())
                .updatedAt(attributes.getUpdatedAt())
                .build();
    }
}
