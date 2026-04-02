-- Document pack: enforce one document slot per (student_id, document_type_id).
--
-- Step 1: Deduplicate — for each (student_id, document_type_id) pair that has
--   more than one row, keep the newest (latest updated_at, tie-break on id DESC)
--   and delete the older ones. This prepares the table for the UNIQUE constraint.
--
-- Step 2: Add the unique constraint.

-- Step 1: Delete duplicate document rows, keeping newest per (student_id, document_type_id)
DELETE FROM document
WHERE id NOT IN (
    SELECT DISTINCT ON (student_id, document_type_id) id
    FROM document
    ORDER BY student_id, document_type_id, updated_at DESC, id DESC
);

-- Step 2: Add unique constraint — one document slot per student per type
ALTER TABLE document
    ADD CONSTRAINT uq_document_student_type UNIQUE (student_id, document_type_id);
