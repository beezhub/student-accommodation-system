package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomFeature;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.beezhub.student_accommodation.model.dto.PageDto;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final BuildingRepository buildingRepository;
    private final RoomFeatureRepository roomFeatureRepository;

    public RoomResponse createRoom(RoomRequest roomRequest) {
        Room entity = roomMapper.toEntity(roomRequest);
        if (roomRequest.getBuildingId() != null) {
            buildingRepository.findById(roomRequest.getBuildingId()).ifPresent(entity::setBuilding);
        }
        if (roomRequest.getFeatureIds() != null && !roomRequest.getFeatureIds().isEmpty()) {
            Set<RoomFeature> features = new HashSet<>();
            for (Long fid : roomRequest.getFeatureIds()) {
                roomFeatureRepository.findById(fid).ifPresent(features::add);
            }
            entity.setFeatures(features);
        }
        if (entity.getCurrentOccupancy() == null)
            entity.setCurrentOccupancy(0);
        Room saved = roomRepository.save(entity);
        return roomMapper.toResponse(saved);
    }

    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));
        return roomMapper.toResponse(room);
    }

    public PageDto<RoomResponse> getAllRooms(Pageable pageable) {
        Page<Room> page = roomRepository.findAll(pageable);
        return PageDto.fromPage(page, roomMapper::toResponse);
    }

    public RoomResponse updateRoom(Long id, RoomRequest roomRequest) {
        Room existing = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));

        existing.setName(roomRequest.getName());
        existing.setDescription(roomRequest.getDescription());
        existing.setCapacity(roomRequest.getCapacity());
        existing.setAvailable(roomRequest.getAvailable());
        if (roomRequest.getBuildingId() != null) {
            buildingRepository.findById(roomRequest.getBuildingId()).ifPresent(existing::setBuilding);
        }
        if (roomRequest.getFeatureIds() != null) {
            Set<RoomFeature> features = new   HashSet<>();
            for (Long fid : roomRequest.getFeatureIds()) {
                roomFeatureRepository.findById(fid).ifPresent(features::add);
            }
            existing.setFeatures(features);
        }

        Room saved = roomRepository.save(existing);
        return roomMapper.toResponse(saved);
    }

    public void deleteRoom(Long id) {
        Room existing = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with id: " + id));
        roomRepository.delete(existing);
    }

    public List<RoomResponse> getRoomsByAvailability(boolean available) {
       List<Room> rooms = roomRepository.findByAvailable(available);
        return rooms.stream().map(roomMapper::toResponse).toList();
    }

    public List<RoomResponse> getRoomsByCapacity(int capacity) {
       List<Room> rooms = roomRepository.findByCapacityGreaterThanEqual(capacity);
        return rooms.stream().map(roomMapper::toResponse).toList();
    }
}
