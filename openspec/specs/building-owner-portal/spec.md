## REMOVED Requirements

### Requirement: Building owner can set a password and activate their account
**Reason**: The building owner role is removed. Multi-tenancy provides full building management through the platform admin role, making a separate owner account activation flow unnecessary.
**Migration**: No replacement. Tenant admins manage buildings directly.

### Requirement: Building owner can log in to the owner portal
**Reason**: The `BUILDING_OWNER` role and its associated JWT claims are removed from the system.
**Migration**: No replacement. Building management is handled by tenant `PLATFORM_ADMIN` users.

### Requirement: Owner portal dashboard shows owner's buildings
**Reason**: The `GET /api/owner/buildings` endpoint and the owner portal frontend are removed.
**Migration**: Admins use the existing admin building management panel at `/admin/buildings`.

### Requirement: Owner portal access is blocked for non-owner roles
**Reason**: The `BuildingOwnerGuard` and all `/owner/**` routes are removed.
**Migration**: No replacement routes exist; the guard and routes are deleted entirely.
