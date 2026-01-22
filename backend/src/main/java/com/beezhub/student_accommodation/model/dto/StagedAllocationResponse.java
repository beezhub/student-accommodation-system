package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.StagedAllocationStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedAllocationResponse {

    private Long id;
    private StagedAllocationStatus status;
    private BigDecimal score;
    private List<String> warnings;
    private String batchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;

    // Student details
    private Long studentId;
    private String studentNumber;
    private String studentFirstName;
    private String studentLastName;
    private String studentEmail;
    private String studentGender;

    // Room details
    private Long roomId;
    private String roomNumber;
    private Integer roomCapacity;
    private Integer currentOccupancy;

    // Building details
    private Long buildingId;
    private String buildingName;

    // Application details
    private Long applicationId;
    private String applicationCode;
    private String applicationStatus;

    // Proposed by details
    private Long proposedById;
    private String proposedByName;
}
