-- Add application_id column to document table
-- Each document must belong to exactly one application

-- Add application_id column (required - NOT NULL)
ALTER TABLE document ADD COLUMN application_id BIGINT NOT NULL;

-- Add foreign key constraint to application table
ALTER TABLE document ADD CONSTRAINT fk_document_application
    FOREIGN KEY (application_id) REFERENCES application(id);

-- Add index for efficient queries by application
CREATE INDEX idx_document_application_id ON document(application_id);
