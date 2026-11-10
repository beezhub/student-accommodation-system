package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearResponse {

    private Long id;
    private Long tenantId;
    /** Empty means "All institutions". */
    private Long institutionId;
    private String institutionName;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate applicationOpenDate;
    private LocalDate applicationCloseDate;
    private Boolean isApplicationWindowOpen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
