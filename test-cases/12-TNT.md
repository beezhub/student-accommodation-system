# Tenant Management — UAT Test Cases

**Module:** Tenant Management
**Route:** `/admin/tenants`
**Guard:** PlatformAdminGuard (PLATFORM_ADMIN only)
**Components:** TenantManagementComponent, TenantFormDialog, TenantAdminManagement
**Environment:** http://localhost:4200

---

## Test Data Reference

| Item | Value |
|------|-------|
| Platform Admin | platformadmin@test.com / Admin@12345 |
| Tenant Admin | tenantadmin@test.com / Admin@12345 |
| Existing Tenant A | BeezhHub Housing (id=1, domain: beezzhub.com) |
| Existing Tenant B | TestTenant (id=2, domain: test.com) |
| New Tenant | Alpha Housing, domain: alpha.com |
| New Tenant Admin | tenantadm.new@test.com / TenantAdmin@123 |

---

## Test Cases

---

### TC-TNT-001: Create tenant with valid name and domain

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-001 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/tenants` page. |
| **Test Data** | Name: "Alpha Housing", Domain: "alpha.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Tenant" / "Create Tenant" button | Tenant form dialog opens with empty fields |
| 2 | Enter "Alpha Housing" in the Name field | Name field accepts input, no validation error |
| 3 | Enter "alpha.com" in the Domain field | Domain field accepts input, no validation error |
| 4 | Click "Save" / "Create" button | Dialog closes. Success snackbar displayed. New tenant "Alpha Housing" appears in the tenant list |
| 5 | Verify the new tenant row shows name "Alpha Housing" and domain "alpha.com" | Correct name and domain displayed |
| 6 | Verify the new tenant status is "Active" by default | Status shows Active |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant "Alpha Housing" exists in the system with Active status |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-002: Create tenant with empty name — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-002 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/tenants` page. |
| **Test Data** | Name: (empty), Domain: "newdomain.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Tenant" button | Tenant form dialog opens |
| 2 | Leave the Name field empty | Name field is blank |
| 3 | Enter "newdomain.com" in the Domain field | Domain field accepts input |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error displayed on Name field indicating it is required |
| 5 | Verify no new tenant was created in the tenant list | Tenant list unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | No new tenant created. Dialog remains open with validation error visible. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-003: Create tenant with name exceeding 200 characters — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-003 |
| **Module** | Tenant Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/tenants` page. |
| **Test Data** | Name: 201-character string (e.g., "A" repeated 201 times), Domain: "longname.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Tenant" button | Tenant form dialog opens |
| 2 | Paste a 201-character string into the Name field | Field accepts or truncates input |
| 3 | Enter "longname.com" in the Domain field | Domain field accepts input |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error displayed indicating name exceeds maximum length of 200 characters, OR backend returns 400 error with appropriate message |
| 5 | Verify no new tenant was created | Tenant list unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | No new tenant created. Error message visible. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Check whether validation is client-side (maxlength attribute) or server-side (400 response). |

---

### TC-TNT-004: Create tenant with domain exceeding 255 characters — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-004 |
| **Module** | Tenant Management |
| **Priority** | Medium |
| **Preconditions** | Logged in as platformadmin@test.com. On `/admin/tenants` page. |
| **Test Data** | Name: "Valid Tenant", Domain: 256-character string (e.g., "a" repeated 252 + ".com") |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Tenant" button | Tenant form dialog opens |
| 2 | Enter "Valid Tenant" in the Name field | Name field accepts input |
| 3 | Paste a 256-character string into the Domain field | Field accepts or truncates input |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error indicating domain exceeds maximum length of 255 characters, OR backend returns 400 error |
| 5 | Verify no new tenant was created | Tenant list unchanged |

| Field | Value |
|-------|-------|
| **Postconditions** | No new tenant created. Error message visible. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-005: Create tenant with duplicate name — verify behavior

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-005 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "BeezhHub Housing" already exists. |
| **Test Data** | Name: "BeezhHub Housing", Domain: "duplicate.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Tenant" button | Tenant form dialog opens |
| 2 | Enter "BeezhHub Housing" in the Name field | Field accepts input |
| 3 | Enter "duplicate.com" in the Domain field | Field accepts input |
| 4 | Click "Save" / "Create" button | Either: (a) backend rejects with error indicating duplicate name, OR (b) tenant is created (if duplicates are allowed). Document actual behavior. |
| 5 | If error: verify error message is user-friendly | Message clearly indicates the name is already in use |

| Field | Value |
|-------|-------|
| **Postconditions** | Behavior documented. If rejected, no duplicate tenant exists. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document whether system allows or rejects duplicate tenant names. |

---

### TC-TNT-006: Update tenant name

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-006 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "TestTenant" (id=2) exists. |
| **Test Data** | Updated name: "TestTenant Updated" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate "TestTenant" in the tenant list | Tenant row visible |
| 2 | Click the edit button / tenant row to open edit form | Tenant form dialog opens pre-populated with current name "TestTenant" and domain "test.com" |
| 3 | Clear the Name field and enter "TestTenant Updated" | Name field shows "TestTenant Updated" |
| 4 | Click "Save" / "Update" button | Dialog closes. Success snackbar displayed. Tenant list shows updated name "TestTenant Updated" |
| 5 | Refresh the page | Tenant still shows "TestTenant Updated" — change persisted |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant id=2 has name "TestTenant Updated". Revert to "TestTenant" after test if needed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-007: Update tenant domain

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-007 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "TestTenant" (id=2) exists with domain "test.com". |
| **Test Data** | Updated domain: "updated-test.com" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate "TestTenant" in the tenant list and click edit | Tenant form dialog opens with domain "test.com" |
| 2 | Clear the Domain field and enter "updated-test.com" | Domain field shows "updated-test.com" |
| 3 | Click "Save" / "Update" button | Dialog closes. Success snackbar displayed. Tenant list shows updated domain "updated-test.com" |
| 4 | Refresh the page | Domain still shows "updated-test.com" — change persisted |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant id=2 has domain "updated-test.com". Revert to "test.com" after test if needed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-008: Toggle tenant from active to inactive — tenant admin access restricted

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-008 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "TestTenant" (id=2) is Active. tenantadmin@test.com is an admin of Tenant id=2 and can currently log in. |
| **Test Data** | Tenant: TestTenant (id=2), Tenant Admin: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate "TestTenant" in the tenant list | Tenant row visible with Active status |
| 2 | Click the toggle/deactivate button for "TestTenant" | Confirmation dialog appears (if any) |
| 3 | Confirm the deactivation | Tenant status changes to "Inactive". Success snackbar displayed. |
| 4 | Log out of platform admin account | Logged out, redirected to login page |
| 5 | Attempt to log in as tenantadmin@test.com / Admin@12345 | Login is rejected OR user sees a restricted/suspended access message indicating the tenant is inactive |
| 6 | Verify the tenant admin cannot access any admin functionality | No admin routes accessible |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant "TestTenant" is Inactive. Tenant admin cannot access the system. Re-activate for subsequent tests. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-009: Toggle tenant from inactive to active — access restored

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-009 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "TestTenant" (id=2) is currently Inactive (from TC-TNT-008). |
| **Test Data** | Tenant: TestTenant (id=2), Tenant Admin: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Locate "TestTenant" in the tenant list | Tenant row visible with Inactive status |
| 2 | Click the toggle/activate button for "TestTenant" | Confirmation dialog appears (if any) |
| 3 | Confirm the activation | Tenant status changes to "Active". Success snackbar displayed. |
| 4 | Log out of platform admin account | Logged out |
| 5 | Log in as tenantadmin@test.com / Admin@12345 | Login succeeds. Tenant admin dashboard loads. |
| 6 | Verify tenant admin can access their admin functionality | Admin routes accessible, data visible |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant "TestTenant" is Active. Tenant admin has full access restored. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-010: View tenant list — all tenants displayed

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-010 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. At least two tenants exist (BeezhHub Housing, TestTenant). |
| **Test Data** | N/A |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/tenants` | Tenant management page loads |
| 2 | Observe the tenant list/table | All existing tenants are displayed (at minimum: "BeezhHub Housing" and "TestTenant") |
| 3 | Verify each row shows tenant name, domain, and status | Columns visible with correct data: BeezhHub Housing / beezzhub.com, TestTenant / test.com |
| 4 | Verify pagination or scrolling if more tenants exist | All tenants accessible via pagination or scroll |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record total number of tenants displayed. |

---

### TC-TNT-011: View tenant details

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-011 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "BeezhHub Housing" (id=1) exists. |
| **Test Data** | Tenant: BeezhHub Housing (id=1, domain: beezzhub.com) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/tenants` | Tenant list page loads |
| 2 | Click on "BeezhHub Housing" row or its detail/view button | Tenant detail view or expanded section opens |
| 3 | Verify tenant name is "BeezhHub Housing" | Correct name displayed |
| 4 | Verify tenant domain is "beezzhub.com" | Correct domain displayed |
| 5 | Verify tenant status (Active/Inactive) is shown | Status is displayed |
| 6 | Verify tenant ID or creation date is visible (if shown in UI) | Additional metadata visible if applicable |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-012: Create tenant admin for a tenant

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-012 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "BeezhHub Housing" (id=1) exists. TenantAdminManagement sub-component accessible. |
| **Test Data** | Tenant: BeezhHub Housing. New admin: firstName: "Tenant", lastName: "NewAdmin", email: tenantadm.new@test.com, password: TenantAdmin@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to tenant "BeezhHub Housing" admin management section | TenantAdminManagement view loads for BeezhHub Housing |
| 2 | Click "Add Admin" / "Create Tenant Admin" button | Tenant admin creation form opens |
| 3 | Enter "Tenant" in First Name field | Field accepts input |
| 4 | Enter "NewAdmin" in Last Name field | Field accepts input |
| 5 | Enter "tenantadm.new@test.com" in Email field | Field accepts input |
| 6 | Enter "TenantAdmin@123" in Password field | Field accepts input (masked) |
| 7 | Click "Save" / "Create" button | Dialog closes. Success snackbar displayed. New admin "Tenant NewAdmin" appears in the tenant admin list for BeezhHub Housing |
| 8 | Log out and log in as tenantadm.new@test.com / TenantAdmin@123 | Login succeeds. User has tenant admin access for BeezhHub Housing |

| Field | Value |
|-------|-------|
| **Postconditions** | New tenant admin exists for BeezhHub Housing. Admin can log in and access tenant-scoped features. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-013: Create tenant admin with empty first name — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-013 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. In TenantAdminManagement for any tenant. |
| **Test Data** | firstName: (empty), lastName: "Admin", email: "empty@test.com", password: "Admin@12345" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Admin" button | Tenant admin form opens |
| 2 | Leave First Name field empty | Field is blank |
| 3 | Enter "Admin" in Last Name, "empty@test.com" in Email, "Admin@12345" in Password | Other fields populated |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error on First Name field indicating it is required |

| Field | Value |
|-------|-------|
| **Postconditions** | No admin created. Form remains open with validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-014: Create tenant admin with invalid email — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-014 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. In TenantAdminManagement for any tenant. |
| **Test Data** | firstName: "Test", lastName: "Admin", email: "not-an-email", password: "Admin@12345" |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Admin" button | Tenant admin form opens |
| 2 | Enter "Test" in First Name, "Admin" in Last Name | Fields populated |
| 3 | Enter "not-an-email" in the Email field | Field accepts input |
| 4 | Enter "Admin@12345" in Password | Field populated |
| 5 | Click "Save" / "Create" button | Form does NOT submit. Validation error on Email field indicating invalid email format |

| Field | Value |
|-------|-------|
| **Postconditions** | No admin created. Form remains open with email validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Also test with: "admin@", "@test.com", "admin@.com" |

---

### TC-TNT-015: Create tenant admin with password less than 8 characters — validation error

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-015 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. In TenantAdminManagement for any tenant. |
| **Test Data** | firstName: "Test", lastName: "Admin", email: "short@test.com", password: "Abc@123" (7 chars) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Admin" button | Tenant admin form opens |
| 2 | Enter "Test" in First Name, "Admin" in Last Name, "short@test.com" in Email | Fields populated |
| 3 | Enter "Abc@123" (7 characters) in the Password field | Field accepts input |
| 4 | Click "Save" / "Create" button | Form does NOT submit. Validation error on Password field indicating minimum 8 characters required |

| Field | Value |
|-------|-------|
| **Postconditions** | No admin created. Form remains open with password validation error. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-016: List tenant admins for a specific tenant

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-016 |
| **Module** | Tenant Management |
| **Priority** | High |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant "BeezhHub Housing" (id=1) has at least one tenant admin. |
| **Test Data** | Tenant: BeezhHub Housing (id=1) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to tenant "BeezhHub Housing" admin management section | TenantAdminManagement view loads |
| 2 | Observe the tenant admin list | All admins assigned to BeezhHub Housing are displayed |
| 3 | Verify each row shows admin name, email, and status (active/inactive) | Columns display correct data |
| 4 | Verify admins from other tenants are NOT shown | Only BeezhHub Housing admins listed |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Record the count of admins displayed. |

---

### TC-TNT-017: Deactivate tenant admin — admin can no longer log in

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-017 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Logged in as platformadmin@test.com. Tenant admin tenantadm.new@test.com exists and is active for BeezhHub Housing (created in TC-TNT-012). |
| **Test Data** | Tenant admin: tenantadm.new@test.com / TenantAdmin@123 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to BeezhHub Housing admin management section | TenantAdminManagement view loads |
| 2 | Locate "tenantadm.new@test.com" in the admin list | Admin row visible with Active status |
| 3 | Click the deactivate button for this admin | Confirmation dialog appears (if any) |
| 4 | Confirm deactivation | Admin status changes to "Inactive". Success snackbar displayed. |
| 5 | Log out of platform admin account | Logged out |
| 6 | Attempt to log in as tenantadm.new@test.com / TenantAdmin@123 | Login is rejected OR access denied message displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin tenantadm.new@test.com is deactivated and cannot log in. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-018: Tenant isolation — Tenant A admin cannot see Tenant B data

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-018 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | Two tenants exist: BeezhHub Housing (id=1) and TestTenant (id=2). tenantadmin@test.com is an admin of one specific tenant. Both tenants have data (buildings, applications, etc.). |
| **Test Data** | Tenant Admin: tenantadmin@test.com / Admin@12345 (admin of Tenant id=1 or id=2 — document which) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenantadmin@test.com / Admin@12345 | Login succeeds. Tenant admin dashboard loads. |
| 2 | Navigate to buildings / applications / rooms management | Data list loads |
| 3 | Verify only data belonging to the tenant admin's own tenant is visible | Only own tenant's buildings, rooms, applications displayed |
| 4 | Verify no data from the other tenant appears in any list | No cross-tenant data leakage |
| 5 | Attempt to access another tenant's resource via direct URL manipulation (e.g., change tenant ID in API call if possible) | Request is rejected with 403 Forbidden or data is filtered out |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. Tenant isolation confirmed. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document which tenant the admin belongs to and what data was checked. |

---

### TC-TNT-019: Tenant admin cannot access /admin/tenants — redirected

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-019 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | tenantadmin@test.com is a tenant admin (NOT platform admin). User is logged in. |
| **Test Data** | Tenant Admin: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenantadmin@test.com / Admin@12345 | Login succeeds. Tenant admin dashboard loads. |
| 2 | Manually navigate to `/admin/tenants` by entering the URL in the browser address bar | PlatformAdminGuard blocks access |
| 3 | Observe the result | User is redirected to their dashboard or an "Access Denied" / "Unauthorized" page. Tenant management page is NOT displayed. |
| 4 | Verify the sidebar/navigation does NOT show "Tenant Management" link | Menu item not visible for this role |

| Field | Value |
|-------|-------|
| **Postconditions** | No changes to system state. Access correctly denied. |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-TNT-020: Student cannot access /admin/tenants — redirected

| Field | Value |
|-------|-------|
| **ID** | TC-TNT-020 |
| **Module** | Tenant Management |
| **Priority** | Critical |
| **Preconditions** | A student user account exists and is logged in. |
| **Test Data** | Any student account credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as a student user | Login succeeds. Student dashboard loads. |
| 2 | Manually navigate to `/admin/tenants` by entering the URL in the browser address bar | Guard blocks access |
| 3 | Observe the result | User is redirected to the student dashboard or login page. Tenant management page is NOT displayed. |
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
| TC-TNT-001 | Create tenant with valid name and domain | Critical | ☐ |
| TC-TNT-002 | Create with empty name — validation error | High | ☐ |
| TC-TNT-003 | Create with name > 200 chars — validation error | Medium | ☐ |
| TC-TNT-004 | Create with domain > 255 chars — validation error | Medium | ☐ |
| TC-TNT-005 | Create with duplicate name — verify behavior | High | ☐ |
| TC-TNT-006 | Update tenant name | Critical | ☐ |
| TC-TNT-007 | Update tenant domain | High | ☐ |
| TC-TNT-008 | Toggle active → inactive — admin access restricted | Critical | ☐ |
| TC-TNT-009 | Toggle inactive → active — access restored | Critical | ☐ |
| TC-TNT-010 | View tenant list — all tenants shown | Critical | ☐ |
| TC-TNT-011 | View tenant details | High | ☐ |
| TC-TNT-012 | Create tenant admin for tenant | Critical | ☐ |
| TC-TNT-013 | Create tenant admin with empty firstName — error | High | ☐ |
| TC-TNT-014 | Create tenant admin with invalid email — error | High | ☐ |
| TC-TNT-015 | Create tenant admin with password < 8 chars — error | High | ☐ |
| TC-TNT-016 | List tenant admins for specific tenant | High | ☐ |
| TC-TNT-017 | Deactivate tenant admin — cannot log in | Critical | ☐ |
| TC-TNT-018 | Tenant isolation — Tenant A admin cannot see Tenant B data | Critical | ☐ |
| TC-TNT-019 | Tenant admin cannot access /admin/tenants — redirected | Critical | ☐ |
| TC-TNT-020 | Student cannot access /admin/tenants — redirected | Critical | ☐ |
