package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearResponse {

    private Long id;
    private Long tenantId;
    /** The owning company, on the platform's list of every company's years. Empty for a platform year. */
    private String companyName;
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
    private List<IntakeResponse> intakes;
    /** Set on an edit: applications filed under this year, which keep their year and intake. */
    private Long applicationCount;
    /** Set on an edit: allocations under this year, which keep their dates. */
    private Long allocationCount;
}
