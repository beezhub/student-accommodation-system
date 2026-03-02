package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.mapper.BuildingMapper;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private BuildingMapper buildingMapper;
    @Mock
    private BuildingImageService buildingImageService;

    @InjectMocks
    private BuildingService buildingService;

    private Building building;
    private BuildingRequest buildingRequest;
    private Institution institution;

    @BeforeEach
    void setUp() {
        building = new Building();
        building.setId(1L);
        building.setName("Block A");

        institution = new Institution();
        institution.setId(1L);
        institution.setName("Test University");

        buildingRequest = new BuildingRequest();
        buildingRequest.setName("Block A");
        buildingRequest.setAddress("123 Test St");
        buildingRequest.setInstitutionIds(java.util.Arrays.asList(1L));
    }

    @Test
    void createBuilding_shouldCreateBuilding() {
        when(institutionRepository.findAllById(any())).thenReturn(Collections.singletonList(institution));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingMapper.toResponse(any(Building.class))).thenReturn(new BuildingResponse());

        BuildingResponse response = buildingService.createBuilding(buildingRequest);

        assertThat(response).isNotNull();
        verify(buildingRepository).save(any(Building.class));
    }

    @Test
    void getBuildingById_shouldReturnBuilding() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingMapper.toResponse(building)).thenReturn(new BuildingResponse());
        when(buildingImageService.getImages(1L)).thenReturn(Collections.emptyList());

        BuildingResponse response = buildingService.getBuildingById(1L);

        assertThat(response).isNotNull();
    }

    @Test
    void getBuildingById_shouldThrowException_whenNotFound() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildingService.getBuildingById(1L))
                .isInstanceOf(BuildingNotFoundException.class);
    }

    @Test
    void updateBuilding_shouldUpdateBuilding() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(institutionRepository.findAllById(any())).thenReturn(Collections.singletonList(institution));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingMapper.toResponse(any(Building.class))).thenReturn(new BuildingResponse());

        BuildingResponse response = buildingService.updateBuilding(1L, buildingRequest);

        assertThat(response).isNotNull();
        verify(buildingRepository).save(building);
    }

    @Test
    void deleteBuilding_shouldDeleteBuilding() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

        buildingService.deleteBuilding(1L);

        verify(buildingRepository).delete(building);
    }

    @Test
    void searchBuildings_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Building> page = new PageImpl<>(Collections.singletonList(building));

        when(buildingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(buildingMapper.toResponse(any(Building.class))).thenReturn(new BuildingResponse());

        PageResponse<BuildingResponse> response = buildingService.searchBuildings(
                null, null, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAllBuildings_listVersion_returnsAllWithPrimaryImage() {
        BuildingResponse resp = new BuildingResponse();
        resp.setId(1L);
        when(buildingRepository.findAll()).thenReturn(List.of(building));
        when(buildingMapper.toResponseList(any())).thenReturn(List.of(resp));
        when(buildingImageService.getPrimaryImageUrl(1L)).thenReturn("http://img.url");

        List<BuildingResponse> result = buildingService.getAllBuildings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrimaryImageUrl()).isEqualTo("http://img.url");
    }

    @Test
    void getAllBuildings_paginatedVersion_returnsPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        BuildingResponse resp = new BuildingResponse();
        resp.setId(1L);
        Page<Building> page = new PageImpl<>(List.of(building));
        when(buildingRepository.findAll(pageable)).thenReturn(page);
        when(buildingMapper.toResponse(building)).thenReturn(resp);
        when(buildingImageService.getPrimaryImageUrl(1L)).thenReturn(null);

        PageResponse<BuildingResponse> result = buildingService.getAllBuildings(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getBuildingsByInstitution_institutionExists_returnsBuildings() {
        BuildingResponse resp = new BuildingResponse();
        resp.setId(1L);
        when(institutionRepository.existsById(1L)).thenReturn(true);
        when(buildingRepository.findByInstitutionId(1L)).thenReturn(List.of(building));
        when(buildingMapper.toResponseList(any())).thenReturn(List.of(resp));
        when(buildingImageService.getPrimaryImageUrl(1L)).thenReturn(null);

        List<BuildingResponse> result = buildingService.getBuildingsByInstitution(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getBuildingsByInstitution_institutionNotFound_throwsException() {
        when(institutionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> buildingService.getBuildingsByInstitution(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createBuilding_withCoordinates_setsLatitudeLongitude() {
        buildingRequest.setLatitude(51.5);
        buildingRequest.setLongitude(-0.1);
        buildingRequest.setPlaceId("ChIJdd4hrwug2EcRmSrV3Vo6llI");

        when(institutionRepository.findAllById(any())).thenReturn(Collections.singletonList(institution));
        when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> {
            Building b = inv.getArgument(0);
            assertThat(b.getLatitude()).isNotNull();
            assertThat(b.getLongitude()).isNotNull();
            return building;
        });
        when(buildingMapper.toResponse(any(Building.class))).thenReturn(new BuildingResponse());

        BuildingResponse response = buildingService.createBuilding(buildingRequest);

        assertThat(response).isNotNull();
    }

    @Test
    void updateBuilding_nullInstitutionIds_clearsInstitutions() {
        building.setInstitutions(new HashSet<>(Collections.singletonList(institution)));
        buildingRequest.setInstitutionIds(null);

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingRepository.save(any(Building.class))).thenReturn(building);
        when(buildingMapper.toResponse(any(Building.class))).thenReturn(new BuildingResponse());

        buildingService.updateBuilding(1L, buildingRequest);

        assertThat(building.getInstitutions()).isEmpty();
        verify(buildingRepository).save(building);
    }
}
