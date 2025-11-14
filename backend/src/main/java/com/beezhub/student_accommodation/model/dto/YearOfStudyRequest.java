package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class YearOfStudyRequest {
    @NotBlank(message = "Year of study is required")
    private String yearOfStudy;
}
