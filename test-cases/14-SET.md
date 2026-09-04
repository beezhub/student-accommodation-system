# System Settings — UAT Test Cases

> **Module:** System Settings (Cascade Model)  
> **Routes:** `/admin/settings` (Frontend), `/api/platform/system-settings` & `/api/admin/system-settings` (Backend)  
> **Guard:** PlatformAdminGuard (platform routes), AdminGuard (tenant routes)  
> **Total Test Cases:** 15

---

### TC-SET-001: Platform admin views all platform settings

| Field | Value |
|-------|-------|
| **ID** | TC-SET-001 |
| **Module** | System Settings |
| **Priority** | Critical |
| **Preconditions** | Platform admin is logged in; platform default settings exist (tenant_id=0) |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin | Login succeeds; redirected to admin dashboard |
| 2 | Navigate to `/admin/settings` | System Settings page loads without errors |
| 3 | Observe the settings list | All platform-level settings (tenant_id=0) are displayed with their current key-value pairs |
| 4 | Verify `auto_allocation_enabled` is present | Setting is listed and editable for platform admin |

| Field | Value |
|-------|-------|
| **Postconditions** | Settings page displayed with all platform defaults |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-002: Platform admin updates a platform setting value

| Field | Value |
|-------|-------|
| **ID** | TC-SET-002 |
| **Module** | System Settings |
| **Priority** | Critical |
| **Preconditions** | Platform admin logged in; at least one platform setting exists |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; pick a non-critical setting to update (e.g., `offer_expiry_days`) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads |
| 2 | Locate a platform setting (e.g., `offer_expiry_days`) | Setting is displayed with its current value |
| 3 | Change the value (e.g., from `7` to `14`) | Input field accepts the new value |
| 4 | Click Save / Submit | Success snackbar appears: "Setting updated successfully" |
| 5 | Refresh the page | The updated value (`14`) persists |
| 6 | Verify via API: `GET /api/platform/system-settings` | Response includes the updated key-value pair |

| Field | Value |
|-------|-------|
| **Postconditions** | Platform setting value is updated in the database (tenant_id=0) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-003: Tenant admin views merged settings (platform + tenant overrides)

| Field | Value |
|-------|-------|
| **ID** | TC-SET-003 |
| **Module** | System Settings |
| **Priority** | Critical |
| **Preconditions** | Tenant admin logged in; platform defaults exist; at least one tenant override exists for this tenant |
| **Test Data** | Login: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenant admin | Login succeeds |
| 2 | Navigate to `/admin/settings` | System Settings page loads |
| 3 | Observe the settings list | A merged view is displayed: platform defaults are shown for settings without tenant overrides, and tenant-specific values are shown where overrides exist |
| 4 | Identify a setting that has a tenant override | The overridden value is displayed, not the platform default |
| 5 | Identify a setting that has no tenant override | The platform default value is displayed |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin sees the correct merged view of settings |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-004: Tenant admin overrides a setting

| Field | Value |
|-------|-------|
| **ID** | TC-SET-004 |
| **Module** | System Settings |
| **Priority** | Critical |
| **Preconditions** | Tenant admin logged in; the setting to override currently shows the platform default |
| **Test Data** | Login: tenantadmin@test.com / Admin@12345; choose a setting that currently has no tenant override (e.g., `offer_expiry_days` showing platform default `7`) |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads with merged view |
| 2 | Locate a setting currently showing the platform default value | Setting displays platform default (e.g., `7`) |
| 3 | Change the value to a tenant-specific value (e.g., `10`) | Input accepts the new value |
| 4 | Click Save | Success snackbar: "Setting updated successfully" |
| 5 | Refresh the page | The setting now shows the tenant override value (`10`) |
| 6 | Verify via API: `GET /api/admin/system-settings` | Response includes the tenant-specific override |

| Field | Value |
|-------|-------|
| **Postconditions** | A new row is created in `system_setting` with the tenant's tenant_id and the overridden value |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-005: Tenant admin deletes override → falls back to platform default

| Field | Value |
|-------|-------|
| **ID** | TC-SET-005 |
| **Module** | System Settings |
| **Priority** | Critical |
| **Preconditions** | Tenant admin logged in; at least one tenant override exists |
| **Test Data** | Login: tenantadmin@test.com / Admin@12345; a setting with an existing tenant override |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads |
| 2 | Locate a setting with a tenant override | Override value is displayed |
| 3 | Click the delete/reset button for the override | Confirmation dialog appears (if applicable) |
| 4 | Confirm the deletion | Success snackbar: "Setting override removed" or similar |
| 5 | Observe the setting value | The value now shows the platform default |
| 6 | Verify via API: `DELETE /api/admin/system-settings/{key}` was called | The tenant override row is removed from the database |
| 7 | Refresh the page | Platform default value is still shown |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant override row deleted; setting falls back to platform default (tenant_id=0) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-006: Verify auto_allocation_enabled is read-only for tenant admin

| Field | Value |
|-------|-------|
| **ID** | TC-SET-006 |
| **Module** | System Settings |
| **Priority** | High |
| **Preconditions** | Tenant admin logged in; `auto_allocation_enabled` exists as a platform setting |
| **Test Data** | Login: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads |
| 2 | Locate the `auto_allocation_enabled` setting | Setting is displayed with its current value |
| 3 | Attempt to edit the value | The input field is disabled/read-only; no edit control is available |
| 4 | Attempt via API: `PUT /api/admin/system-settings` with `auto_allocation_enabled` | API returns 403 Forbidden or ignores the update |

| Field | Value |
|-------|-------|
| **Postconditions** | `auto_allocation_enabled` remains unchanged; tenant admin cannot modify it |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-007: Platform admin can update auto_allocation_enabled

| Field | Value |
|-------|-------|
| **ID** | TC-SET-007 |
| **Module** | System Settings |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; `auto_allocation_enabled` setting exists |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads |
| 2 | Locate `auto_allocation_enabled` | Setting is displayed and editable for platform admin |
| 3 | Toggle the value (e.g., from `true` to `false`) | Input accepts the change |
| 4 | Click Save | Success snackbar appears |
| 5 | Refresh the page | Updated value persists |
| 6 | Verify via API: `GET /api/platform/system-settings` | `auto_allocation_enabled` reflects the new value |

| Field | Value |
|-------|-------|
| **Postconditions** | `auto_allocation_enabled` is updated at platform level (tenant_id=0) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-008: Update setting with empty value

| Field | Value |
|-------|-------|
| **ID** | TC-SET-008 |
| **Module** | System Settings |
| **Priority** | Medium |
| **Preconditions** | Platform admin logged in; a setting with a non-empty value exists |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads |
| 2 | Locate a setting with a value | Current value is displayed |
| 3 | Clear the value field (set to empty string) | Field is empty |
| 4 | Click Save | Either: (a) validation error prevents saving with "Value is required" message, OR (b) empty value is accepted and saved |
| 5 | If saved, refresh the page | Verify the behavior is consistent — empty value either persists or reverts |

| Field | Value |
|-------|-------|
| **Postconditions** | Behavior documented: system either rejects or accepts empty setting values |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | Document whether empty values are accepted or rejected |

---

### TC-SET-009: Verify cascade — platform default shows when no tenant override exists

| Field | Value |
|-------|-------|
| **ID** | TC-SET-009 |
| **Module** | System Settings |
| **Priority** | Critical |
| **Preconditions** | Platform admin has set a platform default for a key; no tenant override exists for that key for the test tenant |
| **Test Data** | Platform admin: platformadmin@test.com / Admin@12345; Tenant admin: tenantadmin@test.com / Admin@12345; Setting key: e.g., `batch_interval_ms` with platform value `21600000` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin | Login succeeds |
| 2 | Navigate to `/admin/settings` and set `batch_interval_ms` to `21600000` | Setting saved at platform level |
| 3 | Log out and log in as tenant admin | Login succeeds |
| 4 | Navigate to `/admin/settings` | Settings page loads |
| 5 | Locate `batch_interval_ms` | Value shows `21600000` (the platform default) |
| 6 | Verify via API: `GET /api/admin/system-settings` | Response includes `batch_interval_ms` with the platform default value |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin sees platform defaults for settings without overrides |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-010: Verify cascade — tenant override takes precedence over platform default

| Field | Value |
|-------|-------|
| **ID** | TC-SET-010 |
| **Module** | System Settings |
| **Priority** | Critical |
| **Preconditions** | A platform default exists for a key; a tenant override also exists for the same key with a different value |
| **Test Data** | Platform admin: platformadmin@test.com / Admin@12345; Tenant admin: tenantadmin@test.com / Admin@12345; Setting: `offer_expiry_days`, platform default `7`, tenant override `14` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as platform admin and verify `offer_expiry_days` = `7` at platform level | Platform setting confirmed |
| 2 | Log out, log in as tenant admin | Login succeeds |
| 3 | Navigate to `/admin/settings` | Settings page loads |
| 4 | Set `offer_expiry_days` to `14` (creating a tenant override) | Save succeeds |
| 5 | Refresh the page | `offer_expiry_days` shows `14` (tenant override), not `7` (platform default) |
| 6 | Verify via API: `GET /api/admin/system-settings` | Response shows tenant override value `14` for this key |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant override (`14`) takes precedence over platform default (`7`) |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-011: Create a new setting key from platform admin

| Field | Value |
|-------|-------|
| **ID** | TC-SET-011 |
| **Module** | System Settings |
| **Priority** | Medium |
| **Preconditions** | Platform admin logged in |
| **Test Data** | Login: platformadmin@test.com / Admin@12345; New setting key: `max_applications_per_student`, value: `3` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads |
| 2 | Look for an "Add Setting" button or form | Add mechanism is available (if supported by the UI) |
| 3 | Enter key: `max_applications_per_student`, value: `3` | Fields accept input |
| 4 | Click Save / Add | Success snackbar appears |
| 5 | Refresh the page | New setting appears in the list with value `3` |
| 6 | Log out, log in as tenant admin and navigate to settings | New setting is visible with platform default value `3` |

| Field | Value |
|-------|-------|
| **Postconditions** | New platform setting exists and is visible to tenant admins via cascade |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | If UI does not support adding new keys, document this and test via API only |

---

### TC-SET-012: Tenant admin cannot access /api/platform/system-settings

| Field | Value |
|-------|-------|
| **ID** | TC-SET-012 |
| **Module** | System Settings |
| **Priority** | High |
| **Preconditions** | Tenant admin logged in |
| **Test Data** | Login: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenant admin | Login succeeds |
| 2 | Open browser developer tools → Network tab | Network tab is ready |
| 3 | Make a direct API call: `GET /api/platform/system-settings` using tenant admin's JWT token | Response: 403 Forbidden |
| 4 | Make a direct API call: `PUT /api/platform/system-settings` using tenant admin's JWT token | Response: 403 Forbidden |

| Field | Value |
|-------|-------|
| **Postconditions** | Tenant admin is denied access to platform-level settings API |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-013: Student cannot access /admin/settings

| Field | Value |
|-------|-------|
| **ID** | TC-SET-013 |
| **Module** | System Settings |
| **Priority** | High |
| **Preconditions** | A student user account exists and is logged in |
| **Test Data** | A registered student account with valid credentials |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as a student user | Login succeeds; redirected to student dashboard |
| 2 | Manually navigate to `http://localhost:4200/admin/settings` | User is redirected away from the settings page (to student dashboard or login) |
| 3 | Verify the settings page content is NOT displayed | No settings data is visible; access is denied by the guard |

| Field | Value |
|-------|-------|
| **Postconditions** | Student cannot access admin settings; route guard redirects |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-014: Tenant admin cannot delete platform settings

| Field | Value |
|-------|-------|
| **ID** | TC-SET-014 |
| **Module** | System Settings |
| **Priority** | High |
| **Preconditions** | Tenant admin logged in; platform settings exist |
| **Test Data** | Login: tenantadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Log in as tenant admin | Login succeeds |
| 2 | Navigate to `/admin/settings` | Settings page loads |
| 3 | Attempt to delete a platform-level setting (not a tenant override) | Delete button is either not shown for platform settings, or action is blocked |
| 4 | Attempt via API: `DELETE /api/platform/system-settings/{key}` with tenant admin JWT | Response: 403 Forbidden |
| 5 | Verify the platform setting still exists via `GET /api/admin/system-settings` | Platform default value is still present |

| Field | Value |
|-------|-------|
| **Postconditions** | Platform settings remain intact; tenant admin cannot delete them |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |

---

### TC-SET-015: Settings changes take effect immediately (no restart needed)

| Field | Value |
|-------|-------|
| **ID** | TC-SET-015 |
| **Module** | System Settings |
| **Priority** | High |
| **Preconditions** | Platform admin logged in; application is running; a setting that has observable behavior is identified (e.g., `offer_expiry_days`) |
| **Test Data** | Login: platformadmin@test.com / Admin@12345 |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/admin/settings` | Settings page loads |
| 2 | Note the current value of `offer_expiry_days` (e.g., `7`) | Value recorded |
| 3 | Change the value to `14` and save | Success snackbar appears |
| 4 | Without restarting the application, perform an action that uses `offer_expiry_days` (e.g., create a new allocation offer) | The new offer uses the updated value (`14` days expiry) |
| 5 | Verify the offer expiry date is calculated using the new setting | Offer expiry = allocation date + 14 days |

| Field | Value |
|-------|-------|
| **Postconditions** | Setting change is immediately effective without application restart |
| **Pass/Fail** | ☐ |
| **Tester** | _________________ |
| **Date** | _________________ |
| **Notes** | |
