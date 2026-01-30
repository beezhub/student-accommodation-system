package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.mapper.RoomMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
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

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private Building building;
    private RoomRequest roomRequest;

    @BeforeEach
    void setUp() {
        building = new Building();
        building.setId(1L);
        building.setName("Block A");

        room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setCapacity(2);
        room.setStatus("AVAILABLE");
        room.setBuilding(building);

        roomRequest = new RoomRequest();
        roomRequest.setRoomNumber("101");
        roomRequest.setCapacity(2);
        roomRequest.setBuildingId(1L);
    }

    @Test
    void createRoom_shouldCreateRoom() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toResponse(any(Room.class))).thenReturn(new RoomResponse());

        RoomResponse response = roomService.createRoom(roomRequest);

        assertThat(response).isNotNull();
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createRoom_shouldThrowException_whenBuildingNotFound() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom(roomRequest))
                .isInstanceOf(BuildingNotFoundException.class);
    }

    @Test
    void updateRoom_shouldUpdateRoom() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toResponse(any(Room.class))).thenReturn(new RoomResponse());

        RoomResponse response = roomService.updateRoom(1L, roomRequest);

        assertThat(response).isNotNull();
        verify(roomRepository).save(room);
    }

    @Test
    void deleteRoom_shouldDeleteRoom() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        roomService.deleteRoom(1L);

        verify(roomRepository).delete(room);
    }

    @Test
    void searchRooms_shouldReturnPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(Collections.singletonList(room));
        
        when(roomRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(roomMapper.toResponse(any(Room.class))).thenReturn(new RoomResponse());

        PageResponse<RoomResponse> response = roomService.searchRooms(
                null, null, null, null, false, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchRooms_onlyAvailable_shouldFilterResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(Collections.singletonList(room));
        
        when(roomRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(roomAllocationRepository.countActiveAllocationsByRoomId(1L)).thenReturn(0L);
        when(roomMapper.toResponse(any(Room.class))).thenReturn(new RoomResponse());

        PageResponse<RoomResponse> response = roomService.searchRooms(
                null, null, null, null, true, pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
    }
}
