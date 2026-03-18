package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OwnerRegistrationRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must not exceed 200 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address is required")
    private String email;

    @NotBlank(message = "Organisation name is required")
    @Size(max = 200, message = "Organisation name must not exceed 200 characters")
    private String organisationName;

    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    private String contactPhone;

    private String message;
}
