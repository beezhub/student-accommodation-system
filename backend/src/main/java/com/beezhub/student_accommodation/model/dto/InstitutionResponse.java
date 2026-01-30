package com.beezhub.student_accommodation.model.dto;

import lombok.Data;

@Data
public class InstitutionResponse {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private String emailAddress;
}
