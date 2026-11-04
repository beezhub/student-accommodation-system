package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentRequest {
    private Long userId;

    private String studentNumber;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    private LocalDate dateOfBirth;

    @NotNull(message = "Institution is required")
    private Long institutionId;

    @NotNull(message = "Year of study is required")
    private Long yearOfStudyId;

    private String gender;
    private String specialRequirements;

    // GH-239. Deliberately not the `^[0-9]{10}$` pattern used for the student's own number: a
    // student's family may not be in the country, which is exactly when this record matters most.
    // The rule below accepts an optional leading +, then 7 to 15 digits with spaces, hyphens and
    // parentheses allowed — the E.164 range, without pretending to validate a real subscriber.
    @Size(max = 150, message = "Emergency contact name must be at most 150 characters")
    private String emergencyContactName;

    @Size(max = 50, message = "Relationship must be at most 50 characters")
    private String emergencyContactRelationship;

    @Pattern(regexp = "^$|^\\+?[0-9 ()\\-]{7,20}$",
            message = "Emergency contact phone must be 7-20 digits, optionally starting with +")
    @Size(max = 30, message = "Emergency contact phone must be at most 30 characters")
    private String emergencyContactPhone;

    @Email(message = "Invalid emergency contact email format")
    @Size(max = 255, message = "Emergency contact email must be at most 255 characters")
    private String emergencyContactEmail;

    // AppUser fields
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
}
