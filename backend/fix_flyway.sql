-- Remove the failed migration entry
DELETE FROM flyway_schema_history
WHERE version = '2026.02.06.1000' AND success = false;

-- Verify the deletion
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
