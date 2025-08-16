package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.RoomFeatureNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomFeatureMapper;
import com.beezhub.student_accommodation.model.dto.RoomFeatureRequest;
import com.beezhub.student_accommodation.model.dto.RoomFeatureResponse;
import com.beezhub.student_accommodation.model.entity.RoomFeature;
import com.beezhub.student_accommodation.repository.RoomFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomFeatureService {

    private final RoomFeatureRepository roomFeatureRepository;
    private final RoomFeatureMapper roomFeatureMapper;

    public RoomFeatureResponse create(RoomFeatureRequest request) {
        RoomFeature entity = roomFeatureMapper.toEntity(request);
        RoomFeature saved = roomFeatureRepository.save(entity);
        return roomFeatureMapper.toResponse(saved);
    }

    public RoomFeatureResponse getById(Long id) {
        RoomFeature f = roomFeatureRepository.findById(id)
                .orElseThrow(() -> new RoomFeatureNotFoundException("Room feature not found with id: " + id));
        return roomFeatureMapper.toResponse(f);
    }

    public List<RoomFeatureResponse> getAll() {
        return roomFeatureMapper.toResponseList(roomFeatureRepository.findAll());
    }

    public RoomFeatureResponse update(Long id, RoomFeatureRequest request) {
        RoomFeature existing = roomFeatureRepository.findById(id)
                .orElseThrow(() -> new RoomFeatureNotFoundException("Room feature not found with id: " + id));
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        RoomFeature saved = roomFeatureRepository.save(existing);
        return roomFeatureMapper.toResponse(saved);
    }

    public void delete(Long id) {
        if (!roomFeatureRepository.existsById(id)) {
            throw new RoomFeatureNotFoundException("Room feature not found with id: " + id);
        }
        roomFeatureRepository.deleteById(id);
    }
}
