package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomFeatureResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
