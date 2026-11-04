package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.aspect.LogActivity;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.RoomAttributesRequest;
import com.beezhub.student_accommodation.model.dto.RoomAttributesResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAttributesRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
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
public class RoomAttributesService {

    private final RoomAttributesRepository roomAttributesRepository;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final TenantOwnershipGuard tenantOwnershipGuard;

    /**
     * Gets room attributes by room ID. Creates default attributes if none exist.
     *
     * @param roomId The room ID
     * @return RoomAttributesResponse
     */
    public RoomAttributesResponse getAttributesByRoomId(Long roomId) {
        log.info("Getting attributes for room ID: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        tenantOwnershipGuard.assertReadable(room, "Room", roomId);

        RoomAttributes attributes = roomAttributesRepository.findByRoom_Id(roomId)
                .orElse(null);

        if (attributes == null) {
            // Return default response if no attributes exist
            return RoomAttributesResponse.builder()
                    .roomId(roomId)
                    .roomNumber(room.getRoomNumber())
                    .buildingId(room.getBuilding().getId())
                    .buildingName(room.getBuilding().getName())
                    .accessibilityCompliant(false)
                    .genderRestriction(GenderRestriction.ANY)
                    .build();
        }

        return mapToResponse(attributes);
    }

    /**
     * Gets all room attributes for a building.
     *
     * @param buildingId The building ID
     * @return List of RoomAttributesResponse
     */
    public List<RoomAttributesResponse> getAttributesByBuildingId(Long buildingId) {
        log.info("Getting room attributes for building ID: {}", buildingId);

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with ID: " + buildingId));
        tenantOwnershipGuard.assertReadable(building, "Building", buildingId);

        List<RoomAttributes> attributes = roomAttributesRepository.findByRoom_Building_Id(buildingId);
        return attributes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Creates or updates room attributes.
     *
     * @param request  The attributes request
     * @param username The admin username
     * @return RoomAttributesResponse
     */
    @Transactional
    @LogActivity(action = "UPDATE", entityType = "ROOM_ATTRIBUTES", description = "Updated room attributes for room {0}", entityIdParamIndex = -1)
    public RoomAttributesResponse saveAttributes(RoomAttributesRequest request, String username) {
        log.info("Saving attributes for room ID: {}", request.getRoomId());

        // Validate year range
        if (request.getMinYearOfStudy() != null && request.getMaxYearOfStudy() != null) {
            if (request.getMinYearOfStudy() > request.getMaxYearOfStudy()) {
                throw new IllegalArgumentException("Minimum year of study cannot be greater than maximum year of study");
            }
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));
        tenantOwnershipGuard.assertOwned(room, "Room", request.getRoomId());

        RoomAttributes attributes = roomAttributesRepository.findByRoom_Id(request.getRoomId())
                .orElse(RoomAttributes.builder()
                        .room(room)
                        .accessibilityCompliant(false)
                        .build());

        // Update fields
        if (request.getAccessibilityCompliant() != null) {
            attributes.setAccessibilityCompliant(request.getAccessibilityCompliant());
        }
        if (request.getGenderRestriction() != null) {
            attributes.setGenderRestriction(request.getGenderRestriction());
        }
        attributes.setMinYearOfStudy(request.getMinYearOfStudy());
        attributes.setMaxYearOfStudy(request.getMaxYearOfStudy());

        RoomAttributes saved = roomAttributesRepository.save(attributes);
        log.info("Saved attributes for room ID: {} with ID: {}", request.getRoomId(), saved.getId());

        return mapToResponse(saved);
    }

    /**
     * Bulk updates room attributes for multiple rooms.
     *
     * @param requests List of attribute requests
     * @param username The admin username
     * @return List of RoomAttributesResponse
     */
    @Transactional
    @LogActivity(action = "BULK_UPDATE", entityType = "ROOM_ATTRIBUTES", description = "Bulk updated room attributes for {0} rooms", entityIdParamIndex = -1)
    public List<RoomAttributesResponse> bulkSaveAttributes(List<RoomAttributesRequest> requests, String username) {
        log.info("Bulk saving attributes for {} rooms", requests.size());

        return requests.stream()
                .map(request -> saveAttributes(request, username))
                .collect(Collectors.toList());
    }

    /**
     * Deletes room attributes (resets to default).
     *
     * @param roomId   The room ID
     * @param username The admin username
     */
    @Transactional
    @LogActivity(action = "DELETE", entityType = "ROOM_ATTRIBUTES", description = "Deleted room attributes for room {0}", entityIdParamIndex = 0)
    public void deleteAttributes(Long roomId, String username) {
        log.info("Deleting attributes for room ID: {}", roomId);

        RoomAttributes attributes = roomAttributesRepository.findByRoom_Id(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room attributes not found for room ID: " + roomId));
        Room room = attributes.getRoom();
        tenantOwnershipGuard.assertOwned(room, "Room", roomId);

        // The guard loads the room for a tenant admin, and Room cascades to its attributes. If it
        // still pointed at them, the flush would cancel this delete and report nothing.
        room.setAttributes(null);
        roomAttributesRepository.delete(attributes);
        log.info("Deleted attributes for room ID: {}", roomId);
    }

    /**
     * Maps RoomAttributes entity to RoomAttributesResponse DTO.
     */
    private RoomAttributesResponse mapToResponse(RoomAttributes attributes) {
        return RoomAttributesResponse.builder()
                .id(attributes.getId())
                .roomId(attributes.getRoom().getId())
                .roomNumber(attributes.getRoom().getRoomNumber())
                .buildingId(attributes.getRoom().getBuilding().getId())
                .buildingName(attributes.getRoom().getBuilding().getName())
                .accessibilityCompliant(attributes.getAccessibilityCompliant())
                .genderRestriction(attributes.getGenderRestriction() != null ?
                        attributes.getGenderRestriction() : GenderRestriction.ANY)
                .minYearOfStudy(attributes.getMinYearOfStudy())
                .maxYearOfStudy(attributes.getMaxYearOfStudy())
                .createdAt(attributes.getCreatedAt())
                .updatedAt(attributes.getUpdatedAt())
                .build();
    }
}
