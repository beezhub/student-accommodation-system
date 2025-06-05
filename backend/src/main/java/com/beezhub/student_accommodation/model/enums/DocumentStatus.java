package com.beezhub.student_accommodation.model.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum DocumentStatus {

    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String status;


}
