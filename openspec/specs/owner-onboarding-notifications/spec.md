## REMOVED Requirements

### Requirement: Registration confirmation email sent on submission
**Reason**: The registration submission flow is removed; there is no trigger for this email.
**Migration**: The `owner-registration-confirm.html` template and `sendOwnerRegistrationConfirmEmail()` method are deleted.

### Requirement: Email verification reminder sent after 24 hours of inactivity
**Reason**: The `owner_registration` table and its `PENDING` status lifecycle are removed, eliminating the scheduled reminder trigger.
**Migration**: The `sendOwnerVerificationReminderEmail()` method, `OwnerVerificationReminderContext`, and the `@Scheduled` `sendVerificationReminders()` job in `OwnerRegistrationService` are deleted.

### Requirement: Approval notification email sent when registration is approved
**Reason**: The approval workflow and `BuildingOwner` entity creation are removed.
**Migration**: The `owner-approved.html` template and `sendOwnerApprovedEmail()` method are deleted.

### Requirement: Rejection notification email sent when registration is rejected
**Reason**: The rejection workflow is removed along with the `owner_registration` table.
**Migration**: The `owner-rejected.html` template and `sendOwnerRejectedEmail()` method are deleted.

### Requirement: All onboarding emails are sent asynchronously
**Reason**: There are no longer any owner onboarding emails to send.
**Migration**: No replacement. The existing `@Async` email infrastructure remains intact for other notification types.
