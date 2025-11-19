package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.BuildingResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BuildingMapper {

    BuildingResponse toResponse(Building building);

    List<BuildingResponse> toResponseList(List<Building> buildings);
}