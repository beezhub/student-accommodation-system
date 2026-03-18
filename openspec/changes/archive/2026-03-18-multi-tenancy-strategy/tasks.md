## 1. Backend — Tenant Controller

- [x] 1.1 Create `TenantController.java` with `GET /api/admin/tenants` endpoint returning all tenants
- [x] 1.2 Add `GET /api/admin/tenants/{id}` endpoint returning a single tenant by ID (404 if not found)
- [x] 1.3 Secure both endpoints with `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`

## 2. Security Configuration

- [x] 2.1 Add `/api/admin/tenants/**` to Spring Security config to permit `PLATFORM_ADMIN` role

## 3. Verification

- [x] 3.1 Verify `GET /api/admin/tenants` returns tenant list for platform admin and 403 for other roles
- [x] 3.2 Verify `GET /api/admin/tenants/{id}` returns correct tenant or 404 when not found
- [x] 3.3 Confirm tenant-selector dropdown in Create User dialog loads tenants correctly
