package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllocationAuditLogResponse {

    private Long id;
    private AllocationAuditActionType actionType;
    private String reason;
    private String constraintBypassed;
    private LocalDateTime createdAt;

    // Allocation details
    private Long allocationId;
    private Long studentId;
    private String studentName;
    private String studentNumber;

    // Admin details
    private Long adminUserId;
    private String adminName;
    private String adminEmail;

    // Room change details
    private Long oldRoomId;
    private String oldRoomNumber;
    private String oldBuildingName;

    private Long newRoomId;
    private String newRoomNumber;
    private String newBuildingName;

    // Status transition fields (populated for STATUS_CHANGE action type)
    private String previousStatus;
    private String newStatus;
}
