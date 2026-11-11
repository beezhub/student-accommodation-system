package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * What a student can apply for at the company owning one building, checked as the building is
 * picked so problems show before submit (GH-495).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyApplyOptionsResponse {
    private Long buildingId;
    private Long companyId;
    private String companyName;

    /** Intakes open today, leaving out years the student already has an active application for here. */
    private List<IntakeOptionResponse> openIntakes;

    /** The student's active applications with this company, to link to instead of a new form. */
    private List<ExistingApplicationLink> existingApplications;

    /** The next intake whose window opens, when none is open now. */
    private IntakeOptionResponse nextIntake;

    /** Required document types of this company the student has not uploaded. */
    private List<String> missingDocuments;
}
