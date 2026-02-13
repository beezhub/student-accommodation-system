package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class StudentRequest {
    private Long userId;

    @NotBlank(message = "Student number is required")
    private String studentNumber;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    private Date dateOfBirth;

    @NotNull(message = "Institution is required")
    private Long institutionId;

    @NotNull(message = "Year of study is required")
    private Long yearOfStudyId;

    private String gender;
    private String specialRequirements;

    // AppUser fields
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
}
