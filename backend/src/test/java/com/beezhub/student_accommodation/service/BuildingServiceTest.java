package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.mapper.BuildingMapper;
import com.beezhub.student_accommodation.model.dto.BuildingRequest;
import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock private BuildingRepository buildingRepository;
    @Mock private BuildingMapper buildingMapper;

    @InjectMocks private BuildingService buildingService;

    @Test
    void create_and_get_update_delete_flow_basic() {
        BuildingRequest req = new BuildingRequest();
        req.setName("B1");
        req.setAddress("Addr");
        req.setDescription("Desc");

        Building mapped = new Building();
        when(buildingMapper.toEntity(req)).thenReturn(mapped);
        Building saved = new Building(); saved.setId(11L);
        when(buildingRepository.save(mapped)).thenReturn(saved);
        BuildingResponse resp = new BuildingResponse(); resp.setId(11L);
        when(buildingMapper.toResponse(saved)).thenReturn(resp);

        BuildingResponse out = buildingService.createBuilding(req);
        assertEquals(11L, out.getId());

        when(buildingRepository.findById(11L)).thenReturn(Optional.of(saved));
        BuildingResponse getResp = new BuildingResponse(); getResp.setId(11L);
        when(buildingMapper.toResponse(saved)).thenReturn(getResp);
        assertEquals(11L, buildingService.getById(11L).getId());

        // update
        BuildingRequest updateReq = new BuildingRequest();
        updateReq.setName("NB"); updateReq.setAddress("NA"); updateReq.setDescription("ND");
        when(buildingRepository.findById(11L)).thenReturn(Optional.of(saved));
        when(buildingRepository.save(saved)).thenReturn(saved);
        assertEquals(11L, buildingService.updateBuilding(11L, updateReq).getId());

        // getAll
        when(buildingRepository.findAll()).thenReturn(List.of(saved));
        when(buildingMapper.toResponseList(List.of(saved))).thenReturn(List.of(resp));
        assertEquals(1, buildingService.getAll().size());

        // delete
        when(buildingRepository.existsById(11L)).thenReturn(true);
        buildingService.deleteBuilding(11L);
        verify(buildingRepository).deleteById(11L);
    }

    @Test
    void getById_notFound_throws() {
        when(buildingRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(BuildingNotFoundException.class, () -> buildingService.getById(9L));
    }

    @Test
    void delete_nonExisting_throws() {
        when(buildingRepository.existsById(100L)).thenReturn(false);
        assertThrows(BuildingNotFoundException.class, () -> buildingService.deleteBuilding(100L));
    }
}
