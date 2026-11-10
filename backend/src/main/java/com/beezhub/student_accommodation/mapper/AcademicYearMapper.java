package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * {@code isApplicationWindowOpen} is left to {@code AcademicYearService}, which reads the date
 * from the calendar {@code Clock} rather than the JVM zone (GH-494).
 */
@Mapper(componentModel = "spring")
public interface AcademicYearMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "institution", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AcademicYear toEntity(AcademicYearRequest request);

    @Mapping(target = "institutionName", source = "institution.name")
    @Mapping(target = "isApplicationWindowOpen", ignore = true)
    AcademicYearResponse toResponse(AcademicYear academicYear);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "institution", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(AcademicYearRequest request, @MappingTarget AcademicYear academicYear);
}
