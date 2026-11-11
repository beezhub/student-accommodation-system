package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Whether a student can stay on in their room for the next intake (GH-498). When they cannot but a
 * next intake exists, the screen links to applying for it instead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StayExtensionResponse {
    private Long allocationId;
    private Boolean allowed;
    /** Why not, in words a student can act on. Empty when allowed. */
    private String reason;
    /** The day the bed is free again today, before any extension. */
    private LocalDate currentStayEndDate;
    /** The next intake of the same type in the same year, when there is one. */
    private Long nextIntakeId;
    private String nextIntakeName;
    private LocalDate nextStayStartDate;
    private LocalDate nextStayEndDate;
}
