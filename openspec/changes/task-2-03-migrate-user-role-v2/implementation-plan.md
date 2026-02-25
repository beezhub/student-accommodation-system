# Implementation Plan
## TASK-2.03 — Migrate UserRole to v2.0 Role Model

**Estimated Total Effort**: 5–7 hours

**Critical Path**: Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Task 8 → Task 9 → Task 10 → Task 11

**Parallel Work Opportunities**:
- Tasks 8–11 (frontend) can begin in parallel with Tasks 4–7 (backend service/controller work)
- Tasks 9 and 10 can be developed simultaneously once Task 8 is complete

---

## Task Breakdown

---

### Task 1 — Flyway Data Migration

- **File**: `backend/src/main/resources/db/migration/V2026_02_25_0001__migrate_user_roles.sql`
- **Estimated Effort**: 15 min
- **Dependencies**: None
- **Description**: Create the data-only migration that renames `ADMIN` → `PLATFORM_ADMIN` and `LANDLORD` → `BUILDING_OWNER` in the `app_user` table. No schema change required — the `user_role` column is already `VARCHAR`.

```sql
-- Rename ADMIN → PLATFORM_ADMIN
UPDATE app_user
SET user_role = 'PLATFORM_ADMIN'
WHERE user_role = 'ADMIN';

-- Rename LANDLORD → BUILDING_OWNER
UPDATE app_user
SET user_role = 'BUILDING_OWNER'
WHERE user_role = 'LANDLORD';
```

- **Acceptance Criteria**:
  - [ ] File named `V2026_02_25_0001__migrate_user_roles.sql` (sorts after `V2026_02_24_0030`)
  - [ ] Both UPDATE statements in a single migration (Flyway wraps in one transaction)
  - [ ] Application starts without Flyway errors
  - [ ] `SELECT COUNT(*) FROM app_user WHERE user_role IN ('ADMIN', 'LANDLORD')` returns 0

---

### Task 2 — Extend UserRole Enum

- **File**: `backend/src/main/java/com/beezhub/student_accommodation/model/enums/UserRole.java`
- **Estimated Effort**: 20 min
- **Dependencies**: None (can run parallel with Task 1)
- **Description**: Add `PLATFORM_ADMIN`, `BUILDING_OWNER`, `BUILDING_STAFF`. Keep `ADMIN` and `LANDLORD` as `@Deprecated`. Update `isAdmin()` to return `true` for both `PLATFORM_ADMIN` and the deprecated `ADMIN`.

```java
@ToString
@Getter
public enum UserRole {
    STUDENT("Student"),
    PLATFORM_ADMIN("Platform_Admin"),
    BUILDING_OWNER("Building_Owner"),
    BUILDING_STAFF("Building_Staff"),
    @Deprecated ADMIN("Admin"),
    @Deprecated LANDLORD("Landlord");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public static UserRole enumRoleOf(String role) {
        return Arrays.stream(UserRole.values())
                .filter(userRole -> userRole.getRole().equalsIgnoreCase(role))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + role));
    }

    /**
     * Returns true if this role has administrative privileges.
     * Covers both PLATFORM_ADMIN and the deprecated ADMIN during migration window.
     */
    public boolean isAdmin() {
        return this == PLATFORM_ADMIN || this == ADMIN;
    }
}
```

- **Acceptance Criteria**:
  - [ ] `enumRoleOf("Platform_Admin")` returns `PLATFORM_ADMIN`
  - [ ] `enumRoleOf("PLATFORM_ADMIN")` returns `PLATFORM_ADMIN` (case-insensitive match)
  - [ ] `enumRoleOf("Admin")` still returns `ADMIN` (backward compat)
  - [ ] `PLATFORM_ADMIN.isAdmin()` returns `true`
  - [ ] `ADMIN.isAdmin()` returns `true`
  - [ ] `STUDENT.isAdmin()` returns `false`
  - [ ] `BUILDING_OWNER.isAdmin()` returns `false`
  - [ ] `./gradlew build` succeeds — no compilation errors

---

### Task 3 — Fix JWT Expiry Check + Security Config

- **Files**:
  - `backend/src/main/java/com/beezhub/student_accommodation/security/jwt/JwtUtil.java`
  - `backend/src/main/java/com/beezhub/student_accommodation/config/SecurityConfig.java`
- **Estimated Effort**: 20 min
- **Dependencies**: Task 2
- **Description**: Two surgical changes — update the role string checked in `JwtUtil.generateToken()` and update the role hierarchy + URL-level matchers in `SecurityConfig`.

**JwtUtil.java** — line 69, change one string:
```java
// Before:
if (roles.contains("ROLE_ADMIN")) {
// After:
if (roles.contains("ROLE_PLATFORM_ADMIN")) {
```

**SecurityConfig.java** — update two `hasRole` matchers and the hierarchy:
```java
// URL-level matchers (lines 77, 79):
.requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
.requestMatchers("/actuator/**").hasRole("PLATFORM_ADMIN")

// Role hierarchy (lines 96-100):
@Bean
public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_PLATFORM_ADMIN > ROLE_BUILDING_OWNER \n" +
        "ROLE_BUILDING_OWNER > ROLE_BUILDING_STAFF"
    );
}
// Update Javadoc comment above roleHierarchy() to reflect new hierarchy
```

- **Acceptance Criteria**:
  - [ ] `PLATFORM_ADMIN` users receive 4-hour JWT (admin expiration)
  - [ ] `BUILDING_OWNER` users receive 10-hour JWT (default expiration)
  - [ ] `STUDENT` users receive 10-hour JWT (default expiration)
  - [ ] `ROLE_STUDENT` is NOT in the hierarchy (no management role inherits student permissions)
  - [ ] `./gradlew build` succeeds

---

### Task 4 — Update @PreAuthorize Annotations Across All Controllers

- **Files** (14 controllers):
  - `controller/AcademicYearController.java`
  - `controller/AccommodationTypeController.java`
  - `controller/ActivityLogController.java`
  - `controller/AdminStatsController.java`
  - `controller/AllocationRuleController.java`
  - `controller/ApplicationController.java`
  - `controller/AuthController.java`
  - `controller/BuildingAttributesController.java`
  - `controller/BuildingController.java`
  - `controller/DocumentController.java`
  - `controller/DocumentTypeController.java`
  - `controller/FeatureController.java`
  - `controller/InstitutionController.java`
  - `controller/RoomAllocationController.java`
- **Estimated Effort**: 45 min
- **Dependencies**: Task 2
- **Description**: Mass find-and-replace. Two substitution rules:
  1. `hasRole('ADMIN')` → `hasRole('PLATFORM_ADMIN')`
  2. `hasAnyRole('STUDENT', 'ADMIN')` → `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`

  `hasRole('STUDENT')` and `isAuthenticated()` are untouched. Also update `SecurityConfig` URL-level matchers (done in Task 3).

  **Also check**: `StagedAllocationController`, `UserManagementController`, `SystemSettingController`, `RoomController`, `StudentController`, `YearOfStudyController` — verify they also use ADMIN and apply the same replacement if present.

- **Acceptance Criteria**:
  - [ ] `rg "hasRole\('ADMIN'\)" backend/src/main/java` returns zero results
  - [ ] `rg "hasAnyRole.*'ADMIN'" backend/src/main/java` returns zero results  
  - [ ] `rg "hasRole\('PLATFORM_ADMIN'\)" backend/src/main/java` returns results for all previously-ADMIN endpoints
  - [ ] `./gradlew build` succeeds — no compilation errors

---

### Task 5 — Update Service-Layer Role Comparisons

- **Files**:
  - `service/StudentService.java` — 2 `== UserRole.ADMIN` comparisons → `getUserRole().isAdmin()`
  - `service/application/ApplicationAdminService.java` — 2 `.equals(UserRole.ADMIN)` → `.isAdmin()`
  - `service/application/ApplicationCoreService.java` — 3 `.equals(UserRole.ADMIN)` → `.isAdmin()`
  - `service/application/ApplicationDocumentService.java` — 1 `.equals(UserRole.ADMIN)` → `.isAdmin()`
  - `service/application/ApplicationQueryService.java` — 1 `.equals(UserRole.ADMIN)` → `.isAdmin()`
  - `service/AdminStatsService.java` — `countByUserRole(UserRole.ADMIN)` → `countByUserRole(UserRole.PLATFORM_ADMIN)`
  - `service/UserManagementService.java` — validation logic (see below)
- **Estimated Effort**: 45 min
- **Dependencies**: Task 2
- **Description**:
  - For all services except `AdminStatsService` and `UserManagementService`: replace direct `== UserRole.ADMIN` / `.equals(UserRole.ADMIN)` with `.isAdmin()` — because `isAdmin()` now covers both `PLATFORM_ADMIN` and deprecated `ADMIN`.
  - `AdminStatsService`: the stat "Total Admins" must count `PLATFORM_ADMIN` users, not the now-empty `ADMIN` role. Change `countByUserRole(UserRole.ADMIN)` → `countByUserRole(UserRole.PLATFORM_ADMIN)`.
  - `UserManagementService`: update the role validation to reject `STUDENT` (cannot create via admin endpoint) and reject deprecated `ADMIN`/`LANDLORD`:

```java
// In UserManagementService — role validation method:
private void validateAdminCreatableRole(UserRole role) {
    if (role == UserRole.STUDENT) {
        throw new ValidationException("Cannot create STUDENT user via admin endpoint.");
    }
    if (role == UserRole.ADMIN || role == UserRole.LANDLORD) {
        throw new ValidationException(
            "Role " + role.name() + " is deprecated. Use PLATFORM_ADMIN or BUILDING_OWNER.");
    }
}
```

  **Note**: `DocumentCommentService`, `DocumentLifecycleService`, `DocumentQueryService`, `DocumentStatusService` all use `isAdmin()` — they require NO changes since `isAdmin()` is updated in Task 2.

- **Acceptance Criteria**:
  - [ ] `rg "UserRole\.ADMIN" backend/src/main/java` returns only `UserRole.java` (the enum definition) and `UserManagementService` (the deprecation rejection check)
  - [ ] `AdminStatsService` counts `PLATFORM_ADMIN` users
  - [ ] `UserManagementService` throws on `STUDENT`, `ADMIN`, `LANDLORD` creation attempts
  - [ ] `UserManagementService` allows `PLATFORM_ADMIN`, `BUILDING_OWNER`, `BUILDING_STAFF`
  - [ ] `./gradlew build` succeeds

---

### Task 6 — Backend Tests

- **Files** (create new):
  - `backend/src/test/java/com/beezhub/student_accommodation/model/enums/UserRoleTest.java`
  - `backend/src/test/java/com/beezhub/student_accommodation/config/SecurityConfigTest.java`
  - `backend/src/test/java/com/beezhub/student_accommodation/service/UserManagementServiceRoleValidationTest.java`
- **Estimated Effort**: 60 min
- **Dependencies**: Tasks 2, 3, 4, 5

**UserRoleTest.java**:
```java
@Test
void enumRoleOf_platformAdminDisplayName_returnsPlatformAdmin() {
    assertEquals(UserRole.PLATFORM_ADMIN, UserRole.enumRoleOf("Platform_Admin"));
}

@Test
void enumRoleOf_platformAdminUpperCase_returnsPlatformAdmin() {
    assertEquals(UserRole.PLATFORM_ADMIN, UserRole.enumRoleOf("PLATFORM_ADMIN"));
}

@Test
void enumRoleOf_adminDisplayName_returnsDeprecatedAdmin() {
    assertEquals(UserRole.ADMIN, UserRole.enumRoleOf("Admin"));
}

@Test
void isAdmin_platformAdmin_returnsTrue() { assertTrue(UserRole.PLATFORM_ADMIN.isAdmin()); }

@Test
void isAdmin_deprecatedAdmin_returnsTrue() { assertTrue(UserRole.ADMIN.isAdmin()); }

@Test
void isAdmin_student_returnsFalse() { assertFalse(UserRole.STUDENT.isAdmin()); }

@Test
void isAdmin_buildingOwner_returnsFalse() { assertFalse(UserRole.BUILDING_OWNER.isAdmin()); }

@Test
void isAdmin_buildingStaff_returnsFalse() { assertFalse(UserRole.BUILDING_STAFF.isAdmin()); }
```

**SecurityConfigTest.java** (integration, `@SpringBootTest` + `@AutoConfigureMockMvc`):
```java
@Test
@WithMockUser(roles = "PLATFORM_ADMIN")
void adminEndpoint_platformAdminRole_isAccessible() throws Exception {
    mockMvc.perform(get("/api/admin/stats")).andExpect(status().isOk());
}

@Test
@WithMockUser(roles = "BUILDING_OWNER")
void adminEndpoint_buildingOwnerRole_isForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/stats")).andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "STUDENT")
void adminEndpoint_studentRole_isForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/stats")).andExpect(status().isForbidden());
}

@Test
void adminEndpoint_noAuth_isUnauthorized() throws Exception {
    mockMvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
}
```

**UserManagementServiceRoleValidationTest.java** (unit, `@ExtendWith(MockitoExtension.class)`):
```java
@Test
void createUser_withBuildingOwnerRole_succeeds() { /* valid — no exception */ }

@Test
void createUser_withBuildingStaffRole_succeeds() { /* valid — no exception */ }

@Test
void createUser_withStudentRole_throwsValidationException() { /* must throw */ }

@Test
void createUser_withDeprecatedAdminRole_throwsValidationException() { /* must throw */ }

@Test
void createUser_withDeprecatedLandlordRole_throwsValidationException() { /* must throw */ }
```

- **Acceptance Criteria**:
  - [ ] All new tests pass: `./gradlew test`
  - [ ] No existing tests regress
  - [ ] `./gradlew jacocoTestReport` shows `UserRole` class at 100% coverage

---

### Task 7 — Verify Full Backend Build

- **Command**: `./gradlew clean build`
- **Estimated Effort**: 10 min
- **Dependencies**: Tasks 1–6
- **Description**: Confirm clean build with all tests passing before starting frontend work.
- **Acceptance Criteria**:
  - [ ] `BUILD SUCCESSFUL`
  - [ ] Zero test failures
  - [ ] Zero compilation warnings about deprecated enum usage in non-deprecated code paths

---

### Task 8 — Frontend Role Model + RoleService

- **Files** (create new):
  - `frontend/src/app/core/models/role.model.ts`
  - `frontend/src/app/core/services/role.service.ts`
- **Estimated Effort**: 30 min
- **Dependencies**: None (can run in parallel with Tasks 3–7)

**role.model.ts**:
```typescript
export enum Role {
  PLATFORM_ADMIN = 'PLATFORM_ADMIN',
  BUILDING_OWNER = 'BUILDING_OWNER',
  BUILDING_STAFF = 'BUILDING_STAFF',
  STUDENT = 'STUDENT',
  /** @deprecated Use PLATFORM_ADMIN */
  ADMIN = 'ADMIN',
  /** @deprecated Use BUILDING_OWNER */
  LANDLORD = 'LANDLORD',
}
```

**role.service.ts**:
```typescript
import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { Role } from '../models/role.model';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  constructor(private authService: AuthService) {}

  hasRole(role: Role): boolean {
    return this.authService.currentUserValue?.userRole === role;
  }

  isPlatformAdmin(): boolean {
    return this.hasRole(Role.PLATFORM_ADMIN);
  }

  isStudent(): boolean {
    return this.hasRole(Role.STUDENT);
  }

  isBuildingOwner(): boolean {
    return this.hasRole(Role.BUILDING_OWNER);
  }

  isBuildingStaff(): boolean {
    return this.hasRole(Role.BUILDING_STAFF);
  }
}
```

- **Acceptance Criteria**:
  - [ ] `role.model.ts` compiles with `npm run build`
  - [ ] `role.service.ts` compiles, `RoleService` is injectable
  - [ ] `isPlatformAdmin()` returns `false` when `currentUserValue` is `null` (no NPE)

---

### Task 9 — Update Angular Guards

- **Files**:
  - `frontend/src/app/core/guards/admin.guard.ts`
  - `frontend/src/app/core/guards/student.guard.ts`
- **Estimated Effort**: 20 min
- **Dependencies**: Task 8

**admin.guard.ts** — replace `currentUser.userRole === 'ADMIN'` with `RoleService.isPlatformAdmin()`:
```typescript
import { RoleService } from '../services/role.service';

export class AdminGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private roleService: RoleService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const currentUser = this.authService.currentUserValue;

    if (currentUser && this.roleService.isPlatformAdmin()) {
      return true;
    }

    if (currentUser) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    }

    return false;
  }
}
```

**student.guard.ts** — replace string comparisons with `RoleService` calls:
```typescript
import { RoleService } from '../services/role.service';

export class StudentGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private roleService: RoleService,
    private router: Router
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const currentUser = this.authService.currentUserValue;

    if (currentUser && this.roleService.isStudent()) {
      return true;
    }

    if (this.roleService.isPlatformAdmin()) {
      this.router.navigate(['/admin']);
    } else if (currentUser) {
      this.router.navigate(['/home']);
    } else {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    }

    return false;
  }
}
```

- **Acceptance Criteria**:
  - [ ] No raw string `'ADMIN'` remains in either guard file
  - [ ] `npm run build` succeeds

---

### Task 10 — Update Hardcoded Role Strings in Components and Services

- **Files** (8 files):
  - `frontend/src/app/features/auth/login/login.component.ts`
  - `frontend/src/app/features/auth/change-password/change-password.component.ts`
  - `frontend/src/app/features/dashboard/applications/application-details/application-details.component.ts`
  - `frontend/src/app/features/shared/components/document-comment-section/document-comment-section.component.ts`
  - `frontend/src/app/features/admin/activity-logs/activity-logs.component.ts`
  - `frontend/src/app/features/admin/user-management/user-management.component.ts`
  - `frontend/src/app/features/admin/user-management/create-user-dialog/create-user-dialog.component.ts`
  - `frontend/src/app/core/services/admin.service.ts` (comment only)
- **Estimated Effort**: 45 min
- **Dependencies**: Task 8

**Change summary per file:**

| File | Change |
|---|---|
| `login.component.ts:~88` | `user.userRole === 'ADMIN'` → `this.roleService.isPlatformAdmin()` (inject `RoleService`) |
| `change-password.component.ts:~96,~138` | `userRole === 'ADMIN'` → `this.roleService.isPlatformAdmin()` (inject `RoleService`) |
| `application-details.component.ts:~96` | `isAdmin = currentUser?.userRole === 'ADMIN'` → `isAdmin = this.roleService.isPlatformAdmin()` (inject `RoleService`) |
| `document-comment-section.component.ts:~73` | `isAdmin = currentUser?.userRole === 'ADMIN'` → `isAdmin = this.roleService.isPlatformAdmin()` (inject `RoleService`) |
| `activity-logs.component.ts:~89` | `['ADMIN', 'STUDENT', 'LANDLORD']` → `['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF', 'STUDENT']` (no RoleService needed — just string values for filter dropdown) |
| `user-management.component.ts:~133,~135` | `case 'ADMIN': return 'Admin'` → `case 'PLATFORM_ADMIN': return 'Platform Admin'`; `case 'LANDLORD': return 'Landlord'` → `case 'BUILDING_OWNER': return 'Building Owner'`; add `case 'BUILDING_STAFF': return 'Building Staff'` |
| `create-user-dialog.component.ts:~49` | default `userRole: ['LANDLORD', ...]` → `userRole: ['BUILDING_OWNER', ...]`; update role options array to `['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF']` |
| `admin.service.ts:~11` | Update comment only: `// Role must be PLATFORM_ADMIN, BUILDING_OWNER, or BUILDING_STAFF` |

- **Acceptance Criteria**:
  - [ ] `rg "'ADMIN'" frontend/src/app` (excluding `role.model.ts` deprecated entries and comments) returns zero results
  - [ ] `rg "'LANDLORD'" frontend/src/app` returns zero results
  - [ ] `npm run build` succeeds
  - [ ] `npm run lint` passes

---

### Task 11 — Frontend Tests

- **Files** (create new):
  - `frontend/src/app/core/services/role.service.spec.ts`
  - `frontend/src/app/core/guards/admin.guard.spec.ts`
  - `frontend/src/app/core/guards/student.guard.spec.ts`
- **Estimated Effort**: 60 min
- **Dependencies**: Tasks 8, 9

**role.service.spec.ts**:
```typescript
describe('RoleService', () => {
  it('isPlatformAdmin_whenUserIsPlatformAdmin_returnsTrue')
  it('isPlatformAdmin_whenUserIsStudent_returnsFalse')
  it('isPlatformAdmin_whenUserIsNull_returnsFalse')
  it('isStudent_whenUserIsStudent_returnsTrue')
  it('isStudent_whenUserIsPlatformAdmin_returnsFalse')
  it('isBuildingOwner_whenUserIsBuildingOwner_returnsTrue')
  it('hasRole_withUnknownRole_returnsFalse')
  it('hasRole_whenCurrentUserIsNull_returnsFalse')
})
```

**admin.guard.spec.ts**:
```typescript
describe('AdminGuard', () => {
  it('canActivate_platformAdminUser_returnsTrue')
  it('canActivate_studentUser_redirectsToDashboard_returnsFalse')
  it('canActivate_unauthenticatedUser_redirectsToLogin_returnsFalse')
})
```

**student.guard.spec.ts**:
```typescript
describe('StudentGuard', () => {
  it('canActivate_studentUser_returnsTrue')
  it('canActivate_platformAdminUser_redirectsToAdmin_returnsFalse')
  it('canActivate_buildingOwnerUser_redirectsToHome_returnsFalse')
  it('canActivate_unauthenticatedUser_redirectsToLogin_returnsFalse')
})
```

- **Acceptance Criteria**:
  - [ ] All spec tests pass: `npm test -- --watch=false`
  - [ ] No existing spec tests regress

---

## Final Validation

After all tasks are complete, run the full validation sequence:

```bash
# Backend
cd backend
./gradlew clean build

# Frontend
cd frontend
npm run build
npm run lint
npm test -- --watch=false
```

**Definition of Done**:
- [ ] `./gradlew clean build` — `BUILD SUCCESSFUL`, zero test failures
- [ ] `npm run build` — successful Angular production build
- [ ] `npm run lint` — zero linting errors
- [ ] `npm test -- --watch=false` — all specs pass
- [ ] `rg "hasRole\('ADMIN'\)" backend/src/main/java` — zero results
- [ ] `rg "'ADMIN'" frontend/src/app --include="*.ts" -l` — zero non-model, non-comment results
- [ ] Application starts with `docker-compose up --build` — no Flyway errors, no Spring bean errors
- [ ] Platform admin can log in → `/admin` route accessible
- [ ] Student can log in → `/dashboard` route accessible
- [ ] `BUILDING_OWNER` login → blocked from `/admin`, redirected to `/home`

---

## Task Dependency Graph

```
Task 1 (Migration)
Task 2 (Enum)          ──► Task 3 (JWT + SecurityConfig)
                       ──► Task 4 (Controllers)
                       ──► Task 5 (Services)
Tasks 3+4+5            ──► Task 6 (Backend Tests)
Task 6                 ──► Task 7 (Full Build Verify)

Task 8 (FE Model+RoleService) ──► Task 9  (Guards)
                               ──► Task 10 (Components)
Tasks 9+10             ──► Task 11 (FE Tests)

Task 7 + Task 11       ──► Final Validation
```

Tasks 1 and 2 have no dependencies and can run simultaneously.  
Tasks 8–11 (all frontend) can run in parallel with Tasks 3–7 (backend) after Task 2 is complete, since the backend and frontend only need to agree on role string values (`'PLATFORM_ADMIN'` etc.), which are defined in Task 2 and used as plain strings in Task 10.
