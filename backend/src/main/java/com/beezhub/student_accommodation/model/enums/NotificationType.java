package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    APPLICATION_APPROVED("Application Approved"),
    APPLICATION_REJECTED("Application Rejected"),
    DOCUMENT_VERIFIED("Document Verified"),
    DOCUMENT_REJECTED("Document Rejected"),
    ROOM_ALLOCATED("Room Allocated"),
    WELCOME_EMAIL("Welcome Email"),
    PASSWORD_RESET("Password Reset"),
    GENERAL("General Notification");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }
}
