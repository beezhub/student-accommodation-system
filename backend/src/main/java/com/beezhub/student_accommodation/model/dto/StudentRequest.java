package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class StudentRequest {
    private UserData user;
    private String studentNumber;
    private String PhoneNumber;
    private Date dateOfBirth;
    private InstitutionResponse institution;
    private YearOfStudyResponse yearOfStudy;
    private String gender;
    private String specialRequirements;
}
