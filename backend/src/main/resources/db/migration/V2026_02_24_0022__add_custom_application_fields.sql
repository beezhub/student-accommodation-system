-- TASK-2.07: Add custom_application_field and custom_field_answer tables
-- Allows building owners to define per-building custom application fields.
-- Students provide answers when submitting applications.

CREATE TABLE IF NOT EXISTS custom_application_field (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    field_label VARCHAR(200) NOT NULL,
    field_type VARCHAR(20) NOT NULL CHECK (field_type IN ('TEXT', 'NUMBER', 'DROPDOWN', 'CHECKBOX', 'DATE')),
    is_required BOOLEAN NOT NULL DEFAULT false,
    help_text VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    options_json TEXT,   -- JSON array for DROPDOWN choices, e.g. ["Option A","Option B"]
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_custom_field_building ON custom_application_field(building_id);

CREATE TABLE IF NOT EXISTS custom_field_answer (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES application(id),
    field_id BIGINT NOT NULL REFERENCES custom_application_field(id),
    answer_value TEXT,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_field_answer UNIQUE (application_id, field_id)
);

CREATE INDEX IF NOT EXISTS idx_custom_answer_application ON custom_field_answer(application_id);
