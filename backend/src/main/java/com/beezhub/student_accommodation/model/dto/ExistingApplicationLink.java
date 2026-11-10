package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The student's own active application with a company, shown instead of a new form for that year
 * (GH-495).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExistingApplicationLink {
    private Long applicationId;
    private String applicationCode;
    private Long academicYearId;
    private String academicYearName;
    private String intakeName;
    private String status;
}
