package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface AcademicYearMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AcademicYear toEntity(AcademicYearRequest request);

    @Mapping(target = "isApplicationWindowOpen", expression = "java(isWindowOpen(academicYear))")
    AcademicYearResponse toResponse(AcademicYear academicYear);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(AcademicYearRequest request, @MappingTarget AcademicYear academicYear);

    default Boolean isWindowOpen(AcademicYear academicYear) {
        if (academicYear == null || academicYear.getApplicationOpenDate() == null
                || academicYear.getApplicationCloseDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !today.isBefore(academicYear.getApplicationOpenDate())
                && !today.isAfter(academicYear.getApplicationCloseDate());
    }
}
