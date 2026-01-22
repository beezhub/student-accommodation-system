-- Create gender_restriction enum type
CREATE TYPE gender_restriction_enum AS ENUM ('MALE_ONLY', 'FEMALE_ONLY', 'ANY');

-- Create building_attributes table for building-level restrictions and metadata
CREATE TABLE building_attributes (
    id SERIAL PRIMARY KEY,
    building_id INT NOT NULL UNIQUE,
    gender_restriction gender_restriction_enum DEFAULT 'ANY',
    accessibility_features JSONB,
    desirability_score INT CHECK (desirability_score >= 1 AND desirability_score <= 10),
    allow_mixed_gender BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE
);

-- Create index for building_attributes
CREATE INDEX idx_building_attributes_building_id ON building_attributes(building_id);
CREATE INDEX idx_building_attributes_gender_restriction ON building_attributes(gender_restriction);

-- Create room_attributes table for room-level overrides and restrictions
CREATE TABLE room_attributes (
    id SERIAL PRIMARY KEY,
    room_id INT NOT NULL UNIQUE,
    accessibility_compliant BOOLEAN DEFAULT FALSE,
    gender_restriction gender_restriction_enum,
    desirability_override INT CHECK (desirability_override IS NULL OR (desirability_override >= 1 AND desirability_override <= 10)),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE
);

-- Create index for room_attributes
CREATE INDEX idx_room_attributes_room_id ON room_attributes(room_id);
CREATE INDEX idx_room_attributes_accessibility ON room_attributes(accessibility_compliant);
