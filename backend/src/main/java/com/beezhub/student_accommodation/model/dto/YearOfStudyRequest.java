package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.validation.NumericYearFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class YearOfStudyRequest {
    @NotBlank(message = "Year of study is required")
    @Size(max = 50, message = "Year of study cannot exceed 50 characters")
    @NumericYearFormat
    private String yearOfStudy;
}
