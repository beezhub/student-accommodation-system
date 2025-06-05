package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum ApplicationStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String status;

    ApplicationStatus(String status) {
        this.status = status;
    }

    public static ApplicationStatus fromString(String status) {
        for (ApplicationStatus applicationStatus : ApplicationStatus.values()) {
            if (applicationStatus.getStatus().equalsIgnoreCase(status)) {
                return applicationStatus;
            }
        }
        throw new IllegalArgumentException("Invalid status: " + status);
    }
}
