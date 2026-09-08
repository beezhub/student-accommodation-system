-- Remove the application_document junction table.
--
-- Documents are now managed at the student level (one slot per document type).
-- Application review reads documents directly via document.student_id.
-- The junction table was an intermediate migration aid and is no longer needed.

DROP TABLE IF EXISTS application_document CASCADE;
