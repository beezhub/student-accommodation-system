package com.beezhub.student_accommodation.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityLogResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String userRole;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private String ipAddress;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
