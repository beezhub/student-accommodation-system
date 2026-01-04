package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.RoomAllocationResponse;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomAllocationMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "student.appUser.firstName", target = "studentFirstName")
    @Mapping(source = "student.appUser.lastName", target = "studentLastName")
    @Mapping(source = "student.appUser.email", target = "studentEmail")
    @Mapping(source = "room.id", target = "roomId")
    @Mapping(source = "room.roomNumber", target = "roomNumber")
    @Mapping(source = "room.building.name", target = "buildingName")
    @Mapping(source = "application.id", target = "applicationId")
    RoomAllocationResponse toResponse(RoomAllocation roomAllocation);

    List<RoomAllocationResponse> toResponseList(List<RoomAllocation> roomAllocations);
}