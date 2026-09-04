-- TASK-2.08: Add required_document_type table for per-building configurable document requirements.
-- Building owners define which documents students must upload when applying to their building.

CREATE TABLE IF NOT EXISTS required_document_type (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    document_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    is_required BOOLEAN NOT NULL DEFAULT true,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_required_doc_type_building ON required_document_type(building_id);
