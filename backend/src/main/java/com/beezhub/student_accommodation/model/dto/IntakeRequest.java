package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.IntakeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * An intake to create or edit. The application window may be left empty: it is then filled in
 * from the move-in date with the company's lead-time settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntakeRequest {

    @NotNull(message = "Intake type is required")
    private IntakeType intakeType;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must be at most 50 characters")
    private String name;

    @NotNull(message = "Move-in date is required")
    private LocalDate stayStartDate;

    @NotNull(message = "Move-out date is required")
    private LocalDate stayEndDate;

    private LocalDate applicationOpenDate;

    private LocalDate applicationCloseDate;
}
