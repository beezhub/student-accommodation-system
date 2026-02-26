package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum NotificationStatus {
    PENDING("Pending"),
    SENT("Sent"),
    FAILED("Failed"),
    RETRYING("Retrying"),
    PERMANENTLY_FAILED("Permanently Failed");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }
}
