-- Add file_size column to document table
-- This column stores the file size in bytes for uploaded documents

ALTER TABLE document ADD COLUMN file_size BIGINT;

-- Add a comment to the column for documentation
COMMENT ON COLUMN document.file_size IS 'File size in bytes of the uploaded document';
