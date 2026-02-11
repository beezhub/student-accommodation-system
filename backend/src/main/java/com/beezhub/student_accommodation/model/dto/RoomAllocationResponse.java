package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.DeclineReason;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomAllocationResponse {
    private Long id;

    // Student information (flattened)
    private Long studentId;
    private String studentFirstName;
    private String studentLastName;
    private String studentEmail;

    // Room information (flattened)
    private Long roomId;
    private String roomNumber;
    private String buildingName;

    // Application information (optional)
    private Long applicationId;

    // Allocation details
    private LocalDateTime allocationDate;
    private LocalDateTime endDate;
    private String status;

    // Expiry tracking
    private LocalDateTime offerExpiryDate;
    private LocalDateTime checkInExpiryDate;

    // Decline tracking
    private DeclineReason declineReason;
    private String declineReasonText;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}