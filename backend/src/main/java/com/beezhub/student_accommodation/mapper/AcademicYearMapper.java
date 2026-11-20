package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * {@code isApplicationWindowOpen} is left to {@code AcademicYearService}, which reads the date
 * from the calendar {@code Clock} rather than the JVM zone (GH-494). The year's window is worked
 * out there from its intakes, since the year no longer stores one (GH-496).
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
    @Mapping(target = "companyName", ignore = true)
    @Mapping(target = "isApplicationWindowOpen", ignore = true)
    @Mapping(target = "applicationOpenDate", ignore = true)
    @Mapping(target = "applicationCloseDate", ignore = true)
    @Mapping(target = "intakes", ignore = true)
    @Mapping(target = "applicationCount", ignore = true)
    @Mapping(target = "allocationCount", ignore = true)
    AcademicYearResponse toResponse(AcademicYear academicYear);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "institution", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(AcademicYearRequest request, @MappingTarget AcademicYear academicYear);
}
