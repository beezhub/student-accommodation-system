package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.YearOfStudyResponse;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface YearOfStudyMapper {

    YearOfStudyResponse toResponse(YearOfStudy yearOfStudy);

    List<YearOfStudyResponse> toResponseList(List<YearOfStudy> yearOfStudyList);
}
