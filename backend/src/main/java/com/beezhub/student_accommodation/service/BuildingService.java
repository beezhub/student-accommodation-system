package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.mapper.BuildingMapper;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.specification.BuildingSpecification;
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
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;

    public List<BuildingResponse> getAllBuildings() {
        log.info("Fetching all buildings");
        List<Building> buildings = buildingRepository.findAll();
        List<BuildingResponse> buildingResponses = buildingMapper.toResponseList(buildings);
        log.info("Found {} buildings", buildingResponses.size());
        return buildingResponses;
    }

    public PageResponse<BuildingResponse> getAllBuildings(Pageable pageable) {
        log.info("Fetching paginated buildings");
        Page<Building> buildings = buildingRepository.findAll(pageable);
        Page<BuildingResponse> responsePage = buildings.map(buildingMapper::toResponse);
        log.info("Found {} buildings in page", buildings.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<BuildingResponse> searchBuildings(String name, String address, Pageable pageable) {
        log.info("Searching buildings with filters - name: {}, address: {}", name, address);
        Specification<Building> spec = BuildingSpecification.filterBy(name, address);
        Page<Building> buildings = buildingRepository.findAll(spec, pageable);
        Page<BuildingResponse> responsePage = buildings.map(buildingMapper::toResponse);
        log.info("Found {} buildings matching criteria", buildings.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public BuildingResponse getBuildingById(Long id) {
        log.info("Fetching building with id: {}", id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + id));
        return buildingMapper.toResponse(building);
    }

    public BuildingResponse createBuilding(BuildingRequest request) {
        log.info("Creating new building with name: {}", request.getName());
        Building building = new Building();
        building.setName(request.getName());
        building.setAddress(request.getAddress());
        building.setDescription(request.getDescription());
        building.setTotalFloors(request.getTotalFloors());
        Building saved = buildingRepository.save(building);
        log.info("Successfully created building with id: {}", saved.getId());
        return buildingMapper.toResponse(saved);
    }

    public BuildingResponse updateBuilding(Long id, BuildingRequest request) {
        log.info("Updating building with id: {}", id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + id));
        building.setName(request.getName());
        building.setAddress(request.getAddress());
        building.setDescription(request.getDescription());
        building.setTotalFloors(request.getTotalFloors());
        Building updated = buildingRepository.save(building);
        log.info("Successfully updated building with id: {}", id);
        return buildingMapper.toResponse(updated);
    }

    public void deleteBuilding(Long id) {
        log.info("Deleting building with id: {}", id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + id));
        buildingRepository.delete(building);
        log.info("Successfully deleted building with id: {}", id);
    }
}