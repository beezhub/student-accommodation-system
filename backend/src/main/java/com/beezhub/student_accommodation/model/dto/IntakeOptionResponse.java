package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** An intake as the application form shows it to a student (GH-495). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeOptionResponse {
    private Long intakeId;
    private String intakeName;
    private String intakeType;
    private Long academicYearId;
    private String academicYearName;
    private LocalDate stayStartDate;
    private LocalDate stayEndDate;
    private LocalDate applicationOpenDate;
    private LocalDate applicationCloseDate;
}
