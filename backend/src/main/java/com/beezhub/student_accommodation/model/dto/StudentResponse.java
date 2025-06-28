package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class StudentResponse {
    private Long id;
    private String phoneNumber;
    private String studentNumber;
    private String dateOfBirth;
    private Date registrationDate;
    private String gender;
    private String specialRequirements;
    private Long institutionId;
    private Long yearOfStudyId;
    private Long userId;
}
