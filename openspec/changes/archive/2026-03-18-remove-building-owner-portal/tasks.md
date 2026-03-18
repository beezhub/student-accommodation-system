## 1. Database Migration

- [x] 1.1 Create Flyway migration `V2026_03_18_XXXX__remove_building_owner_portal.sql` that deletes orphaned `BUILDING_OWNER`/`LANDLORD` users, drops `building_staff`, drops `building.owner_id` column, drops `building_owner`, and drops `owner_registration` tables in FK-safe order

## 2. Backend — Entity and Repository Cleanup

- [x] 2.1 Delete `BuildingOwner.java` entity
- [x] 2.2 Delete `OwnerRegistration.java` entity
- [x] 2.3 Delete `BuildingStaff.java` entity (if it exists as a Java entity)
- [x] 2.4 Delete `BuildingOwnerRepository.java`
- [x] 2.5 Delete `OwnerRegistrationRepository.java`
- [x] 2.6 Remove `owner` field and `@ManyToOne` relationship from `Building.java`; remove `findByOwner_Id` from `BuildingRepository`
- [x] 2.7 Remove `findByOwner_IdAndIsActiveTrue` from `BuildingStaffRepository` (or delete the repo if it only has owner-related methods)

## 3. Backend — Services and Controllers

- [x] 3.1 Delete `OwnerPortalService.java`
- [x] 3.2 Delete `OwnerRegistrationService.java`
- [x] 3.3 Delete `OwnerPortalController.java`
- [x] 3.4 Delete `OwnerRegistrationController.java`
- [x] 3.5 Delete `AdminOwnerRegistrationController.java`

## 4. Backend — DTOs, Enums, and Mapper

- [x] 4.1 Delete `OwnerRegistrationRequest.java`, `OwnerRegistrationResponse.java`, `OwnerRegistrationSummaryDto.java`, `SetOwnerPasswordRequest.java`, `RejectOwnerRegistrationRequest.java`, `OwnerBuildingSummaryDto.java`
- [x] 4.2 Delete `OwnerRegistrationStatus.java` enum
- [x] 4.3 Remove `BUILDING_OWNER` and deprecated `LANDLORD` values from `UserRole.java`
- [x] 4.4 Delete `OwnerRegistrationMapper.java`

## 5. Backend — Security and Auth

- [x] 5.1 Remove `/api/public/owner/**`, `/api/owner/**`, and `/api/admin/owner-registrations/**` security rules from `SecurityConfig.java`; remove `BUILDING_OWNER` from role hierarchy
- [x] 5.2 Remove the `BuildingOwner` profile lookup branch from `AppUserDetailsService.java`

## 6. Backend — Email Notification System

- [x] 6.1 Remove `sendOwnerRegistrationConfirmEmail()`, `sendOwnerVerificationReminderEmail()`, `sendOwnerApprovedEmail()`, `sendOwnerRejectedEmail()` from `EmailDispatchService.java`
- [x] 6.2 Remove `OWNER_REGISTRATION_CONFIRM`, `OWNER_VERIFICATION_REMINDER`, `OWNER_REGISTRATION_APPROVED`, `OWNER_REGISTRATION_REJECTED` from `EmailSubjects.java`
- [x] 6.3 Delete `OwnerRegistrationConfirmContext.java`, `OwnerApprovedContext.java`, `OwnerRejectedContext.java`, `OwnerVerificationReminderContext.java`
- [x] 6.4 Delete email templates: `owner-registration-confirm.html`, `owner-verification-reminder.html`, `owner-approved.html`, `owner-rejected.html`

## 7. Backend — Tests

- [x] 7.1 Delete `OwnerRegistrationControllerTest.java`
- [x] 7.2 Delete `AdminOwnerRegistrationControllerTest.java`
- [x] 7.3 Delete `OwnerRegistrationServiceTest.java`
- [x] 7.4 Delete `OwnerPortalServiceTest.java`
- [x] 7.5 Remove building-owner test cases from `TenantFilterIntegrationTest.java`

## 8. Frontend — Feature Directories

- [x] 8.1 Delete `frontend/src/app/features/owner/` directory (owner portal dashboard + service)
- [x] 8.2 Delete `frontend/src/app/features/owner-registration/` directory (registration, email-verify, verification-pending, awaiting-approval, set-password components)
- [x] 8.3 Delete `frontend/src/app/features/admin/owner-registrations/` directory (admin panel + review dialog + service + spec)

## 9. Frontend — Guards, Models, Services

- [x] 9.1 Delete `frontend/src/app/core/guards/building-owner.guard.ts` and `building-owner.guard.spec.ts`
- [x] 9.2 Remove `BUILDING_OWNER`, `LANDLORD` from `role.model.ts`; remove `BUILDING_OWNER` from `ADMIN_ROLES` array
- [x] 9.3 Remove `isBuildingOwner()` from `role.service.ts`; update `isAdmin()` to no longer include building owner; update `role.service.spec.ts`

## 10. Frontend — Routing and Navigation

- [x] 10.1 Remove all `owner/*` and `admin/owner-registrations` route declarations from `app-routing.module.ts`
- [x] 10.2 Remove owner routes from `TOOLBAR_HIDDEN_ROUTES` in `app.component.ts`
- [x] 10.3 Remove "Owner Registrations" nav item from `admin-sidebar.component.ts`
- [x] 10.4 Remove building-owner login redirect from `login.component.ts` and "Register as building owner" link from `login.component.html`
- [x] 10.5 Remove "Register as a building owner" footer link from `home.component.html`

## 11. Build Verification

- [x] 11.1 Run `./gradlew build` in the `backend/` directory and fix any compile errors
- [x] 11.2 Run `./gradlew test` in the `backend/` directory and ensure all tests pass
- [x] 11.3 Run `npm run build` in the `frontend/` directory and fix any compile or import errors
- [x] 11.4 Run `npm test` in the `frontend/` directory and ensure all tests pass
