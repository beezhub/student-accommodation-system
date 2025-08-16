package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.RoomFeatureRequest;
import com.beezhub.student_accommodation.model.dto.RoomFeatureResponse;
import com.beezhub.student_accommodation.model.entity.RoomFeature;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomFeatureMapper {

    RoomFeature toEntity(RoomFeatureRequest request);

    RoomFeatureResponse toResponse(RoomFeature roomFeature);

    List<RoomFeatureResponse> toResponseList(List<RoomFeature> features);
}
