package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.RoomResponse;
import com.beezhub.student_accommodation.model.entity.Room;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {BuildingMapper.class})
public interface RoomMapper {

    RoomResponse toResponse(Room room);

    List<RoomResponse> toResponseList(List<Room> rooms);
}
