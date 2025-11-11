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
    ApplicationResponse toResponse(Application application);
}
