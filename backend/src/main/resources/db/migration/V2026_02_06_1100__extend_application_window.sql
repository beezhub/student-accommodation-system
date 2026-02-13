-- Extend application window close date to allow submissions
UPDATE academic_year
SET application_close_date = '2026-06-30'
WHERE name = '2026' AND is_current = TRUE;
