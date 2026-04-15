package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.specification.RoomSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final RoomMapper roomMapper;

    public List<RoomResponse> getAllRooms() {
        log.info("Fetching all rooms");
        List<Room> rooms = roomRepository.findAll();
        List<RoomResponse> roomResponses = roomMapper.toResponseList(rooms);
        log.info("Found {} rooms", roomResponses.size());
        return roomResponses;
    }

    public PageResponse<RoomResponse> getAllRooms(Pageable pageable) {
        log.info("Fetching paginated rooms");
        Page<Room> rooms = roomRepository.findAll(pageable);
        Page<RoomResponse> responsePage = rooms.map(roomMapper::toResponse);
        log.info("Found {} rooms in page", rooms.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<RoomResponse> searchRooms(String roomNumber, Long buildingId, Integer minCapacity,
            Integer maxCapacity, Boolean onlyAvailable, Pageable pageable) {
        log.info(
                "Searching rooms with filters - roomNumber: {}, buildingId: {}, minCapacity: {}, maxCapacity: {}, onlyAvailable: {}",
                roomNumber, buildingId, minCapacity, maxCapacity, onlyAvailable);
        Specification<Room> spec = RoomSpecification.filterBy(roomNumber, buildingId, minCapacity, maxCapacity);
        Page<Room> rooms = roomRepository.findAll(spec, pageable);

        // Filter for available rooms if requested
        if (Boolean.TRUE.equals(onlyAvailable)) {
            List<Room> availableRooms = rooms.getContent().stream()
                    .filter(room -> {
                        long activeAllocations = roomAllocationRepository.countActiveAllocationsByRoomId(room.getId());
                        return activeAllocations < room.getCapacity();
                    })
                    .collect(Collectors.toList());
            Page<Room> availableRoomsPage = new PageImpl<>(availableRooms, pageable, availableRooms.size());
            Page<RoomResponse> responsePage = availableRoomsPage.map(roomMapper::toResponse);
            log.info("Found {} available rooms matching criteria", availableRooms.size());
            return PageMapper.toPageResponse(responsePage);
        }

        Page<RoomResponse> responsePage = rooms.map(roomMapper::toResponse);
        log.info("Found {} rooms matching criteria", rooms.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public RoomResponse getRoomById(Long id) {
        log.info("Fetching room with id: {}", id);
        Room room = roomRepository.findByIdWithAttributes(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));
        return roomMapper.toResponse(room);
    }

    public RoomResponse createRoom(RoomRequest request) {
        log.info("Creating new room with room number: {}", request.getRoomNumber());
        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());
        room.setCurrentOccupancy(0);
        room.setStatus(request.getStatus() != null ? request.getStatus() : "AVAILABLE");
        room.setMonthlyRate(request.getMonthlyRate());

        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new BuildingNotFoundException(
                            "Building not found with id: " + request.getBuildingId()));
            if (roomRepository.existsByRoomNumberAndBuilding_Id(request.getRoomNumber(), request.getBuildingId())) {
                throw new DuplicateResourceException(
                        "A room with this number already exists in this building");
            }
            room.setBuilding(building);
        }

        Room saved = roomRepository.save(room);
        log.info("Successfully created room with id: {}", saved.getId());
        return roomMapper.toResponse(saved);
    }

    public List<RoomResponse> createBulkRooms(List<RoomRequest> requests) {
        log.info("Bulk creating {} rooms", requests.size());

        // Cache building lookups to avoid repeated queries
        Map<Long, Building> buildingCache = new HashMap<>();

        // Batch duplicate detection: group requested room numbers by buildingId and query in bulk
        Map<Long, List<String>> roomNumbersByBuilding = requests.stream()
                .filter(r -> r.getBuildingId() != null)
                .collect(Collectors.groupingBy(
                        RoomRequest::getBuildingId,
                        Collectors.mapping(RoomRequest::getRoomNumber, Collectors.toList())));

        Map<Long, Set<String>> existingByBuilding = new HashMap<>();
        for (Map.Entry<Long, List<String>> entry : roomNumbersByBuilding.entrySet()) {
            existingByBuilding.put(entry.getKey(),
                    roomRepository.findRoomNumbersByBuildingIdAndRoomNumberIn(entry.getKey(), entry.getValue()));
        }

        // Track room numbers added within this batch to detect intra-batch duplicates
        Map<Long, Set<String>> addedInBatch = new HashMap<>();

        List<RoomResponse> responses = new java.util.ArrayList<>();
        for (RoomRequest request : requests) {
            Room room = new Room();
            room.setRoomNumber(request.getRoomNumber());
            room.setCapacity(request.getCapacity());
            room.setCurrentOccupancy(0);
            room.setStatus(request.getStatus() != null ? request.getStatus() : "AVAILABLE");
            room.setMonthlyRate(request.getMonthlyRate());

            if (request.getBuildingId() != null) {
                Building building = buildingCache.computeIfAbsent(request.getBuildingId(),
                        id -> buildingRepository.findById(id)
                                .orElseThrow(() -> new BuildingNotFoundException(
                                        "Building not found with id: " + id)));
                Set<String> existing = existingByBuilding.getOrDefault(request.getBuildingId(), Set.of());
                Set<String> inBatch = addedInBatch.computeIfAbsent(request.getBuildingId(), k -> new HashSet<>());
                if (existing.contains(request.getRoomNumber()) || inBatch.contains(request.getRoomNumber())) {
                    throw new DuplicateResourceException(
                            "A room with this number already exists in this building");
                }
                inBatch.add(request.getRoomNumber());
                room.setBuilding(building);
            }

            Room saved = roomRepository.save(room);
            responses.add(roomMapper.toResponse(saved));
        }

        log.info("Successfully bulk created {} rooms", responses.size());
        return responses;
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        log.info("Updating room with id: {} and buildingId: {}", id, request.getBuildingId());
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));

        // Update basic fields
        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());
        room.setStatus(request.getStatus() != null ? request.getStatus() : "AVAILABLE");
        room.setMonthlyRate(request.getMonthlyRate());

        // Handle building relationship
        if (request.getBuildingId() != null) {
            log.info("Fetching building with id: {}", request.getBuildingId());
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new BuildingNotFoundException(
                            "Building not found with id: " + request.getBuildingId()));
            if (roomRepository.existsByRoomNumberAndBuilding_IdAndIdNot(
                    request.getRoomNumber(), request.getBuildingId(), id)) {
                throw new DuplicateResourceException(
                        "A room with this number already exists in this building");
            }
            room.setBuilding(building);
            log.info("Building set to: {}", building.getName());
        } else {
            log.info("Removing building association");
            room.setBuilding(null);
        }

        Room updated = roomRepository.save(room);
        log.info("Successfully updated room with id: {}, room number: {}", updated.getId(), updated.getRoomNumber());
        return roomMapper.toResponse(updated);
    }

    public void deleteRoom(Long id) {
        log.info("Deleting room with id: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));
        roomRepository.delete(room);
        log.info("Successfully deleted room with id: {}", id);
    }
}
