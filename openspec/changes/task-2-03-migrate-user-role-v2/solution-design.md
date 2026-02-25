# Technical Design Document
## TASK-2.03 — Migrate UserRole to v2.0 Role Model

---

## 1. Solution Overview

**High-Level Summary:**

This task is a targeted rename migration. The core change is: `ADMIN → PLATFORM_ADMIN` and `LANDLORD → BUILDING_OWNER`, with `BUILDING_STAFF` added as a new scaffolding role. No new endpoints are opened to new roles. No schema changes are required — `user_role` is already stored as `VARCHAR`. All work is: (1) extend the Java enum, (2) update one Flyway data migration, (3) update `@PreAuthorize` annotations across controllers, (4) update service-layer role comparisons, (5) fix JWT expiry check, (6) create a frontend `RoleService` and `Role` enum, and (7) update guards and components to use `RoleService`.

The Strangler Pattern applies: `ADMIN` and `LANDLORD` enum values are retained as `@Deprecated` for the duration of the deprecation window. No existing code path is removed; the old values remain resolvable via `enumRoleOf()` and `valueOf()`. New `@PreAuthorize` annotations exclusively use the new role names.

The highest-risk step is the mass replacement of `hasRole('ADMIN')` across 14 controllers and 12 service classes. This must be done exhaustively — a single missed occurrence either breaks access for legitimate admins or silently grants no access where access was expected. A `SecurityConfigTest` is mandatory to verify every protected endpoint's effective role.

**Key Technical Decisions:**

1. **Flyway migration timestamp `V2026_02_25_0001`** — Last applied migration is `V2026_02_24_0030`. New file uses `V2026_02_25_0001` (today's date), which sorts after all existing migrations. Flyway `out-of-order: false` is in effect.
2. **`isAdmin()` returns true for both `ADMIN` and `PLATFORM_ADMIN`** — This preserves backward compatibility for all service-layer callers during the deprecation window without requiring them to all be updated simultaneously.
3. **`RoleService` reads from `AuthService.currentUserValue`** — No duplication of user state. The service is a pure role-check facade delegating to an injected `AuthService`.
4. **Role hierarchy becomes `PLATFORM_ADMIN > BUILDING_OWNER > BUILDING_STAFF` (management chain only)** — `STUDENT` is intentionally disconnected from the hierarchy. Any endpoint that must be accessible to both students and platform admins uses `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`.

**Architecture Diagram (ASCII):**
```
┌──────────────────────────────────────────────┐
│              Angular Frontend                 │
│  ┌───────────────┐   ┌──────────────────────┐│
│  │  AdminGuard   │   │   StudentGuard        ││
│  │  (uses        │   │   (uses               ││
│  │  RoleService) │   │   RoleService)        ││
│  └───────┬───────┘   └──────────┬───────────┘│
│          │                      │             │
│  ┌───────▼──────────────────────▼───────────┐│
│  │             RoleService                   ││
│  │  isPlatformAdmin() / isStudent() /        ││
│  │  isBuildingOwner() / hasRole(Role)        ││
│  └───────────────┬───────────────────────────┘│
│                  │ reads currentUserValue      │
│  ┌───────────────▼───────────────────────────┐│
│  │             AuthService                   ││
│  └───────────────────────────────────────────┘│
└──────────────────────┬───────────────────────┘
                       │ HTTP/REST (JWT)
┌──────────────────────▼───────────────────────┐
│              Spring Boot Backend              │
│  ┌──────────────────────────────────────────┐│
│  │  JwtUtil.generateToken()                  ││
│  │  roles.contains("ROLE_PLATFORM_ADMIN")    ││
│  │  → 4h expiry                              ││
│  └──────────────────────────────────────────┘│
│  ┌──────────────────────────────────────────┐│
│  │  SecurityConfig.roleHierarchy()           ││
│  │  PLATFORM_ADMIN > BUILDING_OWNER >        ││
│  │  BUILDING_STAFF                           ││
│  └──────────────────────────────────────────┘│
│  ┌──────────────────────────────────────────┐│
│  │  @PreAuthorize("hasRole('PLATFORM_ADMIN')")││
│  │  on all admin controllers                 ││
│  └──────────────────────────────────────────┘│
└──────────────────────┬───────────────────────┘
                       │
                  PostgreSQL
           (user_role = 'PLATFORM_ADMIN')
```

---

## 2. Backend Architecture

### 2.1 Database Design

#### 2.1.1 Schema Changes

**No schema changes required.** The `app_user.user_role` column is already `VARCHAR` and stores the enum name as a string. Only data needs updating.

**Data Migration:**

```sql
-- V2026_02_25_0001__migrate_user_roles.sql
BEGIN;

UPDATE app_user
SET user_role = 'PLATFORM_ADMIN'
WHERE user_role = 'ADMIN';

UPDATE app_user
SET user_role = 'BUILDING_OWNER'
WHERE user_role = 'LANDLORD';

COMMIT;
```

**Post-Migration Verification (in test):**
```sql
SELECT COUNT(*) FROM app_user WHERE user_role IN ('ADMIN', 'LANDLORD');
-- Must return 0
```

**Rollback Strategy:** Not applicable — Flyway migrations are immutable. A forward-migration approach is used: if rollback is needed in a test environment, run a compensating migration that reverses the UPDATE statements.

#### 2.1.2 Entity Relationships

No changes to entity relationships. `AppUser.userRole` maps to `UserRole` enum via `@Enumerated(EnumType.STRING)`.

#### 2.1.3 Flyway Migration Plan

**Migration File:** `V2026_02_25_0001__migrate_user_roles.sql`

**Migration Order:**
1. UPDATE `app_user` SET `user_role = 'PLATFORM_ADMIN'` WHERE `user_role = 'ADMIN'`
2. UPDATE `app_user` SET `user_role = 'BUILDING_OWNER'` WHERE `user_role = 'LANDLORD'`

Both statements run in a single transaction. Flyway applies this before any Spring bean initializes.

### 2.2 Enum Layer

**File:** `model/enums/UserRole.java`

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
     * Returns true for PLATFORM_ADMIN (and deprecated ADMIN during migration window).
     */
    public boolean isAdmin() {
        return this == PLATFORM_ADMIN || this == ADMIN;
    }
}
```

**Key decisions:**
- `enumRoleOf("Platform_Admin")`, `enumRoleOf("PLATFORM_ADMIN")`, and `enumRoleOf("platform_admin")` all resolve to `PLATFORM_ADMIN` because the comparison is `equalsIgnoreCase` against the `role` field `"Platform_Admin"`. This works because Java's `equalsIgnoreCase` is case-insensitive, so `"platform_admin".equalsIgnoreCase("Platform_Admin")` returns `true`.
- `@Deprecated` on enum constants is a Java 9+ feature — supported on Java 17.
- `ADMIN` and `LANDLORD` remain in the enum so that `UserRole.valueOf("ADMIN")` does not throw. Any DB record not yet migrated (e.g., in a test H2 database) will still deserialize correctly.

### 2.3 Security Configuration

**File:** `config/SecurityConfig.java`

**Changes:**

```java
// Line 77 — URL-level security
.requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
// Line 79
.requestMatchers("/actuator/**").hasRole("PLATFORM_ADMIN")

// Role hierarchy (lines 96–100)
@Bean
public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        "ROLE_PLATFORM_ADMIN > ROLE_BUILDING_OWNER \n" +
        "ROLE_BUILDING_OWNER > ROLE_BUILDING_STAFF"
    );
}
```

**Critical:** `STUDENT` is NOT in this hierarchy. `PLATFORM_ADMIN` does NOT inherit `STUDENT` permissions. Any endpoint that admins and students both need uses `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`.

### 2.4 JWT Token Expiry Fix

**File:** `security/jwt/JwtUtil.java`

**Change (line 69):**
```java
// Before:
if (roles.contains("ROLE_ADMIN")) {
// After:
if (roles.contains("ROLE_PLATFORM_ADMIN")) {
```

`BUILDING_OWNER` and `BUILDING_STAFF` use the default 10-hour expiry. This is intentional per business requirements.

### 2.5 Controller Layer — @PreAuthorize Updates

All `hasRole('ADMIN')` → `hasRole('PLATFORM_ADMIN')`. All `hasAnyRole('STUDENT', 'ADMIN')` → `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`. No endpoint re-scoping.

**Affected controllers (exhaustive):**

| Controller | Change |
|---|---|
| `AcademicYearController` | `hasRole('ADMIN')` × 6 → `hasRole('PLATFORM_ADMIN')` |
| `AccommodationTypeController` | `hasRole('ADMIN')` × 5 → `hasRole('PLATFORM_ADMIN')` |
| `ActivityLogController` | `hasRole('ADMIN')` × 2 → `hasRole('PLATFORM_ADMIN')` |
| `AdminStatsController` | `hasRole('ADMIN')` × 2 → `hasRole('PLATFORM_ADMIN')` |
| `AllocationRuleController` | class-level `hasRole('ADMIN')` → `hasRole('PLATFORM_ADMIN')` |
| `ApplicationController` | `hasRole('ADMIN')` + `hasAnyRole('STUDENT','ADMIN')` → updated |
| `AuthController` | `hasRole('ADMIN')` × 1 → `hasRole('PLATFORM_ADMIN')` |
| `BuildingAttributesController` | class-level `hasRole('ADMIN')` → `hasRole('PLATFORM_ADMIN')` |
| `BuildingController` | `hasRole('ADMIN')` × 6 → `hasRole('PLATFORM_ADMIN')` |
| `DocumentController` | `hasRole('ADMIN')` × 5 → `hasRole('PLATFORM_ADMIN')` |
| `DocumentTypeController` | `hasRole('ADMIN')` × 4 → `hasRole('PLATFORM_ADMIN')` |
| `FeatureController` | `hasRole('ADMIN')` × 5 → `hasRole('PLATFORM_ADMIN')` |
| `InstitutionController` | `hasRole('ADMIN')` × 3 → `hasRole('PLATFORM_ADMIN')` |
| `RoomAllocationController` | `hasRole('ADMIN')` × 12 + `hasAnyRole` → updated |

**Implementation note:** Use a targeted find-and-replace within each file. Verify with `rg 'hasRole\(.*ADMIN' backend/src/main/java` after the change to confirm zero remaining `'ADMIN'` role references in annotations (only `'PLATFORM_ADMIN'` should appear).

### 2.6 Service Layer Updates

**Pattern:** All `getUserRole().isAdmin()` calls continue to work because `isAdmin()` now returns `true` for both `PLATFORM_ADMIN` and `ADMIN`. Services using this pattern need NO change.

Services using direct `== UserRole.ADMIN` or `.equals(UserRole.ADMIN)` must be updated to include `PLATFORM_ADMIN`:

| Service | File | Change |
|---|---|---|
| `StudentService` | service/ | `== UserRole.ADMIN` × 2 → `getUserRole().isAdmin()` |
| `ApplicationAdminService` | service/application/ | `.equals(UserRole.ADMIN)` × 2 → `.isAdmin()` |
| `ApplicationCoreService` | service/application/ | `.equals(UserRole.ADMIN)` × 3 → `.isAdmin()` |
| `ApplicationDocumentService` | service/application/ | `.equals(UserRole.ADMIN)` × 1 → `.isAdmin()` |
| `ApplicationQueryService` | service/application/ | `.equals(UserRole.ADMIN)` × 1 → `.isAdmin()` |
| `AdminStatsService` | service/ | `countByUserRole(UserRole.ADMIN)` → `countByUserRole(UserRole.PLATFORM_ADMIN)` |
| `UserManagementService` | service/ | validation: reject deprecated roles + `STUDENT`; accept `PLATFORM_ADMIN`, `BUILDING_OWNER`, `BUILDING_STAFF` |

**`UserManagementService` validation logic:**
```java
// Reject STUDENT (self-registers only)
if (role == UserRole.STUDENT) {
    throw new ValidationException("Cannot create STUDENT user via admin endpoint.");
}
// Reject deprecated roles
if (role == UserRole.ADMIN || role == UserRole.LANDLORD) {
    throw new ValidationException("Role " + role.name() + " is deprecated. Use PLATFORM_ADMIN or BUILDING_OWNER.");
}
```

**`ActivityLogService`:** Uses `UserRole.valueOf(userRole)` and `enumRoleOf()` for filter parsing. Both continue to work because `ADMIN` and `LANDLORD` remain in the enum. No change required.

**`DocumentCommentService`, `DocumentLifecycleService`, `DocumentQueryService`, `DocumentStatusService`:** All use `isAdmin()`. Because `isAdmin()` is updated to return `true` for `PLATFORM_ADMIN`, no code changes needed in these services.

### 2.7 Testing

**`SecurityConfigTest.java`** — New integration test verifying:
- All `/api/admin/**` endpoints require `PLATFORM_ADMIN`
- `BUILDING_OWNER` cannot access `/api/admin/**`
- `STUDENT` cannot access `/api/admin/**`
- `PLATFORM_ADMIN` can access `/api/admin/**`

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void adminEndpoint_withPlatformAdminRole_returns2xx() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(roles = "BUILDING_OWNER")
    void adminEndpoint_withBuildingOwnerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void adminEndpoint_withStudentRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
               .andExpect(status().isForbidden());
    }
}
```

**`UserRoleTest.java`** — Unit tests:
- `enumRoleOf("Platform_Admin")` returns `PLATFORM_ADMIN`
- `enumRoleOf("PLATFORM_ADMIN")` returns `PLATFORM_ADMIN`
- `enumRoleOf("Admin")` returns `ADMIN` (backward compat)
- `PLATFORM_ADMIN.isAdmin()` returns `true`
- `ADMIN.isAdmin()` returns `true`
- `STUDENT.isAdmin()` returns `false`
- `BUILDING_OWNER.isAdmin()` returns `false`

---

## 3. Frontend Architecture

### 3.1 New Files

#### `frontend/src/app/core/models/role.model.ts`

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

#### `frontend/src/app/core/services/role.service.ts`

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
    const currentUser = this.authService.currentUserValue;
    return currentUser?.userRole === role;
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

**Key design decisions:**
- `currentUser` null → all methods return `false` (EC-5 in business analysis)
- `currentUser.userRole` with an unknown value → `hasRole()` returns `false` (no throw)
- `RoleService` does NOT duplicate state — it delegates to `AuthService.currentUserValue`

### 3.2 Updated Guards

#### `admin.guard.ts`

```typescript
import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

@Injectable({
  providedIn: 'root',
})
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

#### `student.guard.ts`

```typescript
import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';

@Injectable({
  providedIn: 'root',
})
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

### 3.3 Updated Components and Services

All occurrences of hardcoded `'ADMIN'` / `'LANDLORD'` strings in frontend files must be replaced with `Role` enum values or `RoleService` method calls.

| File | Line(s) | Current | Updated |
|---|---|---|---|
| `login.component.ts` | ~88 | `user.userRole === 'ADMIN'` | `this.roleService.isPlatformAdmin()` |
| `change-password.component.ts` | ~96, ~138 | `userRole === 'ADMIN'` | `this.roleService.isPlatformAdmin()` |
| `application-details.component.ts` | ~96 | `currentUser?.userRole === 'ADMIN'` | `this.roleService.isPlatformAdmin()` |
| `document-comment-section.component.ts` | ~73 | `currentUser?.userRole === 'ADMIN'` | `this.roleService.isPlatformAdmin()` |
| `activity-logs.component.ts` | ~89 | `['ADMIN', 'STUDENT', 'LANDLORD']` | `['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF', 'STUDENT']` |
| `user-management.component.ts` | ~133, ~135 | `case 'ADMIN':` / `case 'LANDLORD':` | `case 'PLATFORM_ADMIN':` / `case 'BUILDING_OWNER':` |
| `create-user-dialog.component.ts` | ~49 | default `userRole: ['LANDLORD', ...]` | default `userRole: ['BUILDING_OWNER', ...]` |
| `admin.service.ts` | ~11 | comment / type docs | update comment only |

**Display labels for `user-management.component.ts` switch:**

```typescript
case 'PLATFORM_ADMIN': return 'Platform Admin';
case 'BUILDING_OWNER': return 'Building Owner';
case 'BUILDING_STAFF': return 'Building Staff';
case 'STUDENT': return 'Student';
```

---

## 4. Data Flow

**Platform Admin Login (post-migration):**
```
1. User POSTs /api/auth/login with credentials
2. AuthService authenticates → DB record has user_role = 'PLATFORM_ADMIN'
3. JwtUtil.generateToken(): roles = ["ROLE_PLATFORM_ADMIN"]
   → roles.contains("ROLE_PLATFORM_ADMIN") = true → expiry = 4 hours
4. JWT issued with ROLE_PLATFORM_ADMIN claim
5. Angular receives JWT → decodes → userRole = 'PLATFORM_ADMIN'
6. login.component calls RoleService.isPlatformAdmin() → true → navigate /admin
7. AdminGuard: RoleService.isPlatformAdmin() → true → route allowed
8. API requests: Authorization: Bearer <token>
9. Spring Security: ROLE_PLATFORM_ADMIN passes hasRole('PLATFORM_ADMIN') check
```

---

## 5. API Design

No new endpoints. No changed endpoint contracts. Role check annotations change internally.

---

## 6. Security Design

### 6.1 Authorization Matrix (Post-Migration)

| Endpoint Pattern | PLATFORM_ADMIN | BUILDING_OWNER | BUILDING_STAFF | STUDENT |
|---|---|---|---|---|
| `/api/admin/**` | ✓ | ✗ | ✗ | ✗ |
| `/actuator/**` | ✓ | ✗ | ✗ | ✗ |
| Student-facing endpoints | ✓ (explicit `hasAnyRole`) | ✗ | ✗ | ✓ |
| Authenticated-only endpoints | ✓ | ✓ | ✓ | ✓ |

### 6.2 JWT Expiry by Role

| Role | Expiry |
|---|---|
| `PLATFORM_ADMIN` | 4 hours (`JWT_ADMIN_EXPIRATION`) |
| `BUILDING_OWNER` | 10 hours (`JWT_EXPIRATION`) |
| `BUILDING_STAFF` | 10 hours (`JWT_EXPIRATION`) |
| `STUDENT` | 10 hours (`JWT_EXPIRATION`) |

### 6.3 Role Hierarchy

```
ROLE_PLATFORM_ADMIN > ROLE_BUILDING_OWNER > ROLE_BUILDING_STAFF
```

`ROLE_STUDENT` is NOT in the hierarchy. A `PLATFORM_ADMIN` does NOT inherit `STUDENT` permissions. Dual-audience endpoints use `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')` explicitly.

### 6.4 Backward Compatibility

- In-flight JWTs with `ROLE_ADMIN` continue to be accepted by Spring Security until natural expiry (max 4 hours). No forced invalidation.
- `UserRole.valueOf("ADMIN")` and `enumRoleOf("Admin")` continue to work (enum values retained as deprecated).
- `ActivityLogService` filter by role name `'ADMIN'` returns zero results post-migration (correct behaviour — no 500).

---

## 7. Testing Strategy

### 7.1 Backend Tests

**`UserRoleTest.java`** (unit):
- All `enumRoleOf()` cases (new roles, deprecated roles, case-insensitive)
- `isAdmin()` returns true for `PLATFORM_ADMIN` and `ADMIN`, false for others

**`SecurityConfigTest.java`** (integration, `@SpringBootTest` + `MockMvc`):
- `PLATFORM_ADMIN` can access `/api/admin/stats` (200)
- `BUILDING_OWNER` blocked from `/api/admin/stats` (403)
- `STUDENT` blocked from `/api/admin/stats` (403)
- Unauthenticated blocked from `/api/admin/stats` (401 or 403)

**`UserManagementServiceTest.java`** (unit, mock repo):
- Creating user with `BUILDING_OWNER` succeeds
- Creating user with `BUILDING_STAFF` succeeds
- Creating user with `STUDENT` throws validation exception
- Creating user with deprecated `LANDLORD` throws validation exception

### 7.2 Frontend Tests

**`role.service.spec.ts`** (new):
- `isPlatformAdmin()` returns true for `PLATFORM_ADMIN`, false for others
- `isStudent()` returns true for `STUDENT`, false for others
- `isBuildingOwner()` returns true for `BUILDING_OWNER`, false for others
- All methods return false when `currentUserValue` is null

**`admin.guard.spec.ts`** (update):
- `PLATFORM_ADMIN` → canActivate returns true
- `STUDENT` → redirected to /dashboard
- Unauthenticated → redirected to /login

**`student.guard.spec.ts`** (update):
- `STUDENT` → canActivate returns true
- `PLATFORM_ADMIN` → redirected to /admin
- `BUILDING_OWNER` → redirected to /home
- Unauthenticated → redirected to /login

---

## 8. Implementation Sequence

The following order minimises risk of a broken intermediate state:

1. **Flyway migration** — `V2026_02_25_0001__migrate_user_roles.sql` (data only, no schema change)
2. **`UserRole` enum** — add new values, update `isAdmin()`, keep deprecated values
3. **`JwtUtil`** — update `ROLE_ADMIN` → `ROLE_PLATFORM_ADMIN` expiry check
4. **`SecurityConfig`** — update role hierarchy + URL-level matchers
5. **All controller `@PreAuthorize` annotations** — mass replace `ADMIN` → `PLATFORM_ADMIN`
6. **Service layer** — update direct enum comparisons; update `UserManagementService` validation; update `AdminStatsService`
7. **Backend tests** — `UserRoleTest`, `SecurityConfigTest`, `UserManagementServiceTest`
8. **Frontend `Role` model** — `role.model.ts`
9. **Frontend `RoleService`** — `role.service.ts`
10. **Frontend guards** — update `AdminGuard`, `StudentGuard`
11. **Frontend components/services** — update all hardcoded role strings
12. **Frontend tests** — `role.service.spec.ts`, updated guard specs

---

## 9. Technical Decisions & Rationale

**Decision 1: Retain `ADMIN` and `LANDLORD` as `@Deprecated` enum values**
- **Options Considered**: Remove immediately; keep as deprecated
- **Selected**: Keep as deprecated
- **Rationale**: Removing them immediately would break `UserRole.valueOf("ADMIN")` for any unprocessed records and invalidate in-flight JWTs. The deprecation window costs nothing and eliminates a class of hard-to-debug runtime failures.
- **Trade-offs**: Minor enum clutter; risk of developers accidentally using deprecated values (mitigated by `@Deprecated` annotation and IDE warnings)

**Decision 2: `isAdmin()` covers both `PLATFORM_ADMIN` and deprecated `ADMIN`**
- **Options Considered**: Update all `isAdmin()` call sites to check both; update `isAdmin()` itself
- **Selected**: Update `isAdmin()` itself
- **Rationale**: Centralised; call sites don't need to change; consistent with the Strangler pattern (extend, don't rewrite)
- **Trade-offs**: `isAdmin()` semantics are slightly overloaded during the deprecation window; acceptable given it will be cleaned up when deprecated values are removed

**Decision 3: `PLATFORM_ADMIN` does NOT inherit `STUDENT` in the role hierarchy**
- **Options Considered**: Keep `PLATFORM_ADMIN > STUDENT` (similar to old `ADMIN > LANDLORD > STUDENT`); separate the hierarchies
- **Selected**: Separate hierarchies — management chain is independent of student
- **Rationale**: A building owner must not be able to submit accommodation applications. Inherited permissions are implicit and easy to miss. Explicit `hasAnyRole` on dual-audience endpoints is safer and more readable.
- **Trade-offs**: Requires auditing all `hasAnyRole('STUDENT', 'ADMIN')` annotations and updating them to `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')` (already captured in FR-4)

---

## 10. Risk Assessment

| Risk | Impact | Probability | Mitigation |
|---|---|---|---|
| Missed `@PreAuthorize` annotation | High | Medium | `rg 'hasRole.*ADMIN[^_]'` search after replacement; `SecurityConfigTest` |
| Flyway out-of-order migration | High | Low | Verified: `V2026_02_25_0001` > `V2026_02_24_0030` |
| `AdminStatsService` returns 0 admins | Medium | High | Explicitly in implementation plan; covered by service test |
| `RoleService` null pointer on unauthenticated load | Medium | Medium | `currentUser?.userRole` optional chaining; unit tested |
| In-flight `ROLE_ADMIN` JWT after deploy | Low | High | Acceptable; expires within 4 hours; no session invalidation needed |
| Frontend component missing `RoleService` update | Medium | Medium | Full `rg "'ADMIN'"` / `rg "'LANDLORD'"` search in frontend post-implementation |

---

## Architecture Review Checklist

- [x] Database migration: data-only UPDATE, atomic transaction, correct timestamp
- [x] Enum: new values, deprecated values retained, `isAdmin()` updated, `enumRoleOf()` handles all cases
- [x] JWT expiry: `ROLE_PLATFORM_ADMIN` check in `JwtUtil`
- [x] Security config: new role hierarchy, URL-level matchers updated
- [x] All controller annotations mapped (14 controllers)
- [x] All service-layer direct enum comparisons addressed (12 services)
- [x] `UserManagementService` validation logic specified
- [x] Frontend `Role` enum defined with deprecated values
- [x] `RoleService` designed: reads from `AuthService`, null-safe, all check methods
- [x] Guards updated: `AdminGuard` uses `isPlatformAdmin()`, `StudentGuard` uses `isStudent()`
- [x] All 8 frontend files with hardcoded role strings identified and mapped
- [x] Backend tests: `UserRoleTest`, `SecurityConfigTest`, `UserManagementServiceTest`
- [x] Frontend tests: `role.service.spec.ts`, guard specs
- [x] Implementation sequence ordered to avoid broken intermediate states
- [x] Backward compatibility: in-flight JWTs, deprecated `enumRoleOf()`, activity log filters
- [x] No new endpoints opened to `BUILDING_OWNER` or `BUILDING_STAFF` (out of scope)
- [x] No violations of hard limits (service <400 lines, method <30 lines)
