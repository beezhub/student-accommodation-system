package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * When one of a student's documents expires (GH-498), worked out for the companies the student is
 * applying to or living with. Where they differ, the earliest date is given.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentExpiryResponse {
    private Long documentId;
    private Long documentTypeId;
    private String documentTypeName;
    /** The last day the document is valid. */
    private LocalDate expiryDate;
    private Boolean expired;
    /** Expired or expiring within the reminder period: the student may upload the new one now. */
    private Boolean dueForReplacement;
}
