package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.mapper.BuildingMapper;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.repository.BuildingAttributesRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
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
    private BuildingAttributesRepository buildingAttributesRepository;
    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BuildingMapper buildingMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private BuildingImageService buildingImageService;
    @Mock
    private com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;
    @Mock
    private com.beezhub.student_accommodation.repository.TenantRepository tenantRepository;


    @InjectMocks
    private BuildingService buildingService;

    private Building building;
    private BuildingRequest buildingRequest;
    private Institution institution;

    /** A property company, as opposed to the platform tenant (0), which cannot own buildings. */
    private static final Long OWNING_TENANT = 42L;

    @BeforeEach
    void setUp() {
        // These tests used to run with no tenant context at all, which reads as a platform admin.
        // Building creation now requires an owning company, so the default caller here is a tenant
        // admin — the ordinary case (GH-213).
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(OWNING_TENANT);

        building = new Building();
        building.setId(1L);
        building.setName("Block A");
        building.setTenantId(OWNING_TENANT);

        institution = new Institution();
        institution.setId(1L);
        institution.setName("Test University");

        buildingRequest = new BuildingRequest();
        buildingRequest.setName("Block A");
        buildingRequest.setAddress("123 Test St");
        buildingRequest.setInstitutionIds(java.util.Arrays.asList(1L));
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenantContext() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.clear();
    }

    // ── GH-213: a building's owning company has to be stated, not inherited from the caller ───

    @Test
    void createBuilding_asTenantAdmin_stampsTheirOwnCompany() {
        when(institutionRepository.findAllById(any())).thenReturn(Collections.singletonList(institution));
        when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> inv.getArgument(0));
        when(buildingMapper.toResponse(any(Building.class))).thenReturn(new BuildingResponse());

        buildingService.createBuilding(buildingRequest);

        org.mockito.ArgumentCaptor<Building> saved = org.mockito.ArgumentCaptor.forClass(Building.class);
        verify(buildingRepository).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(OWNING_TENANT);
    }

    @Test
    void createBuilding_asPlatformAdmin_withoutACompany_isRefused() {
        // The platform admin's own context is 0. Left to TenantListener this stamped the building
        // with the platform tenant, making it invisible to the company that owns it — and with no
        // parent column, nothing could repair it afterwards.
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(0L);

        assertThatThrownBy(() -> buildingService.createBuilding(buildingRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("property company");

        verify(buildingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createBuilding_asPlatformAdmin_namingThePlatformTenant_isRefused() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(0L);
        buildingRequest.setTenantId(0L);

        assertThatThrownBy(() -> buildingService.createBuilding(buildingRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBuilding_asPlatformAdmin_withACompany_stampsThatCompany() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(0L);
        buildingRequest.setTenantId(OWNING_TENANT);
        when(tenantRepository.existsById(OWNING_TENANT)).thenReturn(true);
        when(institutionRepository.findAllById(any())).thenReturn(Collections.singletonList(institution));
        when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> inv.getArgument(0));
        when(buildingMapper.toResponse(any(Building.class))).thenReturn(new BuildingResponse());

        buildingService.createBuilding(buildingRequest);

        org.mockito.ArgumentCaptor<Building> saved = org.mockito.ArgumentCaptor.forClass(Building.class);
        verify(buildingRepository).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(OWNING_TENANT);
    }

    @Test
    void createBuilding_asPlatformAdmin_namingAnUnknownCompany_isRefused() {
        com.beezhub.student_accommodation.service.tenant.TenantContext.setTenantId(0L);
        buildingRequest.setTenantId(999L);
        when(tenantRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> buildingService.createBuilding(buildingRequest))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.TenantNotFoundException.class);
    }

    @Test
    void createBuilding_asTenantAdmin_namingAnotherCompany_isRefused() {
        // Refused rather than ignored, so a mistaken id fails loudly instead of quietly creating
        // the building in the caller's own company.
        buildingRequest.setTenantId(99L);

        assertThatThrownBy(() -> buildingService.createBuilding(buildingRequest))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException.class);
    }

    @Test
    void updateBuilding_cannotMoveTheBuildingToAnotherCompany() {
        buildingRequest.setTenantId(99L);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));

        assertThatThrownBy(() -> buildingService.updateBuilding(1L, buildingRequest))
                .isInstanceOf(com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException.class);
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

    // ── GH-111: findById bypasses the Hibernate tenant filter ────────────────

    @Test
    void updateBuilding_buildingOfAnotherTenant_refusedBeforeAnySave() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        doThrow(new ResourceNotFoundException("Building not found with id: 1"))
                .when(tenantOwnershipGuard).assertOwned(building, "Building", 1L);

        assertThatThrownBy(() -> buildingService.updateBuilding(1L, buildingRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(buildingRepository, never()).save(any(Building.class));
    }

    @Test
    void deleteBuilding_buildingOfAnotherTenant_refusedBeforeDelete() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        doThrow(new ResourceNotFoundException("Building not found with id: 1"))
                .when(tenantOwnershipGuard).assertOwned(building, "Building", 1L);

        assertThatThrownBy(() -> buildingService.deleteBuilding(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(buildingRepository, never()).delete(any(Building.class));
    }
}
