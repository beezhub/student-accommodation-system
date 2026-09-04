# User Management — UAT Test Cases

**Module:** User Management
**Route:** `/admin/users`
**Guard:** PlatformAdminGuard (PLATFORM_ADMIN only)
**Components:** UserManagementComponent, CreateUserDialog
**Environment:** http://localhost:4200

---

## Test Data Reference

| Item | Value |
|------|-------|
| Platform Admin | platformadmin@test.com / Admin@12345 |
| Tenant Admin | tenantadmin@test.com / Admin@12345 |
| Existing Tenants | BeezhHub Housing (id=1), TestTenant (id=2) |
| New Admin User | admin.new@test.com / Admin@12345, role: PLATFORM_ADMIN |
| New Tenant Admin | tenantadm.new@test.com / TenantAdmin@123 |

---

## Test Cases

---

### TC-USR-001: Create user with role PLATFORM_ADMIN — success

| Field | Value |
|-------|-------|
| **ID** | TC-USR-001 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: "Admin", lastName: "New", email: admin.new@test.com, password: Admin@12345, role: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens with empty fields |
| 2 | Enter "Admin" in First Name field | Field accepts input |
| 3 | Enter "New" in Last Name field | Field accepts input |
| 4 | Enter "admin.new@test.com" in Email field | Field accepts input |
| 5 | Enter "Admin@12345" in Password field | Field accepts input (masked) |
| 6 | Select "PLATFORM_ADMIN" from Role dropdown | Role selected |
| 7 | Click "Save" / "Create" button | Dialog closes. Success snackbar displayed. New user "Admin New" appears in user list with role PLATFORM_ADMIN |
| 8 | Log out and log in as admin.new@test.com / Admin@12345 | Login succeeds. User has platform admin access. |

| Field | Value |
|-------|-------|
| **Postconditions** | User admin.new@test.com exists with PLATFORM_ADMIN role. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-002: Create user with role TENANT_ADMIN — success

| Field | Value |
|-------|-------|
| **ID** | TC-USR-002 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. Tenant "BeezhHub Housing" (id=1) exists. |
| **Test Data** | firstName: "Tenant", lastName: "Admin", email: tenantadm.new@test.com, password: TenantAdmin@123, role: ADMIN, tenantId: 1 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Enter "Tenant" in First Name, "Admin" in Last Name | Fields populated |
| 3 | Enter "tenantadm.new@test.com" in Email field | Field accepts input |
| 4 | Enter "TenantAdmin@123" in Password field | Field accepts input |
| 5 | Select "ADMIN" from Role dropdown | Role selected |
| 6 | Select tenant "BeezhHub Housing" from Tenant dropdown (if required for this role) | Tenant assigned |
| 7 | Click "Save" / "Create" button | Dialog closes. Success snackbar. User appears in list with role ADMIN. |

| Field | Value |
|-------|-------|
| **Postconditions** | User tenantadm.new@test.com exists with ADMIN role assigned to BeezhHub Housing. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document whether tenant assignment is required/shown for ADMIN role. |

---

### TC-USR-003: Create user with role BUILDING_STAFF — success

| Field | Value |
|-------|-------|
| **ID** | TC-USR-003 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: "Building", lastName: "Staff", email: staff@test.com, password: Staff@12345, role: BUILDING_STAFF, tenantId: 1 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Enter "Building" in First Name, "Staff" in Last Name | Fields populated |
| 3 | Enter "staff@test.com" in Email, "Staff@12345" in Password | Fields populated |
| 4 | Select "BUILDING_STAFF" from Role dropdown | Role selected |
| 5 | Select tenant "BeezhHub Housing" from Tenant dropdown (if applicable) | Tenant assigned |
| 6 | Click "Save" / "Create" button | Dialog closes. Success snackbar. User appears in list with role BUILDING_STAFF. |

| Field | Value |
|-------|-------|
| **Postconditions** | User staff@test.com exists with BUILDING_STAFF role. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-004: Create user with empty first name — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-USR-004 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: (empty), lastName: "Test", email: "nofirst@test.com", password: "Admin@12345", role: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Leave First Name field empty | Field is blank |
| 3 | Fill in all other fields with valid data | Other fields populated |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error on First Name field indicating it is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No user created. Dialog remains open with validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-005: Create user with empty last name — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-USR-005 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: "Test", lastName: (empty), email: "nolast@test.com", password: "Admin@12345", role: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Enter "Test" in First Name, leave Last Name empty | Last Name blank |
| 3 | Fill in all other fields with valid data | Other fields populated |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error on Last Name field indicating it is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No user created. Dialog remains open with validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-006: Create user with empty email — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-USR-006 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: "Test", lastName: "User", email: (empty), password: "Admin@12345", role: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Enter "Test" in First Name, "User" in Last Name | Fields populated |
| 3 | Leave Email field empty | Email field blank |
| 4 | Fill in Password and Role with valid data | Fields populated |
| 5 | Click "Save" / "Create" button | Form does NOT submit. Validation error on Email field indicating it is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No user created. Dialog remains open with validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-007: Create user with invalid email format — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-USR-007 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: "Test", lastName: "User", email: "invalid-email", password: "Admin@12345", role: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Fill in First Name, Last Name, Password, Role with valid data | Fields populated |
| 3 | Enter "invalid-email" in the Email field | Field accepts input |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error on Email field indicating invalid email format |

| Field | Value |
|-------|-------|
| **Postconditions** | No user created. Dialog remains open with email validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Also test: "user@", "@domain.com", "user@.com" |

---

### TC-USR-008: Create user with password less than 8 characters — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-USR-008 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: "Test", lastName: "User", email: "shortpw@test.com", password: "Ab@1234" (7 chars), role: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Fill in First Name, Last Name, Email, Role with valid data | Fields populated |
| 3 | Enter "Ab@1234" (7 characters) in the Password field | Field accepts input |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error on Password field indicating minimum 8 characters required |

| Field | Value |
|-------|-------|
| **Postconditions** | No user created. Dialog remains open with password validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-009: Create user without selecting role — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-USR-009 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. |
| **Test Data** | firstName: "Test", lastName: "User", email: "norole@test.com", password: "Admin@12345", role: (not selected) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Fill in First Name, Last Name, Email, Password with valid data | Fields populated |
| 3 | Do NOT select a Role from the dropdown | Role field remains at default/empty |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error on Role field indicating it is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No user created. Dialog remains open with role validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-010: Create user with duplicate email — error

| Field | Value |
|-------|-------|
| **ID** | TC-USR-010 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. User platformadmin@test.com already exists. |
| **Test Data** | firstName: "Duplicate", lastName: "User", email: "platformadmin@test.com", password: "Admin@12345", role: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Create User" button | CreateUserDialog opens |
| 2 | Enter "Duplicate" in First Name, "User" in Last Name | Fields populated |
| 3 | Enter "platformadmin@test.com" in Email field | Field accepts input |
| 4 | Enter "Admin@12345" in Password, select PLATFORM_ADMIN role | Fields populated |
| 5 | Click "Save" / "Create" button | Backend returns error. Error snackbar or form error displayed indicating email already exists / is taken |
| 6 | Verify no duplicate user was created | User list unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | No duplicate user created. Error message displayed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-011: List all users — all users displayed

| Field | Value |
|-------|-------|
| **ID** | TC-USR-011 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. Multiple users exist in the system. |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/users` | User management page loads |
| 2 | Observe the user list/table | All users are displayed |
| 3 | Verify columns show: name, email, role, status (at minimum) | Column data is correct and readable |
| 4 | Verify pagination or scrolling if many users exist | All users accessible |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record total number of users displayed. |

---

### TC-USR-012: Filter users by role — PLATFORM_ADMIN

| Field | Value |
|-------|-------|
| **ID** | TC-USR-012 |
| **Module** | User Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. At least one PLATFORM_ADMIN user exists. |
| **Test Data** | Filter: PLATFORM_ADMIN |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the role filter/dropdown on the user list page | Filter control visible |
| 2 | Select "PLATFORM_ADMIN" from the filter | Filter applied |
| 3 | Observe the filtered user list | Only users with PLATFORM_ADMIN role are displayed |
| 4 | Verify no users with other roles (ADMIN, STUDENT, BUILDING_STAFF) appear | List correctly filtered |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. Filter can be cleared. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If no filter exists in UI, document that and mark as N/A. |

---

### TC-USR-013: Filter users by role — STUDENT

| Field | Value |
|-------|-------|
| **ID** | TC-USR-013 |
| **Module** | User Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/users` page. At least one STUDENT user exists. |
| **Test Data** | Filter: STUDENT |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate the role filter/dropdown on the user list page | Filter control visible |
| 2 | Select "STUDENT" from the filter | Filter applied |
| 3 | Observe the filtered user list | Only users with STUDENT role are displayed |
| 4 | Verify no users with other roles appear | List correctly filtered |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If no filter exists in UI, document that and mark as N/A. |

---

### TC-USR-014: Delete user — user removed from list

| Field | Value |
|-------|-------|
| **ID** | TC-USR-014 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. A deletable user exists (e.g., admin.new@test.com created in TC-USR-001). |
| **Test Data** | User to delete: admin.new@test.com |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/users` | User list loads |
| 2 | Locate "admin.new@test.com" in the user list | User row visible |
| 3 | Click the delete button for this user | Confirmation dialog appears |
| 4 | Confirm the deletion | User is removed from the list. Success snackbar displayed. |
| 5 | Refresh the page | User "admin.new@test.com" no longer appears in the list |
| 6 | Attempt to log in as admin.new@test.com / Admin@12345 | Login fails — account no longer exists or is deactivated |

| Field | Value |
|-------|-------|
| **Postconditions** | User admin.new@test.com is deleted / removed from the system. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document whether deletion is hard delete or soft delete (deactivation). |

---

### TC-USR-015: Unlock locked user account — user can log in again

| Field | Value |
|-------|-------|
| **ID** | TC-USR-015 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. A user account is in "locked" state (e.g., after multiple failed login attempts). |
| **Test Data** | Locked user: (use a test account that has been locked by entering wrong password multiple times) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Identify a locked user in the user list (status shows "Locked" or similar indicator) | Locked user visible with locked status |
| 2 | Click the "Unlock" button for the locked user | Confirmation dialog appears (if any) |
| 3 | Confirm the unlock action | User status changes from "Locked" to "Active". Success snackbar displayed. |
| 4 | Log out of platform admin account | Logged out |
| 5 | Log in as the previously locked user with correct credentials | Login succeeds. User can access the system normally. |

| Field | Value |
|-------|-------|
| **Postconditions** | User account is unlocked and functional. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | To set up: attempt 5+ failed logins on a test account to trigger lockout before running this test. |

---

### TC-USR-016: Tenant admin cannot access /admin/users — redirected

| Field | Value |
|-------|-------|
| **ID** | TC-USR-016 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | tenantadmin@test.com is a tenant admin (NOT platform admin). |
| **Test Data** | Tenant Admin: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenantadmin@test.com / Admin@12345 | Login succeeds. Tenant admin dashboard loads. |
| 2 | Manually navigate to `/admin/users` by entering the URL in the browser address bar | PlatformAdminGuard blocks access |
| 3 | Observe the result | User is redirected to their dashboard or an "Access Denied" page. User management page is NOT displayed. |
| 4 | Verify the sidebar/navigation does NOT show "User Management" link | Menu item not visible for this role |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. Access correctly denied. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-USR-017: Student cannot access /admin/users — redirected

| Field | Value |
|-------|-------|
| **ID** | TC-USR-017 |
| **Module** | User Management |
| **Priority** | Critical |
| **Preconditions** | A student user account exists and is logged in. |
| **Test Data** | Any student account credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as a student user | Login succeeds. Student dashboard loads. |
| 2 | Manually navigate to `/admin/users` by entering the URL in the browser address bar | Guard blocks access |
| 3 | Observe the result | User is redirected to the student dashboard or login page. User management page is NOT displayed. |
| 4 | Verify the sidebar/navigation does NOT show any admin links | No admin menu items visible |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. Access correctly denied. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

## Summary

| ID | Title | Priority | Pass/Fail |
|----|-------|----------|-----------|
| TC-USR-001 | Create user with role PLATFORM_ADMIN | Critical | ☐ |
| TC-USR-002 | Create user with role TENANT_ADMIN | Critical | ☐ |
| TC-USR-003 | Create user with role BUILDING_STAFF | High | ☐ |
| TC-USR-004 | Create with empty firstName — error | High | ☐ |
| TC-USR-005 | Create with empty lastName — error | High | ☐ |
| TC-USR-006 | Create with empty email — error | High | ☐ |
| TC-USR-007 | Create with invalid email — error | High | ☐ |
| TC-USR-008 | Create with password < 8 chars — error | High | ☐ |
| TC-USR-009 | Create without selecting role — error | High | ☐ |
| TC-USR-010 | Create with duplicate email — error | Critical | ☐ |
| TC-USR-011 | List all users | Critical | ☐ |
| TC-USR-012 | Filter users by role (PLATFORM_ADMIN) | High | ☐ |
| TC-USR-013 | Filter users by role (STUDENT) | Medium | ☐ |
| TC-USR-014 | Delete user | Critical | ☐ |
| TC-USR-015 | Unlock locked user account | Critical | ☐ |
| TC-USR-016 | Tenant admin cannot access /admin/users — redirected | Critical | ☐ |
| TC-USR-017 | Student cannot access /admin/users — redirected | Critical | ☐ |
