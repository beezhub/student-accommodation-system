-- Add preferred_building_id column to application table
ALTER TABLE application ADD COLUMN preferred_building_id INT;

-- Add foreign key constraint for preferred_building_id
ALTER TABLE application
ADD CONSTRAINT fk_application_preferred_building
FOREIGN KEY (preferred_building_id) REFERENCES building(id) ON DELETE SET NULL;

-- Create index for preferred_building_id lookups
CREATE INDEX idx_application_preferred_building_id ON application(preferred_building_id);

-- Create allocation_method enum type
CREATE TYPE allocation_method_enum AS ENUM ('AUTO', 'MANUAL');

-- Add allocation_method column to application table
ALTER TABLE application ADD COLUMN allocation_method allocation_method_enum DEFAULT 'MANUAL';

-- Create index for allocation_method lookups
CREATE INDEX idx_application_allocation_method ON application(allocation_method);
