## 1. Database Migration

- [x] 1.1 Write Flyway migration to delete `auto_allocation_enabled` row where `tenant_id != 0` and re-insert under `tenant_id = 0` with correct `ON CONFLICT (tenant_id, setting_key)` target

## 2. Service Layer — Cascade Logic

- [x] 2.1 Add `getPlatformSetting(String key)` method — reads `tenant_id = 0` row by disabling Hibernate tenant filter on EntityManager, then re-enabling in `finally`
- [x] 2.2 Modify `getSetting(String key)` — call existing tenant-scoped lookup first; on null result call `getPlatformSetting(key)` as fallback
- [x] 2.3 Add `getAllPlatformSettings()` method — returns all rows for `tenant_id = 0` as `Map<String, String>` (filter bypassed)
- [x] 2.4 Modify `getAllSettings()` — fetch platform map first, then overlay current tenant's rows on top to produce merged map
- [x] 2.5 Add `updatePlatformSetting(String key, String value)` — upsert for `tenant_id = 0` (filter bypassed, explicit tenant_id set)
- [x] 2.6 Add `deleteSetting(String key)` — remove current tenant's override row; throw `ResourceNotFoundException` if no row found for that tenant+key

## 3. Backend — Platform Controller

- [x] 3.1 Create `PlatformSettingController` at `/api/platform/system-settings` with `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`
- [x] 3.2 Add `GET /api/platform/system-settings` → `getAllPlatformSettings()`
- [x] 3.3 Add `PUT /api/platform/system-settings/{key}` → `updatePlatformSetting(key, value)`

## 4. Backend — Admin Controller Updates

- [x] 4.1 Change `@PreAuthorize` on `SystemSettingController` from `PLATFORM_ADMIN` to `hasAnyRole('ADMIN', 'PLATFORM_ADMIN')`
- [x] 4.2 Add `DELETE /api/admin/system-settings/{key}` endpoint → `deleteSetting(key)`, returns `204` on success and `404` when no override exists

## 5. Frontend — Service & Component Updates

- [x] 5.1 Add `deleteSetting(key: string): Observable<void>` to `SystemSettingsService` calling `DELETE /api/admin/system-settings/{key}`
- [x] 5.2 Update the auto-allocation toggle in `SystemSettingsComponent` — for `ADMIN` role, make the toggle read-only (display effective cascade value but disable edit, since `auto_allocation_enabled` is a platform setting)
- [x] 5.3 Verify that the existing `GET /api/admin/system-settings` call in `SystemSettingsComponent` now returns the cascade-merged values and displays correctly
