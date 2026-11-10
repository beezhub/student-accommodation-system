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

    /**
     * Optional. Left empty on a new year, it is filled in from the start date with the company's
     * lead-time settings; on an edit, the stored window is kept. Once a year has intakes, its
     * window follows theirs.
     */
    private LocalDate applicationOpenDate;

    private LocalDate applicationCloseDate;
}
