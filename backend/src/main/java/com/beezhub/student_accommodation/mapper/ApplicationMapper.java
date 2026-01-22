package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.entity.Application;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentNumber", source = "student.studentNumber")
    @Mapping(target = "studentName", expression = "java(application.getStudent().getAppUser().getFirstName() + \" \" + application.getStudent().getAppUser().getLastName())")
    @Mapping(target = "studentEmail", source = "student.appUser.email")
    @Mapping(target = "preferredAccommodationType", source = "preferredAccommodationType")
    @Mapping(target = "preferredMoveInDate", source = "preferredMoveInDate")
    @Mapping(target = "preferredDurationMonths", source = "preferredDurationMonths")
    @Mapping(target = "preferredRoomType", source = "preferredRoomType")
    @Mapping(target = "specialRequirements", source = "specialRequirements")
    @Mapping(target = "institutionName", source = "institutionName")
    @Mapping(target = "courseName", source = "courseName")
    @Mapping(target = "preferredBuildingId", source = "preferredBuilding.id")
    @Mapping(target = "preferredBuildingName", source = "preferredBuilding.name")
    @Mapping(target = "allocationMethod", expression = "java(application.getAllocationMethod() != null ? application.getAllocationMethod().name() : null)")
    @Mapping(target = "remarks", source = "remarks")
    ApplicationResponse toResponse(Application application);
}
