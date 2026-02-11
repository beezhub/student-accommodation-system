-- Add geocoding fields to building table for Google Maps integration
ALTER TABLE building
ADD COLUMN latitude DECIMAL(10, 8),
ADD COLUMN longitude DECIMAL(11, 8),
ADD COLUMN place_id VARCHAR(255),
ADD COLUMN formatted_address VARCHAR(500),
ADD COLUMN address_components JSONB;

-- Create indexes for efficient geocoding queries
CREATE INDEX idx_building_coordinates ON building(latitude, longitude);
CREATE INDEX idx_building_place_id ON building(place_id);
