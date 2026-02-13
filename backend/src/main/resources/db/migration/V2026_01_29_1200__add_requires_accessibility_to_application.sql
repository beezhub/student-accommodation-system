-- Add requires_accessibility column to application table
ALTER TABLE application ADD COLUMN requires_accessibility BOOLEAN DEFAULT FALSE;

-- Migrate existing data: set requiresAccessibility=true for applications with preferredRoomType='ACCESSIBLE'
UPDATE application SET requires_accessibility = TRUE
WHERE preferred_room_type = 'ACCESSIBLE';

-- Note: We keep preferred_room_type column for now to avoid data loss
-- It can be dropped in a future migration after verification
