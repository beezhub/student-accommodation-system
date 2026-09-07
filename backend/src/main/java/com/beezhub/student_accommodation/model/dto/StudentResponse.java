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
    // GH-239. Returned to the student on their own profile and to admins through the paths that
    // already reach a student record; it is not added to any list response.
    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactPhone;
    private String emergencyContactEmail;
    private Long institutionId;
    private Long yearOfStudyId;
    private Long userId;
    
    // AppUser fields
    private String firstName;
    private String lastName;
    private String email;
}
