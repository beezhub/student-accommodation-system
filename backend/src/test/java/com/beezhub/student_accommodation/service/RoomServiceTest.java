package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.model.dto.PageDto;
import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomFeature;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomFeatureRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomMapper roomMapper;
    @Mock private BuildingRepository buildingRepository;
    @Mock private RoomFeatureRepository roomFeatureRepository;

    @InjectMocks private RoomService roomService;

    @Test
    void createRoom_setsDefaultOccupancy_andResolvesRelations() {
        RoomRequest req = new RoomRequest();
        req.setName("Room X");
        req.setDescription("Desc");
        req.setCapacity(2);
        req.setAvailable(true);
        req.setBuildingId(5L);
        req.setFeatureIds(Set.of(1L, 2L));

        Room mapped = new Room();
        when(roomMapper.toEntity(req)).thenReturn(mapped);

        Building building = new Building();
        building.setId(5L);
        when(buildingRepository.findById(5L)).thenReturn(Optional.of(building));

        RoomFeature f1 = new RoomFeature(); f1.setId(1L);
        RoomFeature f2 = new RoomFeature(); f2.setId(2L);
        when(roomFeatureRepository.findById(1L)).thenReturn(Optional.of(f1));
        when(roomFeatureRepository.findById(2L)).thenReturn(Optional.of(f2));

        Room saved = new Room();
        saved.setId(99L);
        when(roomRepository.save(any(Room.class))).thenReturn(saved);

        RoomResponse response = new RoomResponse();
        response.setId(99L);
        when(roomMapper.toResponse(saved)).thenReturn(response);

        RoomResponse out = roomService.createRoom(req);

        assertEquals(99L, out.getId());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        Room toSave = captor.getValue();
        assertEquals(0, toSave.getCurrentOccupancy(), "currentOccupancy should default to 0 when null");
        assertNotNull(toSave.getBuilding());
        assertEquals(2, toSave.getFeatures().size());

        verify(roomMapper).toEntity(req);
        verify(roomMapper).toResponse(saved);
    }

    @Test
    void getRoomById_notFound_throws() {
        when(roomRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(RoomNotFoundException.class, () -> roomService.getRoomById(7L));
    }

    @Test
    void getAllRooms_mapsToPageDto() {
        Room r = new Room();
        r.setId(1L);
        Page<Room> page = new PageImpl<>(List.of(r), PageRequest.of(0, 1), 1);
        when(roomRepository.findAll(any(PageRequest.class))).thenReturn(page);
        RoomResponse rr = new RoomResponse(); rr.setId(1L);
        when(roomMapper.toResponse(r)).thenReturn(rr);

        PageDto<RoomResponse> dto = roomService.getAllRooms(PageRequest.of(0,1));

        assertNotNull(dto);
        assertEquals(1, dto.getContent().size());
        assertEquals(1, dto.getTotalElements());
        assertTrue(dto.isFirst());
    }

    @Test
    void updateRoom_updatesFieldsAndRelations() {
        Room existing = new Room(); existing.setId(5L);
        when(roomRepository.findById(5L)).thenReturn(Optional.of(existing));

        RoomRequest req = new RoomRequest();
        req.setName("N"); req.setDescription("D"); req.setCapacity(4); req.setAvailable(false);
        req.setBuildingId(2L); req.setFeatureIds(Set.of(7L));

        Building b = new Building(); b.setId(2L);
        when(buildingRepository.findById(2L)).thenReturn(Optional.of(b));
        RoomFeature f = new RoomFeature(); f.setId(7L);
        when(roomFeatureRepository.findById(7L)).thenReturn(Optional.of(f));

        Room saved = new Room(); saved.setId(5L);
        when(roomRepository.save(existing)).thenReturn(saved);
        RoomResponse rr = new RoomResponse(); rr.setId(5L);
        when(roomMapper.toResponse(saved)).thenReturn(rr);

        RoomResponse out = roomService.updateRoom(5L, req);
        assertEquals(5L, out.getId());
        assertEquals("N", existing.getName());
        assertEquals(1, existing.getFeatures().size());
        assertEquals(b, existing.getBuilding());
    }

    @Test
    void deleteRoom_deletesWhenExists() {
        Room existing = new Room(); existing.setId(3L);
        when(roomRepository.findById(3L)).thenReturn(Optional.of(existing));
        roomService.deleteRoom(3L);
        verify(roomRepository).delete(existing);
    }

    @Test
    void getRoomsByAvailability_andCapacity_mapResponses() {
        Room r1 = new Room(); r1.setId(1L);
        when(roomRepository.findByAvailable(true)).thenReturn(List.of(r1));
        RoomResponse resp1 = new RoomResponse(); resp1.setId(1L);
        when(roomMapper.toResponse(r1)).thenReturn(resp1);
        var list1 = roomService.getRoomsByAvailability(true);
        assertEquals(1, list1.size());

        Room r2 = new Room(); r2.setId(2L);
        when(roomRepository.findByCapacityGreaterThanEqual(3)).thenReturn(List.of(r2));
        RoomResponse resp2 = new RoomResponse(); resp2.setId(2L);
        when(roomMapper.toResponse(r2)).thenReturn(resp2);
        var list2 = roomService.getRoomsByCapacity(3);
        assertEquals(1, list2.size());
    }
}
