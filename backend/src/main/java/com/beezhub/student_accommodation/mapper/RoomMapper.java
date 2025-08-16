package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.RoomRequest;
import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Room;
import org.mapstruct.Mapper;

import java.util.HashSet;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    Room toEntity(RoomRequest request);

    default RoomResponse toResponse(Room room) {
        if (room == null)
            return null;
        RoomResponse resp = new RoomResponse();
        resp.setId(room.getId());
        resp.setName(room.getName());
        resp.setDescription(room.getDescription());
        resp.setCapacity(room.getCapacity());
        resp.setAvailable(room.getAvailable());
        resp.setCreatedAt(room.getCreatedAt());
        resp.setUpdatedAt(room.getUpdatedAt());
        resp.setCurrentOccupancy(room.getCurrentOccupancy());
        if (room.getBuilding() != null)
            resp.setBuildingId(room.getBuilding().getId());
        if (room.getFeatures() != null) {
            Set<Long> fids = new HashSet<>();
            room.getFeatures().forEach(f -> fids.add(f.getId()));
            resp.setFeatureIds(fids);
        }
        return resp;
    }
}
