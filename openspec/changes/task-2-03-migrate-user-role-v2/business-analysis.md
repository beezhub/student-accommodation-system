# Business Requirements Analysis
## TASK-2.03 — Migrate UserRole to v2.0 Role Model

---

## 1. Problem Statement

**What problem are we solving?**

The current `UserRole` enum contains three values: `STUDENT`, `ADMIN`, and `LANDLORD`. This model was designed for a single-university, single-tenancy deployment. The system is evolving toward a multi-tenant SaaS architecture (Phase 2) where each tenant is a university with its own accommodation buildings and staff. The existing roles do not map to this reality:

- `ADMIN` conflates two distinct concerns: **platform-level** administration (creating tenants, managing the platform) and **tenant-level** administration (managing rooms, reviewing applications within one university).
- `LANDLORD` is defined in the enum and in the role hierarchy but is **never used in any controller `@PreAuthorize` annotation**. Its intended meaning is ambiguous — it could mean a building owner within a tenant, or a standalone property manager — and it has no access-controlled endpoints today.
- There is no concept of `BUILDING_STAFF`, even though the data model already has a `building_staff` table introduced in TASK-2.06.
- Role checks are scattered as hardcoded string literals (`'ADMIN'`, `'LANDLORD'`) across 15+ backend controllers, 3+ backend service classes, and 4+ frontend components/guards — with no abstraction layer. Any role rename requires touching dozens of files with high risk of missed occurrences.
- The `isAdmin()` helper method on the enum encodes a binary world-view (`this == ADMIN`) that will break silently when new privileged roles are introduced.

**Current pain points:**
1. **Security gap**: A `LANDLORD`-role user can log in (the enum value exists, `UserRole.enumRoleOf("Landlord")` resolves) but has no defined access to any endpoint. Their JWT is issued but useless. This is confusing and potentially misleading to operators.
2. **Role hierarchy mismatch**: `SecurityConfig.roleHierarchy()` declares `ADMIN > LANDLORD > STUDENT`, meaning a `LANDLORD` user already inherits `STUDENT` privileges. With multi-tenancy, a building owner must not be able to perform student-facing actions (submit applications, accept offers).
3. **Fragility**: `JwtUtil.generateToken()` hardcodes `"ROLE_ADMIN"` to determine JWT expiration time. Adding `PLATFORM_ADMIN` without updating this check silently gives platform admins a student-length token.
4. **No frontend abstraction**: Guards compare `currentUser.userRole === 'ADMIN'` directly. `UserManagementService` comment says `'ADMIN' or 'LANDLORD'`. These will break or need mass-find-replace on any role change.

**Stakeholders affected:**
- University accommodation administrators (currently `ADMIN` role) — their access must not change
- Platform operators / system superusers — need a clearly differentiated `PLATFORM_ADMIN` role
- Building owners (currently `LANDLORD`) — need a usable `BUILDING_OWNER` role with defined endpoint access
- Building staff (new, no role yet) — need `BUILDING_STAFF` role
- Students — no access change; `STUDENT` role is stable

---

## 2. Stakeholder Analysis

**Primary Stakeholders:**

| Role | Needs | Success Criteria |
|------|-------|------------------|
| Platform Admin (was `ADMIN`) | Continue to manage all tenants, system settings, feature flags, and allocation rules without disruption | Zero change to access after migration; login works; JWT expiry unchanged (4h) |
| Building Owner (was `LANDLORD`) | Usable role with defined access to building management endpoints; currently blocked from all secured endpoints | Can log in and perform building management actions post-migration |
| Building Staff (new) | Role exists in enum and DB; ready for endpoint access assignment in a future task | Role can be assigned; does not break existing access controls |
| Student | No change to their login, application flow, document uploads, or allocation workflow | All student-facing flows work identically before and after migration |
| University Accommodation Admin | System operates correctly; no regression in application review, room allocation, or reporting | All admin flows tested and verified |

**Secondary Stakeholders:**
- Developers: Benefit from a `RoleService` abstraction that replaces scattered string literals
- QA/Ops: Need clear role hierarchy documentation for test coverage and audit logging
- Activity Log consumers: `userRole` appears in exported CSV and API responses — format must remain valid

---

## 3. Functional Requirements

### 3.1 Core Functionality

**FR-1: Extend UserRole enum with v2.0 roles**
- **Priority**: Must Have
- **Description**: The `UserRole` enum shall be extended to include `PLATFORM_ADMIN`, `BUILDING_OWNER`, and `BUILDING_STAFF`. The existing `ADMIN` and `LANDLORD` values shall be retained as `@Deprecated` but not removed until a future clean-up task.
- **Business Rules**:
  - `PLATFORM_ADMIN` represents a system-level operator who can manage any tenant
  - `BUILDING_OWNER` represents the owner of one or more buildings within a single tenant
  - `BUILDING_STAFF` represents staff employed by a building owner within a single tenant
  - `ADMIN` and `LANDLORD` shall remain valid enum values to prevent deserialization failures on any records not yet migrated (defensive programming)
- **Validation Rules**:
  - `UserRole.enumRoleOf()` must resolve all five active role strings (`"Platform_Admin"`, `"Building_Owner"`, `"Building_Staff"`, `"Admin"`, `"Landlord"`) case-insensitively
  - `isAdmin()` must be updated to return `true` for both `ADMIN` and `PLATFORM_ADMIN` for the deprecation window

**FR-2: Flyway data migration of existing user records**
- **Priority**: Must Have
- **Description**: All existing `app_user` records with `user_role = 'ADMIN'` shall be updated to `user_role = 'PLATFORM_ADMIN'`. All records with `user_role = 'LANDLORD'` shall be updated to `user_role = 'BUILDING_OWNER'`.
- **Business Rules**:
  - Migration is irreversible once applied (Flyway immutability)
  - Migration must run atomically — partial updates must roll back entirely
  - Migration timestamp: `V2026_02_25_NNNN__migrate_user_roles.sql` (note: the `V2026_02_24_0013` name in TASKS.md pre-dates subsequent migrations already applied; a later timestamp must be used)
- **Validation Rules**:
  - After migration, zero rows in `app_user` shall have `user_role IN ('ADMIN', 'LANDLORD')`
  - [INFERRED] No foreign key constraints reference `user_role` values directly, so the UPDATE is safe

**FR-3: Update Spring Security role hierarchy**
- **Priority**: Must Have
- **Description**: `SecurityConfig.roleHierarchy()` shall be updated to reflect the new role model: `PLATFORM_ADMIN > BUILDING_OWNER > BUILDING_STAFF`. `STUDENT` is an independent role with no hierarchy relationship to management roles.
- **Business Rules**:
  - `PLATFORM_ADMIN` inherits all permissions of `BUILDING_OWNER` and `BUILDING_STAFF`
  - `BUILDING_OWNER` inherits all permissions of `BUILDING_STAFF`
  - `STUDENT` does NOT inherit from `BUILDING_STAFF` and management roles do NOT inherit `STUDENT` permissions (unlike the current `LANDLORD > STUDENT` which was a design mistake)
  - Existing `hasAnyRole('STUDENT', 'ADMIN')` annotations in `ApplicationController` and `RoomAllocationController` shall be updated to `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`

**FR-4: Replace all `hasRole('ADMIN')` annotations in controllers**
- **Priority**: Must Have
- **Description**: Every `@PreAuthorize("hasRole('ADMIN')")` annotation across all controllers shall be replaced with `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`.
- **Business Rules**:
  - Replacement is 1:1 — no endpoint re-scoping (e.g., no endpoints are being granted to `BUILDING_OWNER` in this task)
  - `hasRole('STUDENT')` annotations are unchanged
  - `isAuthenticated()` annotations are unchanged
  - `hasAnyRole('STUDENT', 'ADMIN')` becomes `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`
- **Affected controllers** (exhaustive):
  - `AcademicYearController` (6 annotations)
  - `AccommodationTypeController` (5 annotations)
  - `ActivityLogController` (2 annotations)
  - `AdminStatsController` (2 annotations)
  - `AllocationRuleController` (1 class-level annotation)
  - `ApplicationController` (10 annotations: ADMIN + ANY)
  - `AuthController` (1 annotation)
  - `BuildingAttributesController` (1 class-level annotation)
  - `BuildingController` (6 annotations)
  - `DocumentController` (5 annotations)
  - `DocumentTypeController` (4 annotations)
  - `FeatureController` (5 annotations)
  - `InstitutionController` (3 annotations)
  - `RoomAllocationController` (12 annotations)

**FR-5: Update service-layer role checks**
- **Priority**: Must Have
- **Description**: All service-layer code that compares `getUserRole().equals(UserRole.ADMIN)` or calls `getUserRole().isAdmin()` shall use a semantically correct check for the new role model.
- **Business Rules**:
  - `isAdmin()` on the enum (updated in FR-1) is the preferred check for backward-compatible service logic
  - Direct enum comparisons (`== UserRole.ADMIN`) must include `PLATFORM_ADMIN`
  - `UserManagementService` validation `role != UserRole.ADMIN && role != UserRole.LANDLORD` must be updated to accept `PLATFORM_ADMIN` and `BUILDING_OWNER`
- **Affected services**:
  - `DocumentCommentService` (3 `isAdmin()` calls)
  - `DocumentLifecycleService` (1 `isAdmin()` call)
  - `DocumentQueryService` (3 `isAdmin()` calls)
  - `DocumentStatusService` (1 `isAdmin()` call)
  - `StudentService` (2 `== UserRole.ADMIN` comparisons)
  - `ApplicationAdminService` (2 `.equals(UserRole.ADMIN)` comparisons)
  - `ApplicationCoreService` (3 `.equals(UserRole.ADMIN)` comparisons)
  - `ApplicationDocumentService` (1 `.equals(UserRole.ADMIN)` comparison)
  - `ApplicationQueryService` (1 `.equals(UserRole.ADMIN)` comparison)
  - `AdminStatsService` (`countByUserRole(UserRole.ADMIN)` — must count `PLATFORM_ADMIN` instead)
  - `ActivityLogService` (`UserRole.valueOf(userRole)` / `enumRoleOf()` — must handle new role names)
  - `UserManagementService` (role validation logic)

**FR-6: Update JWT token expiry logic**
- **Priority**: Must Have
- **Description**: `JwtUtil.generateToken()` hardcodes `"ROLE_ADMIN"` to select the admin-length JWT expiration. This must be updated to `"ROLE_PLATFORM_ADMIN"`.
- **Business Rules**:
  - `PLATFORM_ADMIN` users receive the admin-length token (4 hours, `JWT_ADMIN_EXPIRATION`)
  - All other roles (including `BUILDING_OWNER`, `BUILDING_STAFF`, `STUDENT`) receive the default token (10 hours, `JWT_EXPIRATION`)
  - [INFERRED] `BUILDING_OWNER` and `BUILDING_STAFF` use student-length tokens; confirm this is acceptable

**FR-7: Create frontend `RoleService`**
- **Priority**: Must Have
- **Description**: A `RoleService` shall be created at `frontend/src/app/core/services/role.service.ts`. It shall read the current user's role from `AuthService.currentUserValue` and expose typed role-check methods.
- **Business Rules**:
  - `RoleService` must not duplicate user state — it reads from `AuthService`
  - Must expose: `hasRole(role: Role): boolean`, `isPlatformAdmin(): boolean`, `isStudent(): boolean`, `isBuildingOwner(): boolean`
  - A `Role` enum (TypeScript) shall be defined in `frontend/src/app/core/models/role.model.ts` with values `PLATFORM_ADMIN`, `BUILDING_OWNER`, `BUILDING_STAFF`, `STUDENT`
  - `ADMIN` and `LANDLORD` values may be included in the TS enum for the deprecation window, marked with JSDoc `@deprecated`

**FR-8: Update Angular guards to use `RoleService`**
- **Priority**: Must Have
- **Description**: `admin.guard.ts` and `student.guard.ts` shall be updated to use `RoleService` instead of direct string comparisons.
- **Business Rules**:
  - `AdminGuard`: checks `RoleService.isPlatformAdmin()` instead of `currentUser.userRole === 'ADMIN'`
  - `StudentGuard`: checks `RoleService.isStudent()` instead of `currentUser.userRole === 'STUDENT'`; redirect logic for non-student logged-in users shall use `RoleService.isPlatformAdmin()` for the admin redirect

**FR-9: Update all other frontend hardcoded role strings**
- **Priority**: Must Have
- **Description**: All other occurrences of hardcoded role strings (`'ADMIN'`, `'LANDLORD'`) in Angular components and services shall be replaced with `Role` enum references via `RoleService` or direct `Role` enum usage.
- **Affected files**:
  - `login.component.ts:88` — `user.userRole === 'ADMIN'` post-login redirect
  - `change-password.component.ts:96,138` — `userRole === 'ADMIN'` redirect
  - `application-details.component.ts:96` — `isAdmin = this.currentUser?.userRole === 'ADMIN'`
  - `document-comment-section.component.ts:73` — `isAdmin = currentUser?.userRole === 'ADMIN'`
  - `admin.service.ts:11` — comment `'ADMIN' or 'LANDLORD'`; `CreateUserRequest.userRole` type documentation
  - `activity-logs.component.ts:89` — `userRoles: string[] = ['ADMIN', 'STUDENT', 'LANDLORD']` dropdown options
  - `user-management.component.ts:133,135` — switch cases `'ADMIN'` and `'LANDLORD'`
  - `create-user-dialog.component.ts:49` — default `userRole: ['LANDLORD', ...]`

### 3.2 User Workflows

**Workflow 1: Platform Admin Login (post-migration)**
```
1. PLATFORM_ADMIN user enters credentials
2. AuthService authenticates → JWT issued with ROLE_PLATFORM_ADMIN, expiry = 4 hours
3. Angular app reads userRole = 'PLATFORM_ADMIN' from token
4. login.component.ts redirects to /admin (via RoleService.isPlatformAdmin())
5. AdminGuard.canActivate() → RoleService.isPlatformAdmin() → true → route allowed
6. Admin dashboard loads; all admin-scoped API calls include ROLE_PLATFORM_ADMIN
7. All @PreAuthorize("hasRole('PLATFORM_ADMIN')") endpoints accessible
```

**Workflow 2: Building Owner Login (post-migration)**
```
1. BUILDING_OWNER user enters credentials
2. AuthService authenticates → JWT issued with ROLE_BUILDING_OWNER, expiry = 10 hours
3. Angular app reads userRole = 'BUILDING_OWNER'
4. StudentGuard redirects to /home (not STUDENT, not PLATFORM_ADMIN)
5. AdminGuard blocks access to /admin routes
6. [FUTURE TASK] Building owner dashboard routes guarded by BuildingOwnerGuard
```

**Workflow 3: Admin Creates New User (post-migration)**
```
Admin Role:
1. Opens User Management → Create User dialog
2. Selects role from dropdown: PLATFORM_ADMIN / BUILDING_OWNER / BUILDING_STAFF
3. UserManagementService validates role is not STUDENT
4. AppUserMapper.mapAdminRole() resolves and validates role
5. User created with new role enum value stored in DB
```

---

## 4. User Stories with Acceptance Criteria

### Story 1: Platform Admin Access Unchanged

**As a** platform administrator (previously `ADMIN`)  
**I want to** log in and access all management features without any changes  
**So that** the migration is transparent and does not disrupt daily operations

**Acceptance Criteria:**

```gherkin
Given a user exists in app_user with user_role = 'PLATFORM_ADMIN' (migrated from 'ADMIN')
When the user logs in with valid credentials
Then a JWT is issued containing ROLE_PLATFORM_ADMIN
  And the JWT expiry is 4 hours (admin expiration)
  And the Angular app redirects to /admin
  And AdminGuard.canActivate() returns true
  And all previously accessible endpoints remain accessible
```

**Edge Cases:**
- **EC-1**: User logged in before migration with cached JWT containing `ROLE_ADMIN` → Old token must continue working until expiry (Spring Security role hierarchy mapping); OR token must be invalidated at migration time — [INFERRED: since JWT is stateless, existing `ROLE_ADMIN` tokens continue to work until expiry unless a token blacklist is introduced; this is acceptable for a short migration window]
- **EC-2**: Admin JWT expires mid-session during migration deployment → User re-authenticates and receives `ROLE_PLATFORM_ADMIN` token

### Story 2: Building Owner Gets Usable Role

**As a** building owner (previously `LANDLORD`)  
**I want to** log in and receive a valid JWT with a defined role  
**So that** I can perform building management tasks

**Acceptance Criteria:**

```gherkin
Given a user exists in app_user with user_role = 'BUILDING_OWNER' (migrated from 'LANDLORD')
When the user logs in with valid credentials
Then a JWT is issued containing ROLE_BUILDING_OWNER
  And the JWT expiry is 10 hours (default expiration)
  And the Angular app does not crash or redirect incorrectly
```

**Edge Cases:**
- **EC-1**: Building owner attempts to access /admin routes → AdminGuard returns false, redirected to /dashboard
- **EC-2**: Building owner attempts to access /student routes → StudentGuard returns false, redirected to /home

### Story 3: Student Flow Unaffected

**As a** student  
**I want to** log in, submit applications, and manage documents without any change  
**So that** the role migration does not cause regressions in my accommodation workflow

**Acceptance Criteria:**

```gherkin
Given a student user with user_role = 'STUDENT'
When the student logs in
Then a JWT is issued with ROLE_STUDENT and 10-hour expiry
  And StudentGuard.canActivate() returns true for student routes
  And all @PreAuthorize("hasRole('STUDENT')") endpoints remain accessible
  And @PreAuthorize("hasAnyRole('STUDENT', 'PLATFORM_ADMIN')") endpoints remain accessible
```

**Edge Cases:**
- **EC-1**: Student tries to access /admin → AdminGuard blocks, redirect to /dashboard (no change)
- **EC-2**: Student JWT contains `ROLE_STUDENT`; role hierarchy `PLATFORM_ADMIN > BUILDING_OWNER > BUILDING_STAFF` is separate — student does NOT inherit management permissions

### Story 4: Admin Creates Building Owner User

**As a** platform administrator  
**I want to** create a new user with `BUILDING_OWNER` role via User Management  
**So that** I can onboard building owners onto the platform

**Acceptance Criteria:**

```gherkin
Given I am logged in as PLATFORM_ADMIN
When I create a user with role = 'BUILDING_OWNER' via POST /api/admin/users
Then the user is saved with user_role = 'BUILDING_OWNER'
  And no validation error is thrown
  And the response includes userRole = 'BUILDING_OWNER'
```

**Edge Cases:**
- **EC-1**: Admin attempts to create a user with role = `STUDENT` via admin endpoint → validation error (students self-register)
- **EC-2**: Admin attempts to create a user with role = `BUILDING_STAFF` → should this be allowed? [INFERRED: Yes, BUILDING_STAFF is an assignable admin-created role]
- **EC-3**: Legacy API call with role = `'LANDLORD'` (from old client) → `enumRoleOf("Landlord")` still resolves during deprecation window; behavior is unchanged

### Story 5: Role Service Abstracts Frontend Role Checks

**As a** frontend developer  
**I want** all role checks to go through `RoleService`  
**So that** future role changes require editing one file, not dozens

**Acceptance Criteria:**

```gherkin
Given RoleService is injected into a component
When I call RoleService.isPlatformAdmin()
Then it returns true only for a user with userRole === 'PLATFORM_ADMIN'
  And it returns false for 'STUDENT', 'BUILDING_OWNER', 'BUILDING_STAFF'

When I call RoleService.isStudent()
Then it returns true only for userRole === 'STUDENT'

When I call RoleService.hasRole(Role.BUILDING_OWNER)
Then it returns true only for userRole === 'BUILDING_OWNER'
```

**Edge Cases:**
- **EC-1**: `currentUser` is null (not logged in) → all `RoleService` checks return false without throwing
- **EC-2**: `currentUser.userRole` contains an unknown value → `hasRole()` returns false, does not throw

### Story 6: Activity Log Export Preserves Role Values

**As an** administrator  
**I want** the activity log CSV export to show the correct role names  
**So that** audit records remain accurate and parseable

**Acceptance Criteria:**

```gherkin
Given activity logs exist for users with PLATFORM_ADMIN and BUILDING_OWNER roles
When I export activity logs as CSV
Then the userRole column shows 'PLATFORM_ADMIN' or 'BUILDING_OWNER'
  And the role filter dropdown in the UI includes 'PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF', 'STUDENT'
  And filtering by 'PLATFORM_ADMIN' returns only PLATFORM_ADMIN user activity
```

**Edge Cases:**
- **EC-1**: Activity log record references a user deleted post-migration → `userRole` in log is null; no NPE
- **EC-2**: Filter dropdown requests role `'ADMIN'` (old bookmark/URL) → `ActivityLogService` handles gracefully; either resolves via `enumRoleOf` or returns empty results with no 500 error

---

## 5. Non-Functional Requirements

### 5.1 Performance
- **NFR-P1**: Flyway migration executes in < 1 second for typical university deployment (< 1,000 users)
- **NFR-P2**: No additional database queries introduced — role is already stored on the `AppUser` entity loaded from the security context

### 5.2 Security
- **NFR-S1**: Zero endpoints shall be inadvertently downgraded (access widened) by this migration
- **NFR-S2**: Zero endpoints shall be inadvertently upgraded (access denied to legitimate users) by this migration
- **NFR-S3**: The role hierarchy change must not grant management roles student-level access (remove `LANDLORD > STUDENT` from hierarchy)
- **NFR-S4**: JWT token expiry for `PLATFORM_ADMIN` must remain 4 hours; all other roles 10 hours
- **NFR-S5**: All `@PreAuthorize` annotations must be verified by a `SecurityConfigTest`

### 5.3 Usability
- **NFR-U1**: The user-facing role label in the UI (`user-management.component.ts` switch case) shall display "Platform Admin", "Building Owner", "Building Staff" cleanly
- **NFR-U2**: The Create User dialog default role should change from `LANDLORD` to `BUILDING_OWNER`

### 5.4 Reliability
- **NFR-R1**: Migration must be atomic — no partial state is acceptable
- **NFR-R2**: Application startup shall fail fast (Flyway exception) if migration is rolled back manually, preventing operation in an inconsistent state

### 5.5 Backward Compatibility
- **NFR-BC1**: During the deprecation window, `UserRole.enumRoleOf("Admin")` and `UserRole.enumRoleOf("Landlord")` must continue to resolve to `ADMIN` and `LANDLORD` respectively (not fail)
- **NFR-BC2**: JWTs issued before migration (containing `ROLE_ADMIN`) shall continue to be accepted by Spring Security until their natural expiry. Since JWT is stateless and there is no blacklist, no forced invalidation occurs.

---

## 6. Data Requirements

### 6.1 Data to Capture

| Field Name | Type | Required | Validation | Notes |
|------------|------|----------|------------|-------|
| `user_role` | `VARCHAR` (stored as enum name) | Yes | Must match a valid `UserRole` enum value | Existing column on `app_user` — no schema change needed |

### 6.2 Data Relationships
- `app_user.user_role` stores the enum name as a string (e.g., `'PLATFORM_ADMIN'`)
- `ActivityLog.userRole` (in service layer) reads from `app_user.user_role` at log time — value captured at write time, not updated retroactively

### 6.3 Data Migration
- **Migration 1**: `UPDATE app_user SET user_role = 'PLATFORM_ADMIN' WHERE user_role = 'ADMIN'`
- **Migration 2**: `UPDATE app_user SET user_role = 'BUILDING_OWNER' WHERE user_role = 'LANDLORD'`
- No schema change is required — the `user_role` column is already `VARCHAR`/enum string
- Existing `activity_log` records that captured `userRole = 'ADMIN'` are historical records and shall NOT be retroactively updated (audit trail integrity)

---

## 7. Integration Points

### 7.1 Internal Integrations
- **JWT Token**: `JwtUtil.generateToken()` embeds role as `ROLE_<enum_name>` in claims — must reflect new role names
- **Spring Security**: Role hierarchy expression handler used by all `@PreAuthorize` — updated in `SecurityConfig`
- **MapStruct**: `AppUserMapper.mapAdminRole()` validates role is not STUDENT — must accept new admin roles
- **ActivityLog**: `ActivityLogService` uses `UserRole.valueOf()` and `enumRoleOf()` for filter parsing — must handle new role names in API requests

### 7.2 External Systems
- None — role management is fully internal

### 7.3 Data Import/Export
- Activity log CSV export includes `userRole` column — values will change from `ADMIN` to `PLATFORM_ADMIN` for new log entries after migration

---

## 8. UI/UX Requirements

### 8.1 Affected Pages
- **User Management page** (`/admin/users`): Role badge display; create user role dropdown
- **Activity Logs page** (`/admin/activity-logs`): Role filter dropdown
- **Login redirect** (`/login`): Post-login role-based navigation
- **Change Password page**: Post-password-change redirect
- **Application Details**: Admin/student conditional rendering
- **Document Comment Section**: Admin/student conditional rendering

### 8.2 Role Display Labels

| Enum Value | Display Label |
|------------|---------------|
| `PLATFORM_ADMIN` | Platform Admin |
| `BUILDING_OWNER` | Building Owner |
| `BUILDING_STAFF` | Building Staff |
| `STUDENT` | Student |

### 8.3 Dropdown Updates
- Activity logs filter: `['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF', 'STUDENT']`
- Create user dialog: Default to `BUILDING_OWNER`; remove `LANDLORD`; add `BUILDING_STAFF`

---

## 9. Business Rules and Constraints

**BR-1: Role Hierarchy is Management-Only**
- **Rule**: The new role hierarchy `PLATFORM_ADMIN > BUILDING_OWNER > BUILDING_STAFF` applies only within management roles. `STUDENT` is a separate, independent role.
- **Rationale**: In the old model, `ADMIN > LANDLORD > STUDENT` incorrectly implied a property manager inherits student permissions. A building owner should never be able to submit student accommodation applications.
- **Exception Handling**: Any endpoint requiring `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')` explicitly lists both — `PLATFORM_ADMIN` does NOT inherit `STUDENT` via hierarchy.

**BR-2: ADMIN and LANDLORD are Deprecated, Not Removed**
- **Rule**: The `ADMIN` and `LANDLORD` enum values shall be retained and annotated `@Deprecated`. They shall not appear in new `@PreAuthorize` annotations or new frontend role checks.
- **Rationale**: Prevents deserialization failures for any edge case (e.g., caching layer, third-party integration) that might present an old role value. Allows a clean removal in a subsequent task once zero records use these values.
- **Exception Handling**: `UserManagementService` shall refuse to create new users with deprecated roles.

**BR-3: Migration Timestamp Correctness**
- **Rule**: The Flyway migration file must use a timestamp strictly greater than `V2026_02_24_0030` (last applied migration). [INFERRED: `V2026_02_25_0001` is appropriate given today's date is 2026-02-25]
- **Rationale**: Flyway applies migrations in version order; an out-of-order migration causes startup failure.

**BR-4: No Endpoint Re-Scoping in This Task**
- **Rule**: This task is a rename migration only. No new endpoints shall be opened to `BUILDING_OWNER` or `BUILDING_STAFF` as part of TASK-2.03.
- **Rationale**: Endpoint access for new roles is a separate design concern (TASK-2.12 or later). Mixing concerns increases risk.

---

## 10. Success Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Zero access regressions | 0 endpoints changed in effective access | `SecurityConfigTest` covers all controller annotations |
| Zero `ADMIN`/`LANDLORD` in DB post-migration | 0 records | SQL: `SELECT COUNT(*) FROM app_user WHERE user_role IN ('ADMIN', 'LANDLORD')` returns 0 |
| All frontend role checks abstracted | 0 raw string comparisons to `'ADMIN'`/`'LANDLORD'` remaining | ESLint rule or code search post-implementation |
| Build passes | 0 compilation errors, 0 test regressions | `./gradlew build` and `npm run build` succeed |

---

## 11. Edge Cases and Error Scenarios

### 11.1 Edge Cases

**EC-1: In-Flight JWT at Migration Time**
- **Description**: A PLATFORM_ADMIN user is mid-session with a JWT containing `ROLE_ADMIN` when the migration deploys
- **Expected Behavior**: The old `ROLE_ADMIN` token continues to be accepted until expiry (Spring Security does not validate against DB roles on each request). Within 4 hours maximum, the user re-authenticates and receives a `ROLE_PLATFORM_ADMIN` token.
- **Frequency**: Occasional (during deployment windows)

**EC-2: Old Client Sending `userRole: 'LANDLORD'` to Create User API**
- **Description**: An older API client (e.g., cached admin frontend session from before a full frontend redeploy) sends `userRole: 'LANDLORD'` to `POST /api/admin/users`
- **Expected Behavior**: `UserRole.enumRoleOf("Landlord")` resolves to `LANDLORD` (deprecated but still valid). `UserManagementService` should reject creation of new users with deprecated roles and return HTTP 400 with a clear error: "Role LANDLORD is deprecated; use BUILDING_OWNER"
- **Frequency**: Rare (edge case during rolling deployment)

**EC-3: Activity Log Filter with Deprecated Role Name**
- **Description**: An admin bookmarks `/admin/activity-logs?userRole=ADMIN`. After migration, the filter query parameter is `ADMIN`.
- **Expected Behavior**: `ActivityLogService` calls `UserRole.valueOf("ADMIN")` which succeeds (enum still exists). Filters by `ADMIN` role — which after data migration returns zero results. No 500 error. Empty result set with correct HTTP 200.
- **Frequency**: Rare

**EC-4: AdminStatsService Counting Admins**
- **Description**: `AdminStatsService.getTotalStats()` calls `appUserRepository.countByUserRole(UserRole.ADMIN)`. Post-migration, no users have `ADMIN` role — this returns 0.
- **Expected Behavior**: Must count `PLATFORM_ADMIN` instead. The stat "Total Admins" must reflect platform admins.
- **Frequency**: Every time admin stats dashboard is loaded (Common)

**EC-5: RoleService Called Before Login**
- **Description**: An Angular component initializes and calls `RoleService.isPlatformAdmin()` before any user is authenticated.
- **Expected Behavior**: `AuthService.currentUserValue` is null; `RoleService` returns false without throwing a NullPointerException equivalent.
- **Frequency**: Common (initial app load, unauthenticated routes)

**EC-6: Concurrent User Creation During Migration**
- **Description**: A new user is created via the API between the time the Flyway migration runs and the application restarts with the new enum
- **Expected Behavior**: Not applicable — Flyway runs before application beans initialize in Spring Boot; there is no concurrent creation window during a standard rolling deployment
- **Frequency**: Not applicable

**EC-7: AppUserRepository.findByUserRole(UserRole.ADMIN) Post-Migration**
- **Description**: Any code calling `findByUserRole(UserRole.ADMIN)` will return empty list after migration
- **Expected Behavior**: Any such calls must be updated to use `PLATFORM_ADMIN`; none identified in current codebase beyond `AdminStatsService` (counted in EC-4)
- **Frequency**: Triggered on stats queries

**EC-8: UserRole.enumRoleOf() with Mixed-Case Input**
- **Description**: The `enumRoleOf()` method uses `getRole()` (display name, e.g., "Admin") case-insensitively. New roles use display names "Platform_Admin", "Building_Owner", "Building_Staff" — these must be consistent.
- **Expected Behavior**: `enumRoleOf("Platform_Admin")`, `enumRoleOf("PLATFORM_ADMIN")`, `enumRoleOf("platform_admin")` all resolve to `PLATFORM_ADMIN`. Note: the current implementation compares against `getRole()` (display string), not `name()` (enum constant). The display string must be carefully chosen.
- **Frequency**: Every user creation and login

### 11.2 Error Scenarios

| Error Type | User Action | System Response | User Message |
|------------|-------------|-----------------|--------------|
| Deprecated role in create user request | Admin submits `userRole: 'LANDLORD'` | HTTP 400 | "Role LANDLORD is deprecated. Use BUILDING_OWNER." |
| STUDENT role in admin create user | Admin submits `userRole: 'STUDENT'` | HTTP 400 | "Cannot create STUDENT user via admin endpoint." |
| Unknown role in JWT | Tampered JWT with unknown role | HTTP 403 | "Access Denied" |
| AdminGuard blocks BUILDING_OWNER | Building owner navigates to /admin | Redirect to /dashboard | (No error message; silent redirect) |

---

## 12. Dependencies and Prerequisites

**Technical Dependencies:**
- Flyway migration `V2026_02_25_NNNN__migrate_user_roles.sql` must execute before new enum values are used at runtime
- Spring Boot Flyway runs migrations at startup before bean initialization — no application code runs between migration and enum load
- No schema changes needed: `user_role` column is already `VARCHAR`

**Feature Dependencies:**
- TASK-2.06 (building_owner, building_staff tables) is complete — `BUILDING_OWNER` and `BUILDING_STAFF` roles are semantically supported by the data model
- TASK-2.07 (multi-tenancy schema) is complete — tenant_id on app_user is in place

**Data Dependencies:**
- Existing `app_user` records must be migrated before any role-check logic is updated; migration must run first in the same deployment

---

## 13. Out of Scope

**Explicitly NOT included in this task:**
- Granting `BUILDING_OWNER` or `BUILDING_STAFF` access to any specific endpoints (separate future task)
- Implementing a `BuildingOwnerGuard` in Angular
- Email notification to users whose role was migrated
- Adding a building owner dashboard or any building owner UI
- Removing `@Deprecated` `ADMIN` and `LANDLORD` enum values (clean-up task after zero DB records confirmed)
- Multi-tenant isolation enforcement for building owners (separate architecture task)

**Future Enhancements (Parking Lot):**
- `BUILDING_STAFF` endpoint access definitions
- Per-tenant admin role (`TENANT_ADMIN`) distinct from `PLATFORM_ADMIN`
- Role assignment audit trail (who granted which role, when)

---

## 14. Assumptions and Open Questions

**Assumptions:**
- `[INFERRED]` The `BUILDING_OWNER` and `BUILDING_STAFF` roles receive 10-hour JWTs (standard student expiry), not the 4-hour admin expiry. This should be confirmed.
- `[INFERRED]` Historical `activity_log` records showing `userRole = 'ADMIN'` are NOT retroactively updated. Audit trail integrity takes precedence.
- `[INFERRED]` No external systems (third-party integrations, webhooks) consume role values from this system's API responses. If any exist, they must be notified of the role name change.
- `[INFERRED]` `BUILDING_STAFF` is an assignable role but has no endpoint access defined yet. The enum value exists as forward-scaffolding.
- The Flyway migration name shall use `V2026_02_25_0001` as the timestamp (or the next available sequence number on that date).

**Open Questions for Architect/Developer:**
1. Should the `userRole` field in API responses (`UserData.getUserRole()`, `AuthController` login response) return the raw enum name (`PLATFORM_ADMIN`) or a formatted label (`Platform Admin`)? **Currently returns raw enum name — recommend keeping as-is for consistency.**
2. Should `BUILDING_OWNER` receive the 4-hour admin token or the 10-hour default token? [Currently assumed: 10-hour]
3. Should `UserManagementService` allow creating `BUILDING_STAFF` users, or is that only possible via a future building owner self-service flow?
4. After this migration, should the `AdminGuard` be renamed to `PlatformAdminGuard` for clarity, or is that out of scope for this task?

---

## 15. Approval and Sign-off

**Estimated Business Value**: High — enables multi-tenant role model; unblocks building owner onboarding; removes security ambiguity

**Risk Assessment:**

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Missed `@PreAuthorize` annotation causes access regression | High | Medium | Exhaustive grep before and after; `SecurityConfigTest` covers all annotations |
| Flyway migration uses wrong timestamp (out of order) | High | Low | Check latest migration file before naming new one |
| In-flight JWT with old `ROLE_ADMIN` claim after deploy | Low | High | Acceptable; tokens expire within 4 hours; no session state to invalidate |
| Frontend component missing `RoleService` update causes wrong redirect | Medium | Medium | Full search for hardcoded `'ADMIN'` strings; component tests |
| `AdminStatsService` returns 0 admins after migration (no update) | Medium | High | Explicitly listed in FR-5; must be caught in test |

---

## Analysis Checklist

- [x] Clear problem statement with business context
- [x] All stakeholders identified with their needs
- [x] 6 user stories with Given/When/Then acceptance criteria
- [x] Edge cases analyzed (8 scenarios)
- [x] Data requirements fully specified
- [x] Integration points identified
- [x] UI/UX requirements described
- [x] Business rules documented with rationale
- [x] Success metrics defined and measurable
- [x] Out-of-scope items clearly listed
- [x] Security considerations addressed (NFR-S1 through NFR-S5)
- [x] Dependencies and prerequisites listed
- [x] Open questions flagged for technical team
