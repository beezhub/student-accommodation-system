-- Add optional owner_id FK to building table linking to building_owner profile.
-- Nullable: existing buildings are not yet assigned to an owner.

ALTER TABLE building
    ADD COLUMN IF NOT EXISTS owner_id BIGINT REFERENCES building_owner(id);

CREATE INDEX IF NOT EXISTS idx_building_owner ON building(owner_id);
