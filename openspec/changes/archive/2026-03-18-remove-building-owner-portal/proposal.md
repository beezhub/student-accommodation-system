## Why

The system already provides multi-tenancy where each tenant (institution/university) manages its own buildings, rooms, and allocations through the platform admin role. A separate building owner portal duplicates this responsibility, adds complexity to the security model, and creates an unnecessary user role that overlaps with existing tenant admin capabilities. Removing it simplifies the architecture and reduces ongoing maintenance burden.

## What Changes

- **BREAKING** Remove the `BUILDING_OWNER` user role and all associated security rules
- **BREAKING** Remove the `BuildingOwner` entity, `OwnerRegistration` entity, and their database tables (`building_owner`, `owner_registration`)
- **BREAKING** Remove the nullable `owner_id` FK column from the `building` table
- Remove the `building_staff` table (staff management was gated on building owners)
- Remove all building owner registration flows (public registration, email verification, admin approval/rejection)
- Remove the owner portal dashboard and `owner/*` frontend routes
- Remove the `admin/owner-registrations` admin panel and associated admin sidebar nav item
- Remove `BuildingOwnerGuard`, `isBuildingOwner()` role check, and owner-specific login redirect
- Remove 4 Thymeleaf owner email templates and their context/subject classes
- Remove all owner-related backend controllers, services, repositories, DTOs, and mappers
- Remove owner-related frontend components, services, and unit test files
- Add a Flyway migration to drop the removed columns and tables

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `building-owner-portal`: Removing this capability entirely
- `building-owner-registration`: Removing this capability entirely
- `building-owner-approval`: Removing this capability entirely
- `owner-onboarding-notifications`: Removing this capability entirely

## Impact

**Backend:**
- `OwnerPortalController`, `OwnerRegistrationController`, `AdminOwnerRegistrationController` deleted
- `OwnerPortalService`, `OwnerRegistrationService` deleted
- `BuildingOwner`, `OwnerRegistration` entities deleted
- `BuildingOwnerRepository`, `OwnerRegistrationRepository` deleted
- `OwnerRegistrationMapper` deleted
- All owner DTOs deleted; `UserRole.BUILDING_OWNER` and deprecated `LANDLORD` removed
- `SecurityConfig` updated to remove owner role rules
- `AppUserDetailsService` simplified to remove building owner profile lookup
- 4 owner email templates, context classes, and subject constants deleted
- `Building.owner` FK field removed; `building_staff` entity and table removed
- New Flyway migration drops all owner-related tables and columns

**Frontend:**
- `features/owner/`, `features/owner-registration/`, `features/admin/owner-registrations/` deleted
- `BuildingOwnerGuard` deleted; `role.model.ts` and `role.service.ts` cleaned up
- Owner routes removed from routing and `TOOLBAR_HIDDEN_ROUTES`
- Admin sidebar "Owner Registrations" item removed
- Login page owner redirect and "Register as building owner" links removed
- Home page footer "Register as a building owner" link removed

**Database:** New migration drops `building_owner`, `owner_registration`, `building_staff` tables and `building.owner_id` column.
