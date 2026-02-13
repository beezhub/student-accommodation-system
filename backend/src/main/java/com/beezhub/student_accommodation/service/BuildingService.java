package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.mapper.BuildingMapper;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.GeocodingResult;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.specification.BuildingSpecification;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final InstitutionRepository institutionRepository;
    private final BuildingMapper buildingMapper;
    private final BuildingImageService buildingImageService;
    private final GoogleMapsService googleMapsService;

    public List<BuildingResponse> getAllBuildings() {
        log.info("Fetching all buildings");
        List<Building> buildings = buildingRepository.findAll();
        List<BuildingResponse> buildingResponses = buildingMapper.toResponseList(buildings);
        buildingResponses.forEach(this::enrichWithPrimaryImage);
        log.info("Found {} buildings", buildingResponses.size());
        return buildingResponses;
    }

    public PageResponse<BuildingResponse> getAllBuildings(Pageable pageable) {
        log.info("Fetching paginated buildings");
        Page<Building> buildings = buildingRepository.findAll(pageable);
        Page<BuildingResponse> responsePage = buildings.map(building -> {
            BuildingResponse response = buildingMapper.toResponse(building);
            enrichWithPrimaryImage(response);
            return response;
        });
        log.info("Found {} buildings in page", buildings.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<BuildingResponse> searchBuildings(String name, String address, Pageable pageable) {
        log.info("Searching buildings with filters - name: {}, address: {}", name, address);
        Specification<Building> spec = BuildingSpecification.filterBy(name, address);
        Page<Building> buildings = buildingRepository.findAll(spec, pageable);
        Page<BuildingResponse> responsePage = buildings.map(building -> {
            BuildingResponse response = buildingMapper.toResponse(building);
            enrichWithPrimaryImage(response);
            return response;
        });
        log.info("Found {} buildings matching criteria", buildings.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public BuildingResponse getBuildingById(Long id) {
        log.info("Fetching building with id: {}", id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + id));
        BuildingResponse response = buildingMapper.toResponse(building);
        enrichWithPrimaryImage(response);
        response.setImages(buildingImageService.getImages(id));
        return response;
    }

    public List<BuildingResponse> getBuildingsByInstitution(Long institutionId) {
        log.info("Fetching buildings for institution id: {}", institutionId);
        // Verify institution exists
        if (!institutionRepository.existsById(institutionId)) {
            throw new ResourceNotFoundException("Institution not found with id: " + institutionId);
        }
        List<Building> buildings = buildingRepository.findByInstitutionId(institutionId);
        List<BuildingResponse> buildingResponses = buildingMapper.toResponseList(buildings);
        buildingResponses.forEach(this::enrichWithPrimaryImage);
        log.info("Found {} buildings for institution {}", buildings.size(), institutionId);
        return buildingResponses;
    }

    public BuildingResponse createBuilding(BuildingRequest request) {
        log.info("Creating new building with name: {}", request.getName());
        Building building = new Building();
        building.setName(request.getName());
        building.setAddress(request.getAddress());
        building.setDescription(request.getDescription());
        building.setTotalFloors(request.getTotalFloors());

        // Geocode the address
        geocodeAndSetLocation(building, request);

        // Handle institution IDs
        if (request.getInstitutionIds() != null && !request.getInstitutionIds().isEmpty()) {
            Set<Institution> institutions = new HashSet<>(institutionRepository.findAllById(request.getInstitutionIds()));
            building.setInstitutions(institutions);
            log.info("Assigned {} institutions to building", institutions.size());
        }

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

        // Geocode the address
        geocodeAndSetLocation(building, request);

        // Handle institution IDs
        if (request.getInstitutionIds() != null) {
            Set<Institution> institutions = new HashSet<>(institutionRepository.findAllById(request.getInstitutionIds()));
            building.setInstitutions(institutions);
            log.info("Updated building with {} institutions", institutions.size());
        } else {
            building.getInstitutions().clear();
        }

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

    private void enrichWithPrimaryImage(BuildingResponse response) {
        if (response.getId() != null) {
            String primaryImageUrl = buildingImageService.getPrimaryImageUrl(response.getId());
            response.setPrimaryImageUrl(primaryImageUrl);
        }
    }

    /**
     * Geocodes the address and sets location data on the building entity.
     * Gracefully handles geocoding failures by logging warnings and continuing.
     */
    private void geocodeAndSetLocation(Building building, BuildingRequest request) {
        GeocodingResult result = null;

        // If place ID is provided (from autocomplete), use it for geocoding
        if (request.getPlaceId() != null && !request.getPlaceId().isEmpty()) {
            log.info("Geocoding building using place ID: {}", request.getPlaceId());
            result = googleMapsService.geocodePlaceId(request.getPlaceId());
        }
        // Otherwise, if address is provided, geocode the address
        else if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            log.info("Geocoding building using address: {}", request.getAddress());
            result = googleMapsService.geocodeAddress(request.getAddress());
        }
        // If coordinates are provided directly (from frontend), use them
        else if (request.getLatitude() != null && request.getLongitude() != null) {
            log.info("Using coordinates provided from frontend");
            building.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            building.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            if (request.getPlaceId() != null) {
                building.setPlaceId(request.getPlaceId());
            }
            return;
        }

        // If geocoding was successful, populate building fields
        if (result != null && result.isSuccess()) {
            building.setLatitude(BigDecimal.valueOf(result.getLatitude()));
            building.setLongitude(BigDecimal.valueOf(result.getLongitude()));
            building.setPlaceId(result.getPlaceId());
            building.setFormattedAddress(result.getFormattedAddress());
            building.setAddressComponents(result.getAddressComponents());
            log.info("Successfully geocoded building: lat={}, lng={}", result.getLatitude(), result.getLongitude());
        } else if (result != null) {
            log.warn("Geocoding failed: {} - Building will be created without coordinates", result.getErrorMessage());
        }
    }
}