package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.specification.RoomSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
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

    public PageResponse<RoomResponse> searchRooms(String roomNumber, Long buildingId, Integer minCapacity, Integer maxCapacity, Pageable pageable) {
        log.info("Searching rooms with filters - roomNumber: {}, buildingId: {}, minCapacity: {}, maxCapacity: {}",
                roomNumber, buildingId, minCapacity, maxCapacity);
        Specification<Room> spec = RoomSpecification.filterBy(roomNumber, buildingId, minCapacity, maxCapacity);
        Page<Room> rooms = roomRepository.findAll(spec, pageable);
        Page<RoomResponse> responsePage = rooms.map(roomMapper::toResponse);
        log.info("Found {} rooms matching criteria", rooms.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public RoomResponse getRoomById(Long id) {
        log.info("Fetching room with id: {}", id);
        Room room = roomRepository.findById(id)
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

        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + request.getBuildingId()));
            room.setBuilding(building);
        }

        Room saved = roomRepository.save(room);
        log.info("Successfully created room with id: {}", saved.getId());
        return roomMapper.toResponse(saved);
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        log.info("Updating room with id: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));

        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());

        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }

        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + request.getBuildingId()));
            room.setBuilding(building);
        } else {
            room.setBuilding(null);
        }

        Room updated = roomRepository.save(room);
        log.info("Successfully updated room with id: {}", id);
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
