-- TASK-1.02: Fix room.room_number uniqueness scope from global to per-building
-- The global unique constraint allows only one room numbered "101" across the entire
-- platform. This migration replaces it with a per-building composite constraint so
-- different buildings can each have a room numbered "101".

-- Step 1: Drop the global unique constraint
ALTER TABLE room DROP CONSTRAINT IF EXISTS room_room_number_key;

-- Step 2: Add the per-building composite unique constraint
ALTER TABLE room ADD CONSTRAINT uq_room_number_building UNIQUE (building_id, room_number);

-- Step 3: Ensure room_number is unique for unassigned rooms (building_id IS NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_room_unassigned_room_number
    ON room (room_number)
    WHERE building_id IS NULL;
