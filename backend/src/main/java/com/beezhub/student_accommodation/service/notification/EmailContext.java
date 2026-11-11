package com.beezhub.student_accommodation.service.notification;

/**
 * Sealed marker interface for typed email template variable containers.
 * Each email type has a corresponding record implementing this interface.
 * Layout variables (universityName, supportEmail, contactPhone, currentYear)
 * are injected automatically by EmailService and must NOT appear in these records.
 */
public sealed interface EmailContext permits
        WelcomeContext,
        PasswordResetContext,
        ApplicationApprovedContext,
        ApplicationRejectedContext,
        ApplicationChangesRequestedContext,
        DocumentVerifiedContext,
        DocumentRejectedContext,
        DocumentCommentContext,
        RoomAllocatedContext,
        AllocationProposalContext,
        AllocationConfirmedContext,
        AllocationRejectedContext,
        AllocationMovedContext,
        OfferDeclinedRequeueContext,
        OfferDeclinedMaxReachedContext,
        OfferExpiredContext,
        OfferExpiryReminderContext,
        CheckInReminderContext,
        CheckInExpiredContext,
        ReconfirmRequestContext,
        ReconfirmReminderContext,
        PlaceReleasedContext,
        ApplicationExpiredContext,
        ApplicationsWithdrawnContext,
        DocumentExpiryReminderContext,
        StayExtendedContext {
}
