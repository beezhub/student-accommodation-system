package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.beezhub.student_accommodation.model.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {InstitutionMapper.class, YearOfStudyMapper.class})
public interface StudentMapper {

    @Mapping(source = "user", target = "appUser")
    Student toEntity(StudentRequest studentRequest);

    @Mapping(source = "appUser", target = "user")
    StudentResponse toResponse(Student savedStudent);
}
