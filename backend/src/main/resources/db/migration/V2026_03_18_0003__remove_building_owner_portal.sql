-- Remove building owner portal: drop all owner-related tables and columns.
-- Multi-tenancy via PLATFORM_ADMIN covers building management; the owner portal is redundant.

-- Step 1: Drop building_staff (FK -> building_owner)
DROP TABLE IF EXISTS building_staff;

-- Step 2: Drop the owner_id FK column from building
ALTER TABLE building DROP COLUMN IF EXISTS owner_id;

-- Step 3: Drop building_owner (FK -> app_user, tenant) before deleting app_user rows
DROP TABLE IF EXISTS building_owner;

-- Step 4: Delete orphaned app_user rows now that no FK references them
DELETE FROM app_user WHERE user_role IN ('BUILDING_OWNER', 'LANDLORD');

-- Step 5: Drop owner_registration (independent pre-approval records)
DROP TABLE IF EXISTS owner_registration;
