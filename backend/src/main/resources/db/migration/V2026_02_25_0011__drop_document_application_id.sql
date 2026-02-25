-- TASK-2.01: Decouple Documents from Applications - Step 5 (DESTRUCTIVE)
-- Drop the application_id column from document.
--
-- PREREQUISITE: Deploy ONLY after:
--   1. V2026_02_25_0010__decouple_document_from_application.sql has run
--   2. All code paths have been updated to use application_document junction table
--   3. DocumentLifecycleService no longer references application_id
--   4. All existing tests pass with the new code
--
-- This migration is intentionally separate to allow a safe deployment window.

ALTER TABLE document DROP COLUMN IF EXISTS application_id;
