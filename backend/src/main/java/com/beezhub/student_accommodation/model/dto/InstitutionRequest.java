package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InstitutionRequest {
    @NotBlank(message = "Institution name is required")
    private String name;
    @NotBlank(message = "Address is required")
    private String address;
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    @Email(message = "Email address must be valid")
    private String emailAddress;
}
