package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.mapper.BuildingMapper;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;

    public BuildingResponse createBuilding(BuildingRequest request) {
        Building entity = buildingMapper.toEntity(request);
        Building saved = buildingRepository.save(entity);
        return buildingMapper.toResponse(saved);
    }

    public BuildingResponse getById(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + id));
        return buildingMapper.toResponse(building);
    }

    public List<BuildingResponse> getAll() {
        return buildingMapper.toResponseList(buildingRepository.findAll());
    }

    public BuildingResponse updateBuilding(Long id, BuildingRequest request) {
        Building existing = buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + id));
        existing.setName(request.getName());
        existing.setAddress(request.getAddress());
        existing.setDescription(request.getDescription());
        Building saved = buildingRepository.save(existing);
        return buildingMapper.toResponse(saved);
    }

    public void deleteBuilding(Long id) {
        if (!buildingRepository.existsById(id)) {
            throw new BuildingNotFoundException("Building not found with id: " + id);
        }
        buildingRepository.deleteById(id);
    }
}
