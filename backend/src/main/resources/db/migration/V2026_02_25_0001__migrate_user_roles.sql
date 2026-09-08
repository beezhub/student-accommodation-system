-- Migrate user roles to v2.0 role model
-- ADMIN -> PLATFORM_ADMIN (platform-level operator)
-- LANDLORD -> BUILDING_OWNER (building owner within a tenant)

UPDATE app_user
SET user_role = 'PLATFORM_ADMIN'
WHERE user_role = 'ADMIN';

UPDATE app_user
SET user_role = 'BUILDING_OWNER'
WHERE user_role = 'LANDLORD';
