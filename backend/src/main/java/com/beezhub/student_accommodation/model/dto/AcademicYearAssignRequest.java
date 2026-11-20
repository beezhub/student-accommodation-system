package com.beezhub.student_accommodation.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** "Assign to company": the company that gets its own copy of a platform year (GH-575). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearAssignRequest {

    @NotNull(message = "Choose a company")
    private Long tenantId;
}
