package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoomAllocationResponse {
    private String id;
    private LocalDateTime allocationDate;
    private LocalDateTime endDate;
    private String status;
    private StudentResponse student;
    private RoomResponse room;
    private ApplicationResponse application;
}