-- Make student_number nullable to support deferred profile completion.
-- Students will provide their student number during application submission, not profile setup.
ALTER TABLE student ALTER COLUMN student_number DROP NOT NULL;

-- Drop the inline UNIQUE constraint (this also drops its backing index).
-- The constraint must be dropped before the index: PostgreSQL refuses to drop
-- an index that is owned by a constraint.
ALTER TABLE student DROP CONSTRAINT IF EXISTS student_student_number_key;
DROP INDEX IF EXISTS student_student_number_key;

-- Re-add uniqueness excluding NULLs (explicit partial index for clarity).
CREATE UNIQUE INDEX student_student_number_unique ON student (student_number) WHERE student_number IS NOT NULL;
