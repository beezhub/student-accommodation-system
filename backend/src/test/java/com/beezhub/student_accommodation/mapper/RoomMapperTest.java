package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomFeature;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RoomMapperTest {

    private final RoomMapper mapper = Mappers.getMapper(RoomMapper.class);

    @Test
    void toResponse_null_returnsNull() {
        assertNull(mapper.toResponse(null));
    }

    @Test
    void toResponse_mapsAllFields_andCollections() {
        Room room = new Room();
        room.setId(10L);
        room.setName("Room A");
        room.setDescription("Nice room");
        room.setCapacity(3);
        room.setAvailable(true);
        room.setCurrentOccupancy(2);
        room.setCreatedAt(LocalDateTime.now().minusDays(1));
        room.setUpdatedAt(LocalDateTime.now());

        Building building = new Building();
        building.setId(77L);
        room.setBuilding(building);

        RoomFeature f1 = new RoomFeature();
        f1.setId(1L);
        RoomFeature f2 = new RoomFeature();
        f2.setId(2L);
        Set<RoomFeature> features = new HashSet<>();
        features.add(f1);
        features.add(f2);
        room.setFeatures(features);

        RoomResponse resp = mapper.toResponse(room);
        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertEquals("Room A", resp.getName());
        assertEquals("Nice room", resp.getDescription());
        assertEquals(3, resp.getCapacity());
        assertTrue(resp.getAvailable());
        assertEquals(2, resp.getCurrentOccupancy());
        assertNotNull(resp.getCreatedAt());
        assertNotNull(resp.getUpdatedAt());
        assertEquals(77L, resp.getBuildingId());
        assertNotNull(resp.getFeatureIds());
        assertEquals(Set.of(1L, 2L), resp.getFeatureIds());
    }

    @Test
    void toEntity_mapsBasicFields() {
        RoomRequest req = new RoomRequest();
        req.setName("X");
        req.setDescription("Y");
        req.setCapacity(4);
        req.setAvailable(false);
        // buildingId and featureIds are not mapped by MapStruct as relations

        Room entity = mapper.toEntity(req);
        assertNotNull(entity);
        assertEquals("X", entity.getName());
        assertEquals("Y", entity.getDescription());
        assertEquals(4, entity.getCapacity());
        assertEquals(false, entity.getAvailable());
    }
}
