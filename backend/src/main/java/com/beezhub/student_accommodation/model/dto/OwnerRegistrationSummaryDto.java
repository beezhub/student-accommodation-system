package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.OwnerRegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OwnerRegistrationSummaryDto {
    private Long id;
    private String fullName;
    private String email;
    private String organisationName;
    private String contactPhone;
    private OwnerRegistrationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
}
