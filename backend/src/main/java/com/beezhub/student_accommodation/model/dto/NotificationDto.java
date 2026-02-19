package com.beezhub.student_accommodation.model.dto;

import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String recipient;
    private String subject;
    private NotificationType type;
    private NotificationStatus status;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String errorMessage;
}
