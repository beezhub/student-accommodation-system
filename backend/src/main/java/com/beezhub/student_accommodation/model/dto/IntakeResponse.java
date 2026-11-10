package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.IntakeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeResponse {

    private Long id;
    private Long academicYearId;
    private IntakeType intakeType;
    private String name;
    private LocalDate stayStartDate;
    private LocalDate stayEndDate;
    private LocalDate applicationOpenDate;
    private LocalDate applicationCloseDate;
    private Boolean isApplicationWindowOpen;
    /** Set on an edit: applications filed under this intake's year, which keep their intake. */
    private Long applicationCount;
    /** Set on an edit: allocations under this intake's year, which keep their dates. */
    private Long allocationCount;
}
