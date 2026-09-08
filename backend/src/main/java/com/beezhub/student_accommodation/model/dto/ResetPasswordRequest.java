package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @com.beezhub.student_accommodation.validation.StrongPassword
    private String newPassword;
}

