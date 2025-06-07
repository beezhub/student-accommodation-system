package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class StudentRequest {
    private Long userId;
    private String studentNumber;
    private String PhoneNumber;
    private Date dateOfBirth;
    private Long institutionId;
    private Long yearOfStudyId;
    private String gender;
    private String specialRequirements;
}
