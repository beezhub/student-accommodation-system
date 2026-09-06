-- GH-146: rows created by a platform admin were stamped with the platform tenant.
--
-- Only one of the five construction sites for tenant-owned allocation rows set tenant_id
-- explicitly. The rest left it null, so TenantListener copied whatever the acting context held,
-- which is 0 for a platform admin and also 0 for a student, since both carry the platform tenant in
-- their token. The owning tenant then could not see those rows through any filtered query: a
-- confirmed allocation left the student WAITING, the already-allocated guard did not fire, and a
-- tenant admin was refused ownership of their own proposal.
--
-- Each statement is a no-op where the parent is itself tenant 0, so nothing is mis-assigned.

UPDATE room_allocation ra SET tenant_id = r.tenant_id FROM room r
 WHERE r.id = ra.room_id AND ra.tenant_id = 0 AND r.tenant_id <> 0;

UPDATE staged_allocations sa SET tenant_id = r.tenant_id FROM room r
 WHERE r.id = sa.room_id AND sa.tenant_id = 0 AND r.tenant_id <> 0;

UPDATE waiting_list_entry w SET tenant_id = a.tenant_id FROM application a
 WHERE a.id = w.application_id AND w.tenant_id = 0 AND a.tenant_id <> 0;

-- Rooms are the asset every allocation derives its tenant from, so a room stamped 0 propagates the
-- defect to everything built on it. Buildings have no parent to derive from and are left alone.
UPDATE room rm SET tenant_id = b.tenant_id FROM building b
 WHERE b.id = rm.building_id AND rm.tenant_id = 0 AND b.tenant_id <> 0;

-- Deliberately NOT repaired here: app_user rows for TENANT_ADMIN and BUILDING_STAFF that were
-- stamped 0 by the same defect. Their owning tenant cannot be derived from any column, and 0 is the
-- sentinel that disables the tenant filter, so those accounts still have platform-wide read access.
-- Guessing would be worse than leaving them visible. List them with:
--   SELECT id, email, user_role FROM app_user
--    WHERE tenant_id = 0 AND user_role IN ('TENANT_ADMIN', 'BUILDING_STAFF', 'ADMIN');
