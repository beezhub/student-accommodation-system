package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.mapper.BuildingMapper;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.BuildingAttributesRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.specification.BuildingBrowseSpecification;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingAttributesRepository buildingAttributesRepository;
    private final InstitutionRepository institutionRepository;
    private final RoomRepository roomRepository;
    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final BuildingImageService buildingImageService;
    private final com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    public List<BuildingResponse> getAllBuildings() {
        log.info("Fetching all buildings");
        List<Building> buildings = buildingRepository.findAll();
        List<BuildingResponse> buildingResponses = buildingMapper.toResponseList(buildings);
        buildingResponses.forEach(this::enrichWithPrimaryImage);
        enrichWithPricingAndAvailability(buildingResponses);
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
        List<BuildingResponse> content = responsePage.getContent();
        enrichWithPricingAndAvailability(content);
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
        List<BuildingResponse> content = responsePage.getContent();
        enrichWithPricingAndAvailability(content);
        log.info("Found {} buildings matching criteria", buildings.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public PageResponse<BuildingResponse> browseBuildings(
            BigDecimal minPrice, BigDecimal maxPrice,
            Boolean available, Long institutionId, String search,
            Pageable pageable) {
        log.info("Browsing buildings with filters - minPrice: {}, maxPrice: {}, available: {}, institutionId: {}, search: {}",
                minPrice, maxPrice, available, institutionId, search);
        Specification<Building> spec = BuildingBrowseSpecification.filterBy(
                minPrice, maxPrice, available, institutionId, search);
        Page<Building> buildings = buildingRepository.findAll(spec, pageable);
        Page<BuildingResponse> responsePage = buildings.map(building -> {
            BuildingResponse response = buildingMapper.toResponse(building);
            enrichWithPrimaryImage(response);
            return response;
        });
        List<BuildingResponse> content = responsePage.getContent();
        enrichWithPricingAndAvailability(content);
        log.info("Found {} buildings matching browse criteria", buildings.getNumberOfElements());
        return PageMapper.toPageResponse(responsePage);
    }

    public BuildingResponse getBuildingById(Long id) {
        log.info("Fetching building with id: {}", id);
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + id));
        BuildingResponse response = buildingMapper.toResponse(building);
        enrichWithPrimaryImage(response);
        response.setImages(buildingImageService.getImages(id));

        // Enrich with room summaries for detail view
        List<Room> rooms = roomRepository.findByBuildingIdWithAttributes(id);
        response.setRooms(roomMapper.toResponseList(rooms));

        // Enrich single building with pricing/availability
        enrichSingleBuildingWithPricingAndAvailability(response, rooms);

        // Enrich with accessibility features from building attributes
        buildingAttributesRepository.findByBuilding_Id(id).ifPresent(attrs -> {
            response.setAccessibilityFeatures(attrs.getAccessibilityFeatures());
        });

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
        enrichWithPricingAndAvailability(buildingResponses);
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

        // Use coordinates directly from frontend
        if (request.getLatitude() != null && request.getLongitude() != null) {
            building.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            building.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            building.setPlaceId(request.getPlaceId());
        }

        // Handle institution IDs
        if (request.getInstitutionIds() != null && !request.getInstitutionIds().isEmpty()) {
            Set<Institution> institutions = new HashSet<>(
                    institutionRepository.findAllById(request.getInstitutionIds()));
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
        tenantOwnershipGuard.assertOwned(building, "Building", id);
        building.setName(request.getName());
        building.setAddress(request.getAddress());
        building.setDescription(request.getDescription());
        building.setTotalFloors(request.getTotalFloors());

        // Use coordinates directly from frontend
        if (request.getLatitude() != null && request.getLongitude() != null) {
            building.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            building.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            building.setPlaceId(request.getPlaceId());
        }

        // Handle institution IDs
        if (request.getInstitutionIds() != null) {
            Set<Institution> institutions = new HashSet<>(
                    institutionRepository.findAllById(request.getInstitutionIds()));
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
        tenantOwnershipGuard.assertOwned(building, "Building", id);
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
     * Batch-enriches a list of building responses with pricing and availability summary.
     * Uses two bulk queries to avoid N+1 problem.
     */
    private void enrichWithPricingAndAvailability(List<BuildingResponse> responses) {
        if (responses.isEmpty()) return;

        // Batch query: pricing summaries
        Map<Long, BigDecimal[]> pricingMap = new HashMap<>();
        for (Object[] row : roomRepository.findPricingSummaryByBuilding()) {
            Long buildingId = (Long) row[0];
            BigDecimal min = (BigDecimal) row[1];
            BigDecimal max = (BigDecimal) row[2];
            pricingMap.put(buildingId, new BigDecimal[]{min, max});
        }

        // Batch query: available room counts
        Map<Long, Long> availabilityMap = new HashMap<>();
        for (Object[] row : roomRepository.findAvailableRoomCountByBuilding()) {
            Long buildingId = (Long) row[0];
            Long count = (Long) row[1];
            availabilityMap.put(buildingId, count);
        }

        // Apply to each response
        for (BuildingResponse response : responses) {
            BigDecimal[] pricing = pricingMap.get(response.getId());
            if (pricing != null) {
                response.setMinMonthlyRate(pricing[0]);
                response.setMaxMonthlyRate(pricing[1]);
            }
            Long availableCount = availabilityMap.getOrDefault(response.getId(), 0L);
            response.setAvailableRoomCount(availableCount.intValue());
        }
    }

    /**
     * Enriches a single building response with pricing and availability from pre-loaded rooms.
     */
    private void enrichSingleBuildingWithPricingAndAvailability(BuildingResponse response, List<Room> rooms) {
        BigDecimal min = null;
        BigDecimal max = null;
        int availableCount = 0;

        for (Room room : rooms) {
            if (room.getMonthlyRate() != null) {
                if (min == null || room.getMonthlyRate().compareTo(min) < 0) {
                    min = room.getMonthlyRate();
                }
                if (max == null || room.getMonthlyRate().compareTo(max) > 0) {
                    max = room.getMonthlyRate();
                }
            }
            if ("AVAILABLE".equals(room.getStatus())) {
                availableCount++;
            }
        }

        response.setMinMonthlyRate(min);
        response.setMaxMonthlyRate(max);
        response.setAvailableRoomCount(availableCount);
    }
}