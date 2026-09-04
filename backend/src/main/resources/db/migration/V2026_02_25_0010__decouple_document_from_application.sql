-- TASK-2.01: Decouple Documents from Applications
-- Step 1 (non-destructive): Make application_id nullable so documents can exist
--   without being tied to a specific application.
-- Step 2: Create application_document junction table and migrate existing data.
--
-- NOTE: Step 3 (drop application_id column) is in a separate migration file:
--   V2026_02_25_0011__drop_document_application_id.sql
--   Deploy ONLY after all code paths have been verified to use the junction table.

-- Step 1: Make application_id nullable (non-destructive)
ALTER TABLE document ALTER COLUMN application_id DROP NOT NULL;

-- Update FK constraint to use ON DELETE SET NULL so cancelling an application
-- does not orphan documents
ALTER TABLE document DROP CONSTRAINT IF EXISTS fk_document_application;
ALTER TABLE document ADD CONSTRAINT fk_document_application
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE SET NULL;

-- Step 2: Create the application_document junction table
CREATE TABLE IF NOT EXISTS application_document (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES application(id) ON DELETE CASCADE,
    document_id    BIGINT NOT NULL REFERENCES document(id)    ON DELETE CASCADE,
    attached_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_application_document UNIQUE (application_id, document_id)
);

CREATE INDEX IF NOT EXISTS idx_application_document_application ON application_document(application_id);
CREATE INDEX IF NOT EXISTS idx_application_document_document    ON application_document(document_id);

-- Migrate existing associations: every document that currently has an application_id
-- gets a corresponding row in the junction table.
INSERT INTO application_document (application_id, document_id, attached_at)
SELECT application_id, id, created_at
FROM document
WHERE application_id IS NOT NULL
ON CONFLICT (application_id, document_id) DO NOTHING;
