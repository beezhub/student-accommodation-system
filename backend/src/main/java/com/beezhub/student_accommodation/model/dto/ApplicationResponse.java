package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ApplicationResponse {
    private Long id;
    private String applicationCode;
    private LocalDate applicationDate;
    private String status;
}
