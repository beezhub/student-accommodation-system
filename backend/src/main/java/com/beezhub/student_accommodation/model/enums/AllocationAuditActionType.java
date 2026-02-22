package com.beezhub.student_accommodation.model.enums;

import lombok.Getter;

@Getter
public enum AllocationAuditActionType {
    MOVE("Move"),
    OVERRIDE("Override"),
    DELETE("Delete"),
    FORCE_ALLOCATE("Force Allocate"),
    STATUS_CHANGE("Status Change");

    private final String displayName;

    AllocationAuditActionType(String displayName) {
        this.displayName = displayName;
    }

    public static AllocationAuditActionType fromString(String value) {
        for (AllocationAuditActionType actionType : AllocationAuditActionType.values()) {
            if (actionType.name().equalsIgnoreCase(value) ||
                actionType.getDisplayName().equalsIgnoreCase(value)) {
                return actionType;
            }
        }
        throw new IllegalArgumentException("Invalid allocation audit action type: " + value);
    }
}
