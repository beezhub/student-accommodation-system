package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {StudentMapper.class, RoomMapper.class, ApplicationMapper.class})
public interface RoomAllocationMapper {

    RoomAllocationResponse toResponse(RoomAllocation roomAllocation);

    List<RoomAllocationResponse> toResponseList(List<RoomAllocation> roomAllocations);
}