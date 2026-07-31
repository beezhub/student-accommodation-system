# Security & Authorization — UAT Test Cases

> **Module:** Security & Authorization
> **System Under Test:** Route protection (Angular guards), API authorization (Spring Security @PreAuthorize), tenant isolation, input security, account security
> **Environment:** Frontend http://localhost:4200 | Backend http://localhost:8080 | Swagger http://localhost:8080/swagger-ui.html | MailHog http://localhost:8025

---

## Test Data Reference

| Role | Email | Password |
|------|-------|----------|
| Platform Admin | platformadmin@test.com | Admin@12345 |
| Tenant Admin | tenantadmin@test.com | Admin@12345 |
| Student A | john.doe@test.com | Student@123 |
| Student B | jane.smith@test.com | Student@123 |
| Student C | alex.jones@test.com | Student@123 |

---

## Route Protection Tests

### TC-SEC-001: Unauthenticated Access to /admin Redirects to /login

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-001 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | User is NOT logged in. No JWT token in localStorage. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open a new browser session (or clear localStorage) | Clean state, no auth token |
| 2 | Navigate directly to http://localhost:4200/admin | Browser redirects to http://localhost:4200/login |
| 3 | Verify the login page is displayed | Login form with email and password fields is shown |
| 4 | Check the URL bar | URL is `/login` (or `/login?returnUrl=%2Fadmin`) |

| Field | Value |
|-------|-------|
| **Postconditions** | User remains on login page. No admin content exposed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-002: Student Accessing /admin Redirects to /dashboard

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-002 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in with verified email and complete profile. |
| **Test Data** | john.doe@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A at http://localhost:4200/login | Student dashboard loads at /dashboard |
| 2 | Manually navigate to http://localhost:4200/admin by typing in the URL bar | Browser redirects away from /admin |
| 3 | Verify the URL | URL is `/dashboard` (AdminGuard rejects, redirects to student home) |
| 4 | Verify no admin content is visible | Student sees their own dashboard, not admin panels |

| Field | Value |
|-------|-------|
| **Postconditions** | Student cannot access admin routes. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-003: Unauthenticated Access to /dashboard Redirects to /login

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-003 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | No active session. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Clear localStorage / open incognito window | No auth token |
| 2 | Navigate directly to http://localhost:4200/dashboard | Redirected to /login |
| 3 | Verify login page is shown | Login form displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Dashboard inaccessible without authentication. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-004: Platform Admin Accessing /dashboard Redirects to /admin

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-004 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Platform Admin is logged in. |
| **Test Data** | platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin | Admin dashboard loads at /admin |
| 2 | Manually navigate to http://localhost:4200/dashboard | Redirected away from /dashboard |
| 3 | Verify URL is /admin or admin home | StudentGuard rejects admin user, redirects to /admin |

| Field | Value |
|-------|-------|
| **Postconditions** | Admin users cannot access student routes. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-005: Tenant Admin Accessing /admin/users Blocked by PlatformAdminGuard

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-005 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin is logged in. |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin at http://localhost:4200/login | Admin dashboard loads |
| 2 | Manually navigate to http://localhost:4200/admin/users | Redirected — PlatformAdminGuard denies access |
| 3 | Verify URL is NOT /admin/users | Redirected to /admin or an "unauthorized" state |
| 4 | Verify the user management page is NOT displayed | No user list visible |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin cannot access platform-admin-only routes. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-006: Tenant Admin Accessing /admin/settings Blocked

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-006 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Tenant Admin is logged in. |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin | Dashboard loads |
| 2 | Navigate to http://localhost:4200/admin/settings | Redirected — PlatformAdminGuard blocks |
| 3 | Verify platform settings page is NOT shown | Access denied |

| Field | Value |
|-------|-------|
| **Postconditions** | Platform settings inaccessible to tenant admin. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Tenant admins have their own tenant-scoped settings at a different route. |

---

### TC-SEC-007: Tenant Admin Accessing /admin/feature-flags Blocked

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-007 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Tenant Admin is logged in. |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin | Dashboard loads |
| 2 | Navigate to http://localhost:4200/admin/feature-flags | Redirected |
| 3 | Verify feature flags page is NOT displayed | Access denied |

| Field | Value |
|-------|-------|
| **Postconditions** | Feature flags page restricted to platform admin. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-008: Tenant Admin Accessing /admin/activity-logs Blocked

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-008 |
| **Module** | Security & Authorization |
| **Priority** | Medium |
| **Preconditions** | Tenant Admin is logged in. |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin | Dashboard loads |
| 2 | Navigate to http://localhost:4200/admin/activity-logs | Redirected by PlatformAdminGuard |
| 3 | Verify activity logs page is NOT displayed | Access denied |

| Field | Value |
|-------|-------|
| **Postconditions** | Activity logs restricted to platform admin. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-009: Tenant Admin Accessing /admin/tenants Blocked

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-009 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Tenant Admin is logged in. |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin | Dashboard loads |
| 2 | Navigate to http://localhost:4200/admin/tenants | Redirected by PlatformAdminGuard |
| 3 | Verify tenant management page is NOT displayed | Access denied |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant management restricted to platform admin. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-010: Unverified Email Access to Protected Student Routes

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-010 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | A student account exists with email NOT verified (e.g., newly registered). |
| **Test Data** | Register a new student: unverified@test.com / Student@123 (do NOT click verification link) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as unverified@test.com at http://localhost:4200/login | Login succeeds, but limited access |
| 2 | Attempt to navigate to http://localhost:4200/dashboard/applications | Redirected to /email-verification-required (or equivalent) |
| 3 | Verify the email verification prompt is shown | Page instructs user to verify their email |
| 4 | Attempt to navigate to http://localhost:4200/dashboard/documents | Same redirect to email verification page |

| Field | Value |
|-------|-------|
| **Postconditions** | Unverified email users blocked from protected student routes by EmailVerifiedGuard. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-011: Must Change Password Guard Forces Password Change

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-011 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | An admin user exists with `mustChangePassword = true` (e.g., newly created by platform admin). |
| **Test Data** | Admin user created with temporary password and mustChangePassword flag |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as the admin user with mustChangePassword=true | Login succeeds |
| 2 | Observe the redirect | Immediately redirected to /change-password |
| 3 | Try to navigate to http://localhost:4200/admin | Redirected back to /change-password by MustChangePasswordGuard |
| 4 | Enter new password (e.g., `NewAdmin@12345`) and confirm | Password changed successfully |
| 5 | Navigate to /admin | Admin dashboard loads normally |

| Field | Value |
|-------|-------|
| **Postconditions** | Password changed. mustChangePassword cleared. Normal access restored. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## API Authorization Tests

### TC-SEC-012: Admin API Without Token Returns 401

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-012 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Swagger UI is accessible. No auth token configured. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open Swagger UI at http://localhost:8080/swagger-ui.html | Swagger loads |
| 2 | Call GET /api/admin/applications WITHOUT setting an Authorization header | Response: `401 Unauthorized` |
| 3 | Call GET /api/buildings (public endpoint) without token | Response: `200 OK` (public endpoints work) |

| Field | Value |
|-------|-------|
| **Postconditions** | Protected endpoints reject unauthenticated requests. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-013: Admin API With Expired Token Returns 401

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-013 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | An expired JWT token is available (log in, then wait for expiry, or craft an expired token). |
| **Test Data** | Expired JWT token |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Obtain a valid JWT token by logging in via POST /api/auth/login with platformadmin@test.com | Token received |
| 2 | Wait for token expiry (admin tokens expire in 4 hours) OR manually craft an expired JWT | Expired token ready |
| 3 | Call GET /api/admin/applications with the expired token in Authorization: Bearer header | Response: `401 Unauthorized` |
| 4 | Verify response body contains an error message about token expiry | Message indicates expired or invalid token |

| Field | Value |
|-------|-------|
| **Postconditions** | Expired tokens are rejected. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | For faster testing, temporarily set JWT_ADMIN_EXPIRATION to a very short value (e.g., 5000 ms). |

---

### TC-SEC-014: Student Token on Admin API Returns 403

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-014 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student A has a valid JWT token. |
| **Test Data** | john.doe@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /api/auth/login with john.doe@test.com / Student@123 | JWT token returned |
| 2 | Call GET /api/admin/applications with the student's token | Response: `403 Forbidden` |
| 3 | Call POST /api/buildings with the student's token and a valid body | Response: `403 Forbidden` |
| 4 | Call DELETE /api/rooms/{anyId} with the student's token | Response: `403 Forbidden` |

| Field | Value |
|-------|-------|
| **Postconditions** | Student tokens cannot access admin-only endpoints. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-015: Tenant Admin Token on Platform-Admin API Returns 403

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-015 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Tenant Admin has a valid JWT token. |
| **Test Data** | tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | POST /api/auth/login with tenantadmin@test.com / Admin@12345 | JWT token returned |
| 2 | Call GET /api/tenants with the tenant admin's token | Response: `403 Forbidden` |
| 3 | Call GET /api/platform/system-settings with the tenant admin's token | Response: `403 Forbidden` |
| 4 | Call POST /api/platform/document-types with the tenant admin's token | Response: `403 Forbidden` |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin tokens rejected on platform-admin-only endpoints. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-016: Student Accesses Own Applications Successfully

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-016 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student A has at least one submitted application. |
| **Test Data** | john.doe@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A → obtain JWT token | Token received |
| 2 | Call GET /api/applications/my-applications with Student A's token | Response: `200 OK` with list of Student A's applications |
| 3 | Verify returned applications belong to Student A | All application records have Student A's student ID |

| Field | Value |
|-------|-------|
| **Postconditions** | Student can access their own application data. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-017: Student Cannot Access Another Student's Application

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-017 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student A and Student B both have applications. Student A knows Student B's application ID. |
| **Test Data** | john.doe@test.com (Student A), jane.smith@test.com (Student B) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin → note Student B's application ID from the admin applications list | Application ID recorded (e.g., ID=42) |
| 2 | Log in as Student A → obtain JWT token | Token received |
| 3 | Call GET /api/applications/{studentB_appId} with Student A's token | Response: `403 Forbidden` or `404 Not Found` |
| 4 | Verify no data from Student B's application is returned | Response body contains error, not application data |

| Field | Value |
|-------|-------|
| **Postconditions** | Cross-student data access prevented. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-018: Student Deletes Own Application Successfully

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-018 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Student A has a PENDING or CANCELLED application eligible for deletion. |
| **Test Data** | john.doe@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A → obtain JWT token | Token received |
| 2 | Call GET /api/applications/my-applications to find a deletable application | Application ID obtained |
| 3 | Call DELETE /api/applications/{ownAppId} with Student A's token | Response: `200 OK` or `204 No Content` |
| 4 | Call GET /api/applications/my-applications again | Deleted application no longer in list |

| Field | Value |
|-------|-------|
| **Postconditions** | Student's own application deleted. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-019: Student Cannot Delete Another Student's Application

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-019 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student B has an application. Student A has a valid token. |
| **Test Data** | john.doe@test.com (Student A), Student B's application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Obtain Student B's application ID (via admin API) | ID recorded |
| 2 | Log in as Student A → obtain JWT token | Token received |
| 3 | Call DELETE /api/applications/{studentB_appId} with Student A's token | Response: `403 Forbidden` |
| 4 | Verify Student B's application still exists (via admin API) | Application unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | Cross-student deletion prevented. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Tenant Isolation Tests

### TC-SEC-020: Tenant Admin Sees Only Own Tenant Data

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-020 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Two tenants exist (Tenant A and Tenant B). Each has buildings, rooms, and applications. Tenant Admin belongs to Tenant A. |
| **Test Data** | tenantadmin@test.com (Tenant A admin) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin (Tenant A) | Dashboard loads |
| 2 | Navigate to Buildings list | Only Tenant A's buildings displayed (e.g., Residence Hall Alpha) |
| 3 | Navigate to Applications list | Only Tenant A's applications displayed |
| 4 | Navigate to Rooms list | Only rooms belonging to Tenant A's buildings shown |
| 5 | Call GET /api/buildings with Tenant A's admin token via Swagger | Response contains only Tenant A buildings |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant A admin sees only Tenant A data across all entities. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Requires multi-tenant setup with at least two tenants. |

---

### TC-SEC-021: Tenant Admin Cannot Access Other Tenant's Building by ID

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-021 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Tenant B has a building (e.g., ID=99). Tenant A admin is logged in. |
| **Test Data** | tenantadmin@test.com (Tenant A), Tenant B building ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant A admin → obtain JWT token | Token received |
| 2 | Call GET /api/buildings/{tenantB_buildingId} with Tenant A's token | Response: `404 Not Found` or `403 Forbidden` |
| 3 | Verify no building data from Tenant B is returned | Error response only |

| Field | Value |
|-------|-------|
| **Postconditions** | Cross-tenant building access blocked. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-022: Tenant Admin Cannot Access Other Tenant's Application by ID

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-022 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Tenant B has an application (e.g., ID=55). Tenant A admin is logged in. |
| **Test Data** | tenantadmin@test.com (Tenant A), Tenant B application ID |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant A admin → obtain JWT token | Token received |
| 2 | Call GET /api/admin/applications/{tenantB_appId} with Tenant A's token | Response: `404 Not Found` or `403 Forbidden` |
| 3 | Verify no application data from Tenant B is returned | Error response only |

| Field | Value |
|-------|-------|
| **Postconditions** | Cross-tenant application access blocked. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-023: Platform Admin Can Access Data Across All Tenants

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-023 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Multiple tenants exist with data. Platform admin is logged in. |
| **Test Data** | platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin | Admin dashboard loads |
| 2 | Navigate to Tenants list | All tenants displayed |
| 3 | Navigate to Buildings (if cross-tenant view available) or call GET /api/buildings via Swagger | Buildings from multiple tenants visible |
| 4 | Navigate to Users list | Users across all tenants listed |
| 5 | Access a specific building from Tenant A by ID | Building data returned successfully |
| 6 | Access a specific building from Tenant B by ID | Building data returned successfully |

| Field | Value |
|-------|-------|
| **Postconditions** | Platform admin has cross-tenant visibility. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Input Security Tests

### TC-SEC-024: XSS Injection in Name Field

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-024 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Student A is logged in with profile setup accessible. |
| **Test Data** | XSS payload: `<script>alert('XSS')</script>` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A | Dashboard loads |
| 2 | Navigate to Profile settings | Profile form displayed |
| 3 | Enter `<script>alert('XSS')</script>` in the First Name field | Text entered |
| 4 | Submit the form | Form either: (a) rejects with validation error, or (b) saves as plain text |
| 5 | If saved, reload the page and navigate back to profile | No JavaScript alert popup. Name displayed as literal text `<script>alert('XSS')</script>` |
| 6 | Inspect the page source (F12 → Elements) | Script tags are HTML-encoded (e.g., `&lt;script&gt;`) or sanitized |

| Field | Value |
|-------|-------|
| **Postconditions** | No XSS execution. Payload stored/displayed safely. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-025: SQL Injection in Search Field

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-025 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Tenant Admin is logged in. Application or building list has a search/filter field. |
| **Test Data** | SQL injection payload: `'; DROP TABLE app_user; --` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Tenant Admin | Dashboard loads |
| 2 | Navigate to Applications list | Application list with search/filter |
| 3 | Enter `'; DROP TABLE app_user; --` in the search field | Text entered |
| 4 | Submit/trigger the search | Either: (a) empty results returned, (b) validation error, or (c) results with no SQL error |
| 5 | Verify no error page or stack trace is shown | Normal UI response |
| 6 | Navigate to another page and back | Application still works; no tables dropped |
| 7 | Via Swagger, call GET /api/admin/applications?search='; DROP TABLE app_user; -- | Response: `200 OK` with empty results or all results (query parameterized) |

| Field | Value |
|-------|-------|
| **Postconditions** | No SQL injection. Database intact. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Spring Data JPA uses parameterized queries, preventing SQL injection. |

---

### TC-SEC-026: Extremely Long Input String

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-026 |
| **Module** | Security & Authorization |
| **Priority** | Medium |
| **Preconditions** | Student A is logged in. Application form accessible. |
| **Test Data** | A string of 10,000+ characters (e.g., "A" repeated 10,000 times) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A | Dashboard loads |
| 2 | Navigate to a form with a text field (e.g., application special requirements, or profile notes) | Form displayed |
| 3 | Paste a 10,000-character string into the text field | Text pasted (may be truncated by UI maxlength) |
| 4 | Submit the form | Either: (a) validation error for exceeding max length, or (b) accepted if within DB limits |
| 5 | If rejected, verify error message is user-friendly | Message like "Field must be less than X characters" |
| 6 | If accepted, verify no server error (500) or crash | Server handles gracefully |
| 7 | Via Swagger, attempt POST with 10,000-char field in request body | Response: `400 Bad Request` with validation error, NOT `500 Internal Server Error` |

| Field | Value |
|-------|-------|
| **Postconditions** | Application handles oversized input gracefully. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Account Security Tests

### TC-SEC-027: Account Lockout After Failed Login Attempts

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-027 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student A's account is active and not locked. |
| **Test Data** | john.doe@test.com with wrong password: `WrongPass123` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to http://localhost:4200/login | Login page displayed |
| 2 | Enter john.doe@test.com / WrongPass123 → Submit | Login fails: "Invalid credentials" or similar error |
| 3 | Repeat step 2 four more times (total 5 failed attempts) | Each attempt shows error message |
| 4 | On the 6th attempt with the CORRECT password (Student@123) → Submit | Login fails: "Account locked" message displayed |
| 5 | Via Swagger, POST /api/auth/login with correct credentials | Response: `403 Forbidden` or `401 Unauthorized` with locked message |

| Field | Value |
|-------|-------|
| **Postconditions** | Account is locked after 5+ failed attempts. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Exact lockout threshold depends on backend configuration. Adjust steps if threshold differs. |

---

### TC-SEC-028: Locked Account Shows Appropriate Message

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-028 |
| **Module** | Security & Authorization |
| **Priority** | High |
| **Preconditions** | Student A's account is locked (from TC-SEC-027). |
| **Test Data** | john.doe@test.com / Student@123 (correct password) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to http://localhost:4200/login | Login page displayed |
| 2 | Enter john.doe@test.com / Student@123 (correct password) → Submit | Login fails |
| 3 | Verify the error message | Message indicates account is locked (e.g., "Your account has been locked. Please contact support.") |
| 4 | Verify the message does NOT reveal whether the password was correct | Message focuses on lockout, not credential validity |

| Field | Value |
|-------|-------|
| **Postconditions** | Locked account displays clear, secure error message. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-029: Platform Admin Unlocks Account

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-029 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student A's account is locked. Platform Admin is available. |
| **Test Data** | platformadmin@test.com / Admin@12345, john.doe@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Platform Admin at http://localhost:4200/login | Admin dashboard loads |
| 2 | Navigate to Users → find john.doe@test.com | User record shown with locked status |
| 3 | Click Unlock (or toggle lock status) → Confirm | Account unlocked successfully |
| 4 | Log out as Platform Admin | Logged out |
| 5 | Log in as john.doe@test.com / Student@123 | Login succeeds — student dashboard loads |

| Field | Value |
|-------|-------|
| **Postconditions** | Student A's account is unlocked and functional. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SEC-030: Token Blacklisted After Logout

| Field | Value |
|-------|-------|
| **ID** | TC-SEC-030 |
| **Module** | Security & Authorization |
| **Priority** | Critical |
| **Preconditions** | Student A is logged in with a valid JWT token. |
| **Test Data** | john.doe@test.com / Student@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as Student A via POST /api/auth/login | JWT token returned; note the token value |
| 2 | Call GET /api/applications/my-applications with the token | Response: `200 OK` — token is valid |
| 3 | Log out via POST /api/auth/logout with the token in Authorization header | Response: `200 OK` — logout successful |
| 4 | Call GET /api/applications/my-applications with the SAME token | Response: `401 Unauthorized` — token is blacklisted/invalidated |
| 5 | In the frontend, verify that after clicking Logout, navigating to /dashboard redirects to /login | Redirect to login confirmed |

| Field | Value |
|-------|-------|
| **Postconditions** | Token is invalidated after logout. Cannot be reused. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |
