package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Application count per status for the caller's tenant; the counts add up to the total. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusCountsResponse {
    private Long total;
    /** Every {@link ApplicationStatus}, zeros included. */
    private Map<ApplicationStatus, Long> byStatus;
}
