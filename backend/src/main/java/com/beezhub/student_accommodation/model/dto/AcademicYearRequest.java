package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearRequest {

    /** Empty means every institution the company serves ("All institutions"). */
    private Long institutionId;

    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be at most 20 characters")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Application open date is required")
    private LocalDate applicationOpenDate;

    @NotNull(message = "Application close date is required")
    private LocalDate applicationCloseDate;
}
