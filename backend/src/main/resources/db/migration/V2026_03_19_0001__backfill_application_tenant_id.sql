-- Backfill tenant_id on applications that were submitted with tenant_id = 0
-- (platform sentinel) before the per-tenant assignment rule was enforced.
--
-- Strategy: set tenant_id from the building with the lowest preference_order
-- in application_building_preference. Applications with no resolvable building
-- preference are left at tenant_id = 0 (visible to PLATFORM_ADMIN only).

UPDATE application a
SET tenant_id = (
    SELECT b.tenant_id
    FROM application_building_preference abp
    JOIN building b ON b.id = abp.building_id
    WHERE abp.application_id = a.id
      AND b.tenant_id IS NOT NULL
      AND b.tenant_id <> 0
    ORDER BY abp.preference_order ASC
    LIMIT 1
)
WHERE a.tenant_id = 0
  AND EXISTS (
      SELECT 1
      FROM application_building_preference abp
      JOIN building b ON b.id = abp.building_id
      WHERE abp.application_id = a.id
        AND b.tenant_id IS NOT NULL
        AND b.tenant_id <> 0
  );
