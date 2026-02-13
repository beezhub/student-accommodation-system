CREATE TABLE building_image (
    id              BIGSERIAL PRIMARY KEY,
    building_id     BIGINT NOT NULL REFERENCES building(id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,
    object_path     VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100),
    file_size       BIGINT,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_building_image_building_id ON building_image(building_id);
CREATE UNIQUE INDEX idx_building_image_primary ON building_image(building_id) WHERE is_primary = TRUE;
