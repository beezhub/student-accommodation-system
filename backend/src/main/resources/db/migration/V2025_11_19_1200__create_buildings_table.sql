-- Create Building table
CREATE TABLE building (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    address VARCHAR(255),
    description TEXT,
    total_floors INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Migrate existing building data from room table to building table
INSERT INTO building (name)
SELECT DISTINCT building
FROM room
WHERE building IS NOT NULL
ON CONFLICT (name) DO NOTHING;

-- Add building_id column to room table
ALTER TABLE room ADD COLUMN building_id INT;

-- Update room table to reference the building table
UPDATE room
SET building_id = building.id
FROM building
WHERE room.building = building.name;

-- Add foreign key constraint
ALTER TABLE room
ADD CONSTRAINT fk_room_building
FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE SET NULL;

-- Drop the old building column (optional - comment out if you want to keep it temporarily)
ALTER TABLE room DROP COLUMN building;