## Why

The system currently has no structured way for building owners (private landlords, property companies) to register and onboard their accommodation offerings. Admins must manually create building and owner records, creating a bottleneck and limiting scalability as the platform grows to support multiple institutions and external property providers.

## What Changes

- New **building owner registration** flow — owners can self-register with contact details, organisation name, and verification documents
- New **owner dashboard** — a lightweight portal for building owners to manage their buildings and view allocation statistics
- **Admin review and approval** for building owner registration requests before access is granted
- **Role: BUILDING_OWNER** — new Spring Security role scoped to owner-managed buildings only
- Email notifications at each onboarding step (registration received, approved, rejected, activation)
- Existing admin building management updated to show owner association

## Capabilities

### New Capabilities

- `building-owner-registration`: Self-service registration form and submission workflow for building owners
- `building-owner-approval`: Admin review queue for pending owner registrations with approve/reject actions
- `building-owner-portal`: Authenticated owner dashboard scoped to their own buildings and allocation data
- `owner-onboarding-notifications`: Transactional email notifications throughout the onboarding lifecycle

### Modified Capabilities

- `frontend-multi-tenancy`: Owner role must be scoped within the tenant (institution) context — owner access does not cross tenant boundaries

## Impact

- **Backend**: New `building_owner` entity and `owner_registration` table; new `BUILDING_OWNER` role in `app_user`; new controller endpoints under `/api/owner`; updates to `SecurityConfig` for owner-scoped access; new Flyway migrations
- **Frontend**: New public registration page; new owner portal module; admin registration review page; updates to admin sidebar nav
- **Email**: New notification templates for owner onboarding events
- **Database**: New tables: `owner_registration`, updates to `building` to enforce `owner_id` FK
