package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.AccommodationTypeResponse;
import com.beezhub.student_accommodation.model.entity.AccommodationType;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccommodationTypeMapper {

    AccommodationTypeResponse toResponse(AccommodationType accommodationType);

    List<AccommodationTypeResponse> toResponseList(List<AccommodationType> accommodationTypes);
}
