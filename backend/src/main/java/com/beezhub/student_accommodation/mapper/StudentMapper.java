package com.beezhub.student_accommodation.mapper;

import com.beezhub.student_accommodation.model.dto.StudentRequest;
import com.beezhub.student_accommodation.model.dto.StudentResponse;
import com.beezhub.student_accommodation.model.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {InstitutionMapper.class, YearOfStudyMapper.class})
public interface StudentMapper {

    @Mapping(source = "userId", target = "appUser.id")
    @Mapping(source = "institutionId", target = "institution.id")
    @Mapping(source = "yearOfStudyId", target = "yearOfStudy.id")
    Student toEntity(StudentRequest studentRequest);

    @Mapping(source = "appUser.id", target = "userId")
    @Mapping(source = "institution.id", target = "institutionId")
    @Mapping(source = "yearOfStudy.id", target = "yearOfStudyId")
    StudentResponse toResponse(Student savedStudent);
}
