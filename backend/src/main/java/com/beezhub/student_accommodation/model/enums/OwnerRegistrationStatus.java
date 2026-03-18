package com.beezhub.student_accommodation.model.enums;

public enum OwnerRegistrationStatus {
    /** Submitted but email not yet verified. */
    PENDING,
    /** Email verified; awaiting admin review. */
    PENDING_REVIEW,
    /** Admin approved; app_user + building_owner profile created. */
    APPROVED,
    /** Admin rejected; can re-apply. */
    REJECTED
}
