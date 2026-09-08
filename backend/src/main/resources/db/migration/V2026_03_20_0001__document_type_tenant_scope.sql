-- Add tenant_id to document_type for per-tenant document type configuration.
-- Existing rows are assigned to tenant_id = 0 (platform defaults).
-- Uniqueness changes from global type_name to (tenant_id, type_name).

ALTER TABLE document_type
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;

ALTER TABLE document_type
    ADD CONSTRAINT fk_document_type_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id);

ALTER TABLE document_type
    DROP CONSTRAINT IF EXISTS document_type_type_name_key;

ALTER TABLE document_type
    ADD CONSTRAINT uq_document_type_tenant_name UNIQUE (tenant_id, type_name);
