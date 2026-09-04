## 1. Database Migrations

- [x] 1.1 Create Flyway migration for `owner_registration` table (id, full_name, email, organisation_name, contact_phone, message, status, tenant_id, created_at, verified_at, reminder_sent_at)
- [x] 1.2 Create Flyway migration for `owner_profile` table (id, app_user_id FK, organisation_name, contact_phone, tenant_id)
- [x] 1.3 Create Flyway migration to add `owner_id` FK column on `building` table referencing `owner_profile`
- [x] 1.4 Create Flyway migration to add `email_verification_token` and `password_setup_token` columns to `owner_registration`

## 2. Backend — Entities & Repositories

- [x] 2.1 Create `OwnerRegistration` JPA entity with `OwnerRegistrationStatus` enum (PENDING, PENDING_REVIEW, APPROVED, REJECTED)
- [x] 2.2 Create `OwnerProfile` JPA entity linked to `AppUser` and `Tenant`
- [x] 2.3 Update `AppUser` entity to support `ROLE_BUILDING_OWNER` authority
- [x] 2.4 Create `OwnerRegistrationRepository` and `OwnerProfileRepository`
- [x] 2.5 Update `Building` entity to add optional `ownerProfile` ManyToOne relationship

## 3. Backend — Registration & Approval Service

- [x] 3.1 Create `OwnerRegistrationService` with `submit()`, `verifyEmail()`, `resendVerification()`, `approve()`, `reject()`, `reapply()` methods
- [x] 3.2 Implement email verification token generation (reuse or adapt existing `EmailVerificationToken` pattern)
- [x] 3.3 Implement `approve()` — creates `AppUser` + `OwnerProfile`, sets status `APPROVED`, triggers approval email
- [x] 3.4 Implement `reject()` — sets status `REJECTED`, triggers rejection email with optional reason
- [x] 3.5 Implement `reapply()` — allows new submission for previously `REJECTED` email, leaves old record intact
- [x] 3.6 Implement password-setup token generation and validation (48-hour expiry)
- [x] 3.7 Create scheduled job to send 24-hour verification reminder for unverified `PENDING` registrations

## 4. Backend — Owner Portal Service

- [x] 4.1 Create `OwnerPortalService` with `getOwnerBuildings()` method filtering by `building.owner_id`
- [x] 4.2 Implement `setPassword()` method — validates token, sets encoded password, activates account
- [x] 4.3 Add `ownerProfileId` claim to JWT token for `BUILDING_OWNER` users
- [x] 4.4 Add `@PreAuthorize` ownership checks on all owner-scoped building endpoints

## 5. Backend — REST Controllers

- [x] 5.1 Create `OwnerRegistrationController` with endpoints: `POST /api/public/owner/register`, `GET /api/public/owner/verify-email`, `POST /api/public/owner/resend-verification`
- [x] 5.2 Create `AdminOwnerRegistrationController` with endpoints: `GET /api/admin/owner-registrations`, `POST /api/admin/owner-registrations/{id}/approve`, `POST /api/admin/owner-registrations/{id}/reject`
- [x] 5.3 Create `OwnerPortalController` with endpoints: `GET /api/owner/buildings`, `POST /api/public/owner/set-password`
- [x] 5.4 Add `SecurityConfig` rules: permit `/api/public/owner/**`, require `ROLE_ADMIN` for `/api/admin/owner-registrations/**`, require `ROLE_BUILDING_OWNER` for `/api/owner/**`
- [x] 5.5 Validate tenant constraint: reject `BUILDING_OWNER` user creation with `tenantId = 0`

## 6. Backend — DTOs & Mappers

- [x] 6.1 Create `OwnerRegistrationRequest` DTO with validation annotations
- [x] 6.2 Create `OwnerRegistrationResponse` DTO and `OwnerRegistrationSummaryDto` for admin queue
- [x] 6.3 Create `OwnerBuildingSummaryDto` for portal dashboard
- [x] 6.4 Create `OwnerRegistrationMapper` using MapStruct

## 7. Backend — Email Notifications

- [x] 7.1 Create email template for registration confirmation + verification link
- [x] 7.2 Create email template for 24-hour verification reminder
- [x] 7.3 Create email template for approval + password-setup link
- [x] 7.4 Create email template for rejection (with/without reason variants)
- [x] 7.5 Wire all templates into `OwnerRegistrationService` via `NotificationService` `@Async` calls

## 8. Frontend — Owner Registration (Public)

- [x] 8.1 Create `features/owner-registration/` module with lazy-loaded route at `/owner/register`
- [x] 8.2 Build registration form component with fields: full name, email, organisation, phone, message
- [x] 8.3 Build email verification pending page with resend option
- [x] 8.4 Build email verified success page redirecting to "awaiting approval" screen
- [x] 8.5 Build password-setup page (linked from approval email) with password + confirm password fields

## 9. Frontend — Admin Owner Registration Queue

- [x] 9.1 Create `features/admin/owner-registrations/` component with paginated registration list
- [x] 9.2 Build registration detail/review dialog with approve and reject actions
- [x] 9.3 Add rejection reason input to reject dialog (optional field)
- [x] 9.4 Add "Owner Registrations" link to admin sidebar nav group
- [x] 9.5 Add `OwnerRegistrationService` Angular service calling admin registration endpoints

## 10. Frontend — Owner Portal

- [x] 10.1 Create `features/owner/` module with lazy-loaded routing under `/owner/**`
- [x] 10.2 Create `OwnerGuard` — redirects non-`BUILDING_OWNER` users to their home; unauthenticated to `/login`
- [x] 10.3 Build owner portal dashboard page showing assigned buildings list with occupancy summary
- [x] 10.4 Create `OwnerPortalService` Angular service calling `/api/owner/buildings`
- [x] 10.5 Add owner portal login route and ensure `BUILDING_OWNER` JWT is stored and decoded correctly

## 11. Frontend — Multi-Tenancy Update

- [x] 11.1 Update user creation form to exclude platform tenant (id=0) when `BUILDING_OWNER` role is selected
- [x] 11.2 Display validation error if platform tenant is selected for a building owner

## 12. Tests

- [x] 12.1 Write integration tests for `OwnerRegistrationService` (submit, verify, approve, reject, reapply)
- [x] 12.2 Write integration tests for `OwnerPortalService` ownership filtering
- [x] 12.3 Write unit tests for `OwnerRegistrationController` and `AdminOwnerRegistrationController`
- [x] 12.4 Write Angular unit tests for registration form, admin queue component, and OwnerGuard
