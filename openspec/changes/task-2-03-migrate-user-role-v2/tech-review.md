# Tech Lead - Technical Review & Approval
## TASK-2.03 — Migrate UserRole to v2.0 Role Model

**Feature**: Migrate UserRole to v2.0 Role Model (ADMIN → PLATFORM_ADMIN, LANDLORD → BUILDING_OWNER, add BUILDING_STAFF)
**Reviewer**: Tech Lead (AI)
**Review Date**: 2026-02-25
**Review Status**: ⚠️ APPROVED WITH CONDITIONS

**Quick Assessment**:
- Architecture Alignment: ✅
- Code Quality Standards: ✅
- Security Review: ⚠️
- Performance Considerations: ✅
- Test Coverage Plan: ⚠️
- Implementation Readiness: ✅

---

## 1. Architecture Review

### 1.1 Backend Architecture Compliance

**Spring Boot Patterns:**
- [x] Package structure follows `com.beezhub.student_accommodation` convention
- [x] Controller → Service → Repository → Entity layering respected
- [x] DTOs separate from entities (no entity exposure in API)
- [x] MapStruct used for entity-DTO mapping (`AppUserMapper.mapAdminRole()` noted)
- [x] Services annotated with `@Service` and `@Transactional` where needed
- [x] Repositories extend JpaRepository and JpaSpecificationExecutor

**Findings:**
- ✅ **Compliant**: The enum extension, Flyway migration, service-layer refactoring, and JWT fix all operate within the existing layer boundaries. No new classes are added to the wrong package. The `UserManagementService` validation change is appropriately placed in the service layer.
- ✅ **Strangler Pattern honoured**: `ADMIN` and `LANDLORD` are retained as `@Deprecated`. No existing code path is removed. `isAdmin()` is extended, not rewritten. Full backward compatibility during the deprecation window.
- ⚠️ **`enumRoleOf()` input contract ambiguity**: The method matches against `getRole()` (display name, e.g., `"Platform_Admin"`) not the enum `name()` (`"PLATFORM_ADMIN"`). This is case-insensitive, so `enumRoleOf("PLATFORM_ADMIN")` does resolve correctly — but the behaviour depends on `"PLATFORM_ADMIN".equalsIgnoreCase("Platform_Admin")` returning true, which it does. This is subtle and should be documented explicitly in the method Javadoc to prevent future confusion.
- ⚠️ **`AppUserMapper.mapAdminRole()` is not explicitly updated in the implementation plan.** The BA document mentions it at section 7.1 (`AppUserMapper.mapAdminRole() validates role is not STUDENT — must accept new admin roles`). If this mapper currently contains `if (role == UserRole.ADMIN || role == UserRole.LANDLORD)` logic, it needs the same treatment as `UserManagementService`. This must be verified during Task 5.

**Recommendations:**
1. Add explicit Javadoc to `enumRoleOf()` documenting that it matches against the display name field (`getRole()`), not the enum constant name, and that it is case-insensitive.
2. Add `AppUserMapper` to the Task 5 checklist. Run `rg "UserRole.ADMIN\|UserRole.LANDLORD" backend/src/main/java --include="*.java" -l` at the start of Task 5 to capture every file needing update.

### 1.2 Frontend Architecture Compliance

**Angular Structure:**
- [x] Components in appropriate module (admin/student/shared)
- [x] Services use dependency injection (`RoleService` injected via constructor)
- [x] Reactive forms used for data entry (no change to forms in this task)
- [x] Material Design components used consistently (no new UI components introduced)
- [x] Guards applied for route protection (`AdminGuard`, `StudentGuard` updated)
- [x] Interceptors handle auth tokens and errors (no change needed — token content changes are transparent to the interceptor)

**Findings:**
- ✅ **Compliant**: `RoleService` is correctly designed as a root-provided singleton delegating to `AuthService`. No state duplication. All role-check methods are null-safe via optional chaining.
- ✅ **Correct placement**: `role.model.ts` in `core/models/` and `role.service.ts` in `core/services/` are the right locations per module structure.
- ⚠️ **`AdminGuard` redirect target is `/dashboard`, not `/admin`**: The implementation plan's guard code redirects authenticated non-admin users to `/dashboard`. This matches the current guard behaviour but the student dashboard at `/dashboard` may not be appropriate for `BUILDING_OWNER` or `BUILDING_STAFF` users who have no student identity. Consider redirecting to `/home` for these roles. However, since no `BuildingOwnerGuard` exists yet and no building owner routes are defined, `/dashboard` is acceptable as a safe fallback for this task. Flagged for awareness.

**Recommendations:**
1. Add a comment in `AdminGuard.canActivate()` noting that the `/dashboard` redirect is a temporary fallback until `BuildingOwnerGuard` is implemented.

### 1.3 Design Patterns & SOLID Principles

**Single Responsibility Principle:**
- `RoleService` has a single, clear responsibility: role-check facade over `AuthService`. No business logic, no HTTP calls.
- `UserManagementService` validation change is scoped to its existing responsibility.
- The `UserRole` enum update adds a helper method (`isAdmin()`) and new constants — appropriate for an enum.

**Open/Closed Principle:**
- The `isAdmin()` extension pattern (covering both `PLATFORM_ADMIN` and deprecated `ADMIN`) is open for extension without modifying call sites. Correct.
- The role hierarchy in `SecurityConfig` is closed for modification without explicit code changes — acceptable for this architecture.

**Dependency Inversion:**
- `RoleService` depends on `AuthService` (abstraction-friendly, both are injected). Correct.
- No new concrete dependencies introduced.

**Findings:**
SOLID compliance is strong. The key design decision — updating `isAdmin()` centrally rather than updating all call sites — is the right application of the Open/Closed and DRY principles.

---

## 2. Database Design Review

### 2.1 Schema Design

**Table Structure:**
- [x] No schema change required — `user_role` is `VARCHAR`; no column additions, constraints, or index changes needed
- [x] Data UPDATE is atomic within Flyway transaction
- [x] No foreign key constraints reference `user_role` values (verified in BA section 6.2)
- [x] NOT NULL/DEFAULT constraints unaffected

**Findings:**
- ✅ **Good Design**: Data-only migration is the minimum necessary change. No schema evolution risks.
- ✅ **Atomic**: Both UPDATE statements in a single Flyway migration file, which Flyway wraps in one transaction by default (PostgreSQL DDL/DML transactional).
- ⚠️ **`activity_log` historical records**: The BA correctly identifies that existing `activity_log` records capturing `userRole = 'ADMIN'` are NOT retroactively updated. This is correct for audit trail integrity. However, the implementation plan does not explicitly test this. If `ActivityLogService` does a `UserRole.valueOf(String)` (using the enum constant name) rather than `enumRoleOf()` (using the display name), historical log records containing `'ADMIN'` will still deserialize correctly since `ADMIN` remains in the enum. **Verify which method is used** — if it is `UserRole.valueOf("ADMIN")`, no problem; if it is `enumRoleOf("ADMIN")`, verify that `"ADMIN".equalsIgnoreCase("Admin")` resolves correctly (it does). Either path is safe.

**Recommendations:**
1. None critical. The migration is minimal and correct.

### 2.2 Indexes & Performance

No new indexes are needed. The migration executes two UPDATE statements scanning `app_user` by `user_role` value. There is no index on `user_role` (role filtering is not a high-frequency query path), but for a university-scale deployment (< 10,000 users), a full sequential scan is imperceptible at < 1ms.

**Query Performance Considerations:**
- N+1 query risks: None introduced
- Join performance: Not applicable
- Full table scans: The two UPDATE statements scan `app_user` — acceptable at university scale

### 2.3 Flyway Migration Quality

**Migration Script Review:**
- [x] Migration follows naming convention `V{YYYY_MM_DD_HHMM}__description.sql` — `V2026_02_25_0001__migrate_user_roles.sql`
- [x] Timestamp `V2026_02_25_0001` sorts strictly after last applied `V2026_02_24_0030` — confirmed
- [x] Idempotent: Running again is safe (UPDATE WHERE user_role = 'PLATFORM_ADMIN' affects zero rows)
- [x] No schema change — pure DML; rollback via compensating migration if needed
- ⚠️ **`BEGIN`/`COMMIT` in migration script**: The solution design includes explicit `BEGIN`/`COMMIT` in the SQL. Flyway on PostgreSQL wraps migrations in a transaction by default (`flyway.execute-in-transaction=true`). Explicit `BEGIN`/`COMMIT` inside a Flyway migration is redundant and can cause issues with some JDBC drivers that auto-commit. **Remove `BEGIN`/`COMMIT` from the migration file** — let Flyway manage the transaction. The implementation plan's SQL (Task 1) correctly omits them; the solution design includes them. The implementation plan version should be used.

**Recommendations:**
1. **Use the Task 1 SQL (no explicit BEGIN/COMMIT)** — not the solution design SQL. Both UPDATE statements in a single file, no transaction wrappers.

---

## 3. API Design Review

### 3.1 RESTful Design Compliance

No new endpoints are introduced. This task is purely internal refactoring of role names — external API contracts are unchanged. The `@PreAuthorize` annotations are implementation details, not API contracts.

**Findings:**
- ✅ **No API contract changes**: Request/response DTOs unchanged; HTTP status codes unchanged; URL structure unchanged.
- ✅ **Backward compatible**: Clients using the existing API continue to work identically.

### 3.2 OpenAPI Documentation Quality

No OpenAPI changes required. The `@Tag`, `@Operation`, and `@ApiResponses` annotations are unaffected.

### 3.3 Versioning & Backward Compatibility

**API Evolution:**
- In-flight JWTs containing `ROLE_ADMIN` continue to be valid until natural expiry (max 4 hours). No forced invalidation. Acceptable.
- `enumRoleOf("Admin")` and `enumRoleOf("Landlord")` continue to resolve — any legacy API client sending old role strings gets correct behaviour during the deprecation window.
- `UserManagementService` will reject `LANDLORD` role in new create-user calls — this is a purposeful breaking change for that specific input, correctly specified in FR-5 and EC-2.

---

## 4. Security Review

### 4.1 Authentication & Authorization

**Security Configuration:**
- [x] All endpoints protected with JWT authentication (no change)
- [x] `@PreAuthorize` correctly applied — mass replacement `hasRole('ADMIN')` → `hasRole('PLATFORM_ADMIN')` is 1:1, no re-scoping
- [x] Student users can only access their own data (unchanged)
- [x] Admin users have appropriate access (PLATFORM_ADMIN has all previous ADMIN access)

**Critical Security Observation — Role Hierarchy Disconnection:**
The new hierarchy `ROLE_PLATFORM_ADMIN > ROLE_BUILDING_OWNER > ROLE_BUILDING_STAFF` correctly removes `STUDENT` from the management chain. However, the **existing hierarchy was `ROLE_ADMIN > ROLE_LANDLORD > ROLE_STUDENT`**, meaning any `hasRole('STUDENT')` endpoint was previously accessible by `ADMIN` users via inheritance. After this change, `PLATFORM_ADMIN` does **NOT** inherit `STUDENT` permissions.

The implementation plan correctly identifies `hasAnyRole('STUDENT', 'ADMIN')` annotations must become `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')` — but this must be complete and exhaustive. **A missed `hasAnyRole('STUDENT', 'ADMIN')` annotation would silently deny platform admins access to an endpoint they previously had.** The ripgrep verification command in Task 4 acceptance criteria covers this: `rg "hasAnyRole.*'ADMIN'" backend/src/main/java`.

Additionally: **any endpoint annotated with `hasRole('STUDENT')` only** — endpoints that admins previously accessed via hierarchy inheritance — will now block `PLATFORM_ADMIN`. These must be explicitly audited.

**Authorization Matrix Verification:**

| Endpoint Pattern | PLATFORM_ADMIN | BUILDING_OWNER | BUILDING_STAFF | STUDENT |
|---|---|---|---|---|
| `/api/admin/**` | ✅ (URL matcher) | ❌ | ❌ | ❌ |
| `hasRole('PLATFORM_ADMIN')` endpoints | ✅ | ❌ | ❌ | ❌ |
| `hasAnyRole('STUDENT','PLATFORM_ADMIN')` | ✅ | ❌ | ❌ | ✅ |
| `hasRole('STUDENT')` only | ❌ (hierarchy removed) | ❌ | ❌ | ✅ |
| `isAuthenticated()` | ✅ | ✅ | ✅ | ✅ |

**The fourth row is the security risk.** Any `hasRole('STUDENT')` endpoint that previously allowed `ADMIN` via hierarchy inheritance will now silently block `PLATFORM_ADMIN`. Run this search before and after Task 4:

```bash
rg "hasRole\('STUDENT'\)" backend/src/main/java --include="*.java"
```

For each result: confirm whether `PLATFORM_ADMIN` access was intentional via old hierarchy. If yes, update to `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`.

**Findings:**
- ⚠️ **`hasRole('STUDENT')` audit required** — see above. This is the single highest-risk step in this entire task.

**Critical Issues:**
1. Audit all `hasRole('STUDENT')` annotations to confirm none relied on ADMIN hierarchy inheritance. This must be completed as part of Task 4.

**Recommendations:**
1. Add to Task 4 acceptance criteria: `rg "hasRole\('STUDENT'\)" backend/src/main/java` — review each result and confirm PLATFORM_ADMIN access intent.

### 4.2 Input Validation & Sanitization

- [x] `UserManagementService` validation explicitly rejects deprecated roles and STUDENT
- [x] `enumRoleOf()` throws `IllegalArgumentException` on unknown role strings — Spring will surface as HTTP 400 via global exception handler (verify `@ControllerAdvice` handles this)
- [x] No new input surfaces introduced
- [x] JPA parameterized queries used (Flyway UPDATE uses literal values, no parameterization needed)

**Findings:**
- ✅ Input validation is appropriately updated for the new role model.
- ⚠️ Confirm the global `@ControllerAdvice` exception handler maps `IllegalArgumentException` to HTTP 400 and does not expose a stack trace. This pre-exists but should be verified during implementation.

### 4.3 Data Protection

- No new PII fields introduced
- `user_role` is not sensitive
- JWT claims include role name — `PLATFORM_ADMIN` is no more sensitive than `ADMIN` was
- No sensitive data added to logs

---

## 5. Code Quality Review

### 5.1 Exception Handling

- `enumRoleOf()` throws `IllegalArgumentException` with a clear message. Appropriate.
- `UserManagementService.validateAdminCreatableRole()` throws `ValidationException`. Appropriate — consistent with existing patterns.
- No new exception types introduced. Consistent.

**Recommendations:**
1. Verify `ValidationException` is a custom exception (not `javax.validation.ValidationException`) and that it is mapped to HTTP 400 in `@ControllerAdvice`. This is likely already correct but must be confirmed.

### 5.2 Code Duplication & Reusability

- ✅ `isAdmin()` centralisation eliminates the need to update 9 call sites individually — this is the correct approach.
- ✅ `RoleService` eliminates duplicated `currentUser.userRole === 'ADMIN'` checks across 8 frontend files — the right abstraction.
- No new duplication introduced.

### 5.3 Naming Conventions

**Java:**
- `PLATFORM_ADMIN`, `BUILDING_OWNER`, `BUILDING_STAFF` follow UPPER_SNAKE_CASE — correct
- `validateAdminCreatableRole()` — camelCase, descriptive — correct
- `enumRoleOf()` — slightly unconventional (not `fromDisplayName()` or `fromString()`), but consistent with existing codebase convention — acceptable

**TypeScript:**
- `Role` enum with UPPER_SNAKE_CASE values — correct
- `RoleService`, `role.service.ts`, `role.model.ts` — all follow Angular conventions — correct
- `isPlatformAdmin()`, `isStudent()`, `isBuildingOwner()` — camelCase boolean methods — correct

**Findings:**
No naming convention violations.

### 5.4 Code Comments & Documentation

- `isAdmin()` Javadoc is specified in the implementation plan — covers both `PLATFORM_ADMIN` and the deprecation window context. Good.
- `@Deprecated` annotation on `ADMIN` and `LANDLORD` constants is sufficient for IDE warnings.
- `@deprecated` JSDoc on `Role.ADMIN` and `Role.LANDLORD` in TypeScript — correct.

**Recommendations:**
1. Add the Javadoc comment to `enumRoleOf()` documenting the display-name matching behaviour (noted in §1.1).

---

## 6. Testing Strategy Review

### 6.1 Backend Test Coverage

**Test Scope:**
- [x] `UserRoleTest.java` — unit tests for `enumRoleOf()` and `isAdmin()` covering all new values and backward compat — comprehensive (8 test cases specified)
- [x] `SecurityConfigTest.java` — integration test with MockMvc verifying the role hierarchy and endpoint access — covers the critical security path
- [x] `UserManagementServiceRoleValidationTest.java` — unit tests for the new validation logic — covers all 5 cases

**Coverage Target:**
- `UserRole` class: 100% (all branches of `isAdmin()` and `enumRoleOf()` covered by specified tests)
- `UserManagementService` validation: 100% for the validation method
- `SecurityConfig`: Covered by integration test (cannot achieve traditional line coverage on Spring configuration beans)

**Findings:**
- ✅ **Well Tested**: Core enum behaviour, security config, and validation logic are all covered.
- ⚠️ **`SecurityConfigTest` scenario gap**: The test covers `PLATFORM_ADMIN`, `BUILDING_OWNER`, `STUDENT`, and unauthenticated. It does NOT test `BUILDING_STAFF`. Add one test case: `@WithMockUser(roles = "BUILDING_STAFF")` on `/api/admin/stats` should return 403.
- ⚠️ **Missing test for `enumRoleOf()` unknown value**: The specified tests do not include a case for `enumRoleOf("UNKNOWN_ROLE")` throwing `IllegalArgumentException`. Add this negative case to `UserRoleTest`.
- ⚠️ **No test for `PLATFORM_ADMIN` JWT expiry**: `SecurityConfigTest` (integration) does not verify that `PLATFORM_ADMIN` users receive a 4-hour token and `STUDENT` users receive a 10-hour token. This is the hardest path to test in a unit/integration test, but it is critical enough to warrant a `JwtUtilTest` unit test.

**Recommendations:**
1. Add `isAdmin_buildingStaff_returnsFalse()` test — specified; confirm it is included.
2. Add `SecurityConfigTest` case for `BUILDING_STAFF` role on `/api/admin/stats` (expects 403).
3. Add `UserRoleTest` case: `enumRoleOf_unknownRole_throwsIllegalArgumentException()`.
4. Add `JwtUtilTest` cases: `generateToken_platformAdminRoles_usesAdminExpiry()` and `generateToken_studentRoles_usesDefaultExpiry()`.

### 6.2 Frontend Test Coverage

**Test Scope:**
- [x] `role.service.spec.ts` — 8 test cases covering all methods and the null/unknown edge cases
- [x] `admin.guard.spec.ts` — 3 test cases covering all guard paths
- [x] `student.guard.spec.ts` — 4 test cases covering all redirect paths

**Findings:**
- ✅ `role.service.spec.ts` covers `hasRole_whenCurrentUserIsNull_returnsFalse` (EC-5) — critical null safety test present.
- ⚠️ `admin.guard.spec.ts` missing `BUILDING_OWNER` test case: what happens when a `BUILDING_OWNER` user hits an admin route? The guard should redirect to `/dashboard`, not `/admin`. Add: `canActivate_buildingOwnerUser_redirectsToDashboard_returnsFalse`.

**Recommendations:**
1. Add `admin.guard.spec.ts` test case for `BUILDING_OWNER` — redirects to `/dashboard`, not `/admin`.

### 6.3 Edge Case Coverage

| Edge Case | Tested? | Location |
|---|---|---|
| EC-1: In-flight JWT with `ROLE_ADMIN` | No (by design — stateless JWT) | N/A — documented in BA §11.1 |
| EC-2: Legacy API client sending `LANDLORD` role | Yes (UserManagementServiceTest) | `createUser_withDeprecatedLandlordRole_throwsValidationException` |
| EC-3: Activity log filter with `ADMIN` role | No | Not explicitly tested — see note below |
| EC-4: AdminStatsService counting wrong role | Partially (specified but implementation test not detailed) | Needs service test |
| EC-5: RoleService called before login (null user) | Yes | `role.service.spec.ts` |
| EC-7: `findByUserRole(UserRole.ADMIN)` returns empty | No | Handled by migration — acceptable |
| EC-8: enumRoleOf mixed-case | Partially | `enumRoleOf_platformAdminUpperCase_returnsPlatformAdmin` covers this |

**Missing Test Scenarios:**
1. `ActivityLogService` receiving `userRole=ADMIN` in filter request post-migration — should return HTTP 200 with empty results (not 500). Add integration smoke test or note in Task 6.
2. `AdminStatsService.countByUserRole(PLATFORM_ADMIN)` returns correct count — add to `UserManagementServiceRoleValidationTest` or a dedicated `AdminStatsServiceTest`.

---

## 7. Performance Review

### 7.1 Backend Performance

**Database Query Optimization:**
- Lazy vs Eager loading: No entity changes — no impact
- N+1 queries: None introduced
- Pagination: Not applicable
- Caching: `user_role` is stored on the authenticated user's security context — no additional DB queries for role checks

**Findings:**
No performance concerns. This is a rename migration with zero runtime query overhead.

### 7.2 Frontend Performance

No new HTTP calls, no new subscriptions, no new observables. `RoleService` is a pure synchronous facade reading from an in-memory `BehaviorSubject`. Zero performance impact.

### 7.3 Scalability Considerations

- Stateless JWT: Role stored in token claim — no server-side session state. No scalability concern.
- `RoleService`: Pure synchronous — no async, no network.

---

## 8. Integration Review

### 8.1 MapStruct Integration

`AppUserMapper.mapAdminRole()` is mentioned in the BA as a location that validates role is not STUDENT. **This must be audited in Task 5.** If it contains a direct `UserRole.ADMIN` or `UserRole.LANDLORD` comparison, it must be updated. MapStruct regenerates mappers at build time — run `./gradlew build` after any mapper changes.

### 8.2 FF4J Feature Flags

No feature flag wrapping of this migration is required. This is a database-level and code rename — not a feature that needs staged rollout. The Strangler Pattern (deprecated values retained) provides the rollback safety that feature flags would otherwise provide.

### 8.3 ActivityLogService Integration

`ActivityLogService` parses `userRole` filter parameters from API requests. After this migration:
- New logs will contain `PLATFORM_ADMIN`, `BUILDING_OWNER` — correctly handled by `enumRoleOf()` and `valueOf()`
- Requests filtering by old value `ADMIN` — `valueOf("ADMIN")` succeeds; returns empty results (correct)
- The activity logs role filter dropdown in the UI (Task 10) must be updated to `['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF', 'STUDENT']`

---

## 9. User Experience Review

### 9.1 Role Display Labels

| Enum Value | Display Label (UI) | Source |
|---|---|---|
| `PLATFORM_ADMIN` | "Platform Admin" | `user-management.component.ts` switch |
| `BUILDING_OWNER` | "Building Owner" | `user-management.component.ts` switch |
| `BUILDING_STAFF` | "Building Staff" | `user-management.component.ts` switch |
| `STUDENT` | "Student" | `user-management.component.ts` switch |

Labels are clear and consistent with the BA §8.2 specification. No concerns.

### 9.2 Create User Dialog

Default role changing from `LANDLORD` to `BUILDING_OWNER` is correct. Role options `['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF']` are appropriate for admin-created users.

### 9.3 Activity Log Filter Dropdown

Updated to `['PLATFORM_ADMIN', 'BUILDING_OWNER', 'BUILDING_STAFF', 'STUDENT']` is correct and complete.

---

## 10. Deployment & Operations Review

### 10.1 Configuration Management

- No new environment variables required
- No FF4J feature flag configuration required
- JWT expiry times (`JWT_ADMIN_EXPIRATION`, `JWT_EXPIRATION`) unchanged

### 10.2 Rollback Strategy

**Migration rollback**: If rollback is needed post-deployment in a non-production environment, a compensating migration is required:

```sql
-- V2026_02_25_0002__rollback_user_roles.sql (only if needed)
UPDATE app_user SET user_role = 'ADMIN' WHERE user_role = 'PLATFORM_ADMIN';
UPDATE app_user SET user_role = 'LANDLORD' WHERE user_role = 'BUILDING_OWNER';
```

This is NOT a standard rollback — it must be a forward migration. Document this in deployment notes.

**Code rollback**: Application code cannot be rolled back independently of the database migration — if the migration has run, the code must also include the new enum values (otherwise deserialization of `PLATFORM_ADMIN` will throw `IllegalArgumentException`). Rollback requires both a compensating migration AND a code revert.

### 10.3 Logging

The `@Deprecated` annotation on enum values may generate compiler warnings at build time. Configure `build.gradle` to suppress deprecation warnings only for the enum file, or accept the warnings during the deprecation window.

---

## 11. Risk Assessment

### 11.1 Technical Risks

| Risk ID | Risk Description | Impact | Likelihood | Mitigation Strategy |
|---|---|---|---|---|
| TR-1 | Missed `hasRole('STUDENT')` annotation that previously worked via ADMIN hierarchy | High | Medium | Mandatory audit: `rg "hasRole\('STUDENT'\)"` before Task 4 complete; `SecurityConfigTest` covers representative endpoints |
| TR-2 | Missed `hasRole('ADMIN')` or `hasAnyRole.*ADMIN` annotation | High | Low | `rg` acceptance criteria in Task 4 verifies zero remaining occurrences |
| TR-3 | `AppUserMapper.mapAdminRole()` not updated — creates new users with wrong role validation | Medium | Medium | Add to Task 5 checklist; caught by `UserManagementServiceRoleValidationTest` |
| TR-4 | `AdminStatsService` silently returns 0 admins post-migration | Medium | High | Explicitly specified in Task 5; add `AdminStatsServiceTest` |
| TR-5 | Flyway migration uses explicit `BEGIN`/`COMMIT` — conflict with driver auto-transaction | Low | Low | Use Task 1 SQL (no BEGIN/COMMIT), not solution-design SQL |

### 11.2 Security Risks

| Risk ID | Risk Description | Severity | Mitigation Strategy |
|---|---|---|---|
| SR-1 | `PLATFORM_ADMIN` loses access to `hasRole('STUDENT')` endpoints that were previously accessible via role hierarchy | High | Mandatory `hasRole('STUDENT')` audit in Task 4 |
| SR-2 | In-flight `ROLE_ADMIN` JWT after deploy allows access with old role claims | Low | Acceptable — expires ≤4 hours; no blacklist required |

### 11.3 Operational Risks

| Risk ID | Risk Description | Impact | Mitigation Strategy |
|---|---|---|---|
| OR-1 | Frontend deployed before backend migration runs — `PLATFORM_ADMIN` string in frontend, `ADMIN` in DB | Medium | Deploy backend (Flyway + code) first, then frontend; or deploy atomically via Docker Compose |
| OR-2 | Compiler deprecation warnings from `@Deprecated` enum values in non-deprecated usages | Low | Acceptable during deprecation window; clean up in next task |

---

## 12. Implementation Readiness Assessment

### 12.1 Blockers

**Critical Blockers (must fix before starting):**
None. The plan is sound and implementation can proceed.

**Medium Blockers (should fix before starting):**
1. **`hasRole('STUDENT')` audit**: Run `rg "hasRole\('STUDENT'\)" backend/src/main/java --include="*.java"` before Task 4 and document each occurrence with access intent. This must be done before replacing role strings — a missed endpoint could silently block platform admins.
2. **`AppUserMapper` audit**: Run `rg "UserRole\.(ADMIN|LANDLORD)" backend/src/main/java --include="*.java" -l` before Task 5 to produce the complete file list.

**Minor Blockers (can fix during implementation):**
1. Remove `BEGIN`/`COMMIT` from Flyway SQL (use Task 1 version, not solution-design version)
2. Add Javadoc to `enumRoleOf()` documenting display-name matching
3. Add `BUILDING_STAFF` test to `SecurityConfigTest`
4. Add negative case (`enumRoleOf` unknown value) to `UserRoleTest`
5. Add `JwtUtilTest` for token expiry by role
6. Add `BUILDING_OWNER` test case to `admin.guard.spec.ts`

### 12.2 Open Questions

**Questions for Developer:**
1. Does `AppUserMapper.mapAdminRole()` contain direct `UserRole.ADMIN` or `UserRole.LANDLORD` comparisons? If so, add it to Task 5. If it delegates to `isAdmin()`, no change needed.
2. Does the global `@ControllerAdvice` map `IllegalArgumentException` to HTTP 400? Confirm this before Task 5 testing.
3. Does `ActivityLogService` use `UserRole.valueOf()` (matches enum name) or `enumRoleOf()` (matches display name) when parsing the `userRole` filter parameter? The answer determines whether `?userRole=ADMIN` filter requests work post-migration.

### 12.3 Dependencies

**Internal Dependencies:**
- [x] Flyway migration infrastructure in place (confirmed working)
- [x] `isAdmin()` method exists on `UserRole` enum (confirmed in §2.2)
- [x] `AuthService.currentUserValue` getter returns `User | null` (confirmed in `auth.service.ts`)
- [x] Spring Security `RoleHierarchyImpl.fromHierarchy()` API available (already used in `SecurityConfig`)

---

## 13. Final Recommendations

### 13.1 Must-Fix Issues (P0)

None blocking implementation start. See P1 items.

### 13.2 Should-Fix Issues (P1)

1. **`hasRole('STUDENT')` Endpoint Audit**
   - **Problem**: The role hierarchy change removes ADMIN's inherited access to STUDENT-scoped endpoints. Any `hasRole('STUDENT')` annotation that admin previously accessed via hierarchy will silently block PLATFORM_ADMIN.
   - **Impact**: Silent access regression for platform admins on student endpoints they legitimately used.
   - **Solution**: Run `rg "hasRole\('STUDENT'\)"` before completing Task 4. For each hit, verify whether PLATFORM_ADMIN should have access; if yes, update to `hasAnyRole('STUDENT', 'PLATFORM_ADMIN')`.
   - **Estimated Effort**: 15–30 min

2. **`AppUserMapper` Audit**
   - **Problem**: Not included in Task 5 file list despite BA flagging it.
   - **Impact**: New users created with deprecated role values may pass mapper validation when they should not.
   - **Solution**: Add to Task 5 file list; run rg search to confirm whether update is needed.
   - **Estimated Effort**: 10 min

3. **`AdminStatsService` Test**
   - **Problem**: Task 5 specifies the fix (`countByUserRole(PLATFORM_ADMIN)`) but no test verifies it.
   - **Impact**: Silent regression — stats dashboard shows 0 admins post-migration until manually noticed.
   - **Solution**: Add `AdminStatsService` test verifying correct count target role.
   - **Estimated Effort**: 20 min

### 13.3 Nice-to-Fix Issues (P2)

1. **`JwtUtilTest` for token expiry by role**
   - **Problem**: Token expiry logic change in Task 3 has no dedicated unit test.
   - **Solution**: Add `JwtUtilTest` with two cases: `PLATFORM_ADMIN` → admin expiry, `STUDENT` → default expiry.

2. **`AdminGuard` redirect comment**
   - **Problem**: Authenticated non-admin users redirect to `/dashboard` which is a student route.
   - **Solution**: Add inline comment noting this is a temporary fallback; add `BUILDING_OWNER` test case to `admin.guard.spec.ts`.

3. **`enumRoleOf()` Javadoc**
   - **Problem**: The display-name matching behaviour is non-obvious.
   - **Solution**: Add Javadoc explaining that the method matches against the `role` display field, not the enum constant name.

---

## 14. Approval Decision

### Decision:

- [x] ⚠️ **APPROVED WITH CONDITIONS** - Can proceed if conditions addressed

### Rationale

The implementation plan is well-structured, architecturally sound, and appropriately scoped. The Strangler Pattern is correctly applied — backward compatibility is preserved throughout the deprecation window. The `RoleService` abstraction is the right design decision. Test coverage is materially complete with only targeted gaps.

**Implementation may proceed.** The conditions below must be addressed during implementation (not before):

**Conditions that must be met during implementation:**
1. **[C1] Run `hasRole('STUDENT')` audit before completing Task 4.** Document each occurrence; update to `hasAnyRole` where PLATFORM_ADMIN access is required. This is the single highest-risk step in the task.
2. **[C2] Audit `AppUserMapper` for direct `UserRole.ADMIN`/`LANDLORD` comparisons** before completing Task 5.
3. **[C3] Remove `BEGIN`/`COMMIT` from Flyway migration SQL** (use Task 1 version, not solution-design version).
4. **[C4] Add `BUILDING_STAFF` test case to `SecurityConfigTest`** (expects 403 on `/api/admin/stats`).
5. **[C5] Add negative test to `UserRoleTest`**: `enumRoleOf_unknownRole_throwsIllegalArgumentException()`.

**Re-review needed**: No. Conditions can be self-verified via the acceptance criteria in the implementation plan.

**Key strengths:**
- The decision to retain `@Deprecated` enum values rather than remove them is the correct engineering choice for a live system.
- `isAdmin()` centralisation correctly prevents the need to update 9+ call sites simultaneously.
- `RoleService` null-safety design (optional chaining on `currentUserValue`) correctly handles EC-5.
- The implementation plan's final validation sequence (`rg` searches + full build) provides strong regression safety.
- Task dependency graph is correct; parallel frontend/backend work is safe given the only shared contract is the role string values.

---

## 15. Review Checklist

### Architecture & Design
- [x] Backend architecture follows Spring Boot patterns
- [x] Frontend architecture follows Angular best practices
- [x] SOLID principles adhered to
- [x] No architectural anti-patterns

### Database & Data
- [x] Schema design is minimal and correct (data-only migration)
- [x] No indexes needed for this change
- [x] Flyway migration naming is correct; no explicit transaction wrappers needed
- [x] Data model supports all business requirements

### API & Integration
- [x] No API contract changes
- [x] No OpenAPI changes required
- [x] Integration points (JWT, SecurityConfig, MapStruct) identified
- [x] Error handling consistent with existing patterns

### Security
- [x] Authentication and authorization correct for role rename
- [x] Input validation updated in `UserManagementService`
- [⚠️] `hasRole('STUDENT')` endpoint audit required (C1 condition)
- [x] No new sensitive data exposure

### Code Quality
- [x] Exception handling robust
- [x] Naming conventions followed
- [x] No new code duplication (centralization improved)
- [x] Documentation adequate (with P2 Javadoc improvement recommended)

### Testing
- [x] Core test coverage adequate
- [⚠️] `BUILDING_STAFF` SecurityConfigTest case missing (C4 condition)
- [⚠️] Negative `enumRoleOf` test case missing (C5 condition)
- [x] Manual testing scenarios defined in Final Validation section

### Performance
- [x] No performance bottlenecks introduced
- [x] Migration execution time < 1s at university scale
- [x] No new DB queries for role checks at runtime

### Deployment
- [x] No new configuration required
- [x] Rollback strategy documented (compensating migration)
- [x] Deploy order: backend first, then frontend

### Risk Management
- [x] TR-1 (`hasRole('STUDENT')` audit) flagged as P1 condition
- [x] SR-2 (in-flight JWT) accepted as low risk
- [x] OR-1 (deploy order) documented

### Readiness
- [x] No critical blockers
- [x] Open questions are implementation-time verifications, not blockers
- [x] Dependencies confirmed ready
- [x] Implementation plan is clear and actionable with specific file paths and code snippets

---

## 16. Sign-off

**Tech Lead Review Completed**: 2026-02-25
**Next Steps**: Proceed to implementation. Create branch `feature/TASK-2-03-migrate-user-role-v2` from `develop`. Address C1–C5 conditions inline during their respective tasks. Final validation command sequence is defined in the implementation plan.
**Follow-up Required**: After implementation, verify with `rg "hasRole\('ADMIN'\)" backend/src/main/java` and `rg "'ADMIN'" frontend/src/app --include="*.ts"` return zero non-model results before merging.
