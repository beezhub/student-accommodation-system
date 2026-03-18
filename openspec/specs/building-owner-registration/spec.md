## REMOVED Requirements

### Requirement: Building owner can submit a registration request
**Reason**: The public self-registration flow for building owners is removed. External parties no longer need a separate onboarding path; building and room management is owned by tenant admins.
**Migration**: No replacement. The `POST /api/public/owner/register` endpoint is deleted.

### Requirement: Owner must verify email before registration is reviewable
**Reason**: The `owner_registration` table and its verification token lifecycle are removed.
**Migration**: No replacement. The `owner_registration` database table is dropped.

### Requirement: Registration request is persisted independently of app_user
**Reason**: The `owner_registration` entity and table are deleted as part of the removal of the building owner portal.
**Migration**: The `owner_registration` table is dropped via Flyway migration. Existing records are deleted before the table is dropped.
