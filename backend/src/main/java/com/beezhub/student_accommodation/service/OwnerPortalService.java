package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.OwnerBuildingSummaryDto;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingOwner;
import com.beezhub.student_accommodation.repository.BuildingOwnerRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerPortalService {

    private final BuildingOwnerRepository buildingOwnerRepository;
    private final BuildingRepository buildingRepository;

    /**
     * Task 4.1: Returns buildings owned by the authenticated owner.
     * Scoped by building.owner_id to enforce data isolation.
     */
    public List<OwnerBuildingSummaryDto> getOwnerBuildings(Long ownerProfileId) {
        BuildingOwner owner = buildingOwnerRepository.findById(ownerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found: " + ownerProfileId));

        return buildingRepository.findByOwner_Id(owner.getId())
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private OwnerBuildingSummaryDto toSummary(Building building) {
        int totalRooms = building.getRooms().size();
        long occupiedRooms = building.getRooms().stream()
                .filter(r -> "OCCUPIED".equalsIgnoreCase(r.getStatus()) || "FULL".equalsIgnoreCase(r.getStatus()))
                .count();

        return OwnerBuildingSummaryDto.builder()
                .id(building.getId())
                .name(building.getName())
                .address(building.getFormattedAddress() != null ? building.getFormattedAddress() : building.getAddress())
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .build();
    }
}
