package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.IntakeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

/**
 * "Create year" from a pattern: the institution, the year's dates, and any of Annual, Semesters
 * and Trimesters. The intakes and their windows are filled in.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearPatternRequest {

    /** Empty means every institution the company serves ("All institutions"). */
    private Long institutionId;

    @NotBlank(message = "Name is required")
    @Size(max = 20, message = "Name must be at most 20 characters")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotEmpty(message = "Pick at least one of Annual, Semesters or Trimesters")
    private Set<IntakeType> intakeTypes;
}
