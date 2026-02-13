package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class WaitingListEntryResponse {

    private Long id;
    private Long studentId;
    private String studentNumber;
    private String studentName;
    private String studentEmail;
    private String studentGender;

    private Long applicationId;
    private String applicationCode;
    private String applicationStatus;

    private Long preferredBuildingId;
    private String preferredBuildingName;

    private BigDecimal priorityScore;
    private BigDecimal bestRoomScore;
    private Long bestMatchRoomId;
    private String bestMatchRoomNumber;

    private String status;
    private LocalDateTime createdAt;
    private Integer position;
}
