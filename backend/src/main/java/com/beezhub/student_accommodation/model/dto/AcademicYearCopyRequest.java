package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** "Copy last year". The name may be left empty when the old one contains a year, such as 2027. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearCopyRequest {

    @Size(max = 20, message = "Name must be at most 20 characters")
    private String name;
}
