package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.model.entity.Institution;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {

    InstitutionResponse toResponse(Institution institution);

    List<InstitutionResponse> toResponseList(List<Institution> institutions);
}
