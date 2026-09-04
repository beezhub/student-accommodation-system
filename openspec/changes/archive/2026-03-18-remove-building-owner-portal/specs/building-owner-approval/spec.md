## REMOVED Requirements

### Requirement: Admin can view pending owner registration requests
**Reason**: The admin owner registrations panel and the `GET /api/admin/owner-registrations` endpoint are removed. There are no longer external building owner accounts to review.
**Migration**: The `admin/owner-registrations` route and sidebar nav item are deleted. The `AdminOwnerRegistrationController` is removed.

### Requirement: Admin can approve a building owner registration
**Reason**: Approval creates a `BUILDING_OWNER` user and `owner_profile`, both of which are removed from the system.
**Migration**: The `POST /api/admin/owner-registrations/{id}/approve` endpoint is deleted.

### Requirement: Admin can reject a building owner registration
**Reason**: The rejection workflow depends on `OwnerRegistration` records, which are removed.
**Migration**: The `POST /api/admin/owner-registrations/{id}/reject` endpoint is deleted.

### Requirement: Rejected owner can re-apply
**Reason**: Re-application depends on the `owner_registration` table, which is dropped.
**Migration**: The `POST /api/public/owner/reapply` endpoint is deleted. Existing `REJECTED` records are deleted by the migration.
