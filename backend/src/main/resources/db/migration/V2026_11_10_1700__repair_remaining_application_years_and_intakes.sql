-- GH-496: the backfill step before application.academic_year_id and intake_id become NOT NULL.
--
-- Step 3 (V2026_11_10_1300) filled both for existing rows. This repeats its rules for any row left
-- empty since, or that step 3 could not fill because the year had no intake yet. Nothing is
-- deleted. Idempotent: a second run finds nothing to fill.

-- 1. Applications with no year: the company's year containing the application date, preferring one
--    that applies to the student's institution, then the nearest by date (as step 3).
UPDATE application a
SET academic_year_id = (
        SELECT y.id
        FROM academic_year y
        LEFT JOIN student s ON s.id = a.student_id
        WHERE y.tenant_id = a.tenant_id
        ORDER BY
            CASE WHEN y.institution_id IS NULL OR y.institution_id = s.institution_id THEN 0 ELSE 1 END,
            CASE WHEN COALESCE(a.application_date, a.created_at::date, CURRENT_DATE)
                      BETWEEN y.start_date AND y.end_date THEN 0 ELSE 1 END,
            LEAST(ABS(y.start_date - COALESCE(a.application_date, a.created_at::date, CURRENT_DATE)),
                  ABS(y.end_date - COALESCE(a.application_date, a.created_at::date, CURRENT_DATE))),
            CASE WHEN y.institution_id IS NULL THEN 1 ELSE 0 END,
            y.id
        LIMIT 1)
WHERE a.academic_year_id IS NULL
  AND EXISTS (SELECT 1 FROM academic_year y WHERE y.tenant_id = a.tenant_id);

-- 2. A year an application points at that has no intake at all gets one Annual intake from its own
--    dates and window, exactly as the group A backfill (V2026_11_10_1101) gave every year then. A
--    year that ends before it starts gets none, rather than failing the deploy.
INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                    application_open_date, application_close_date)
SELECT y.tenant_id, y.id, 'ANNUAL', 'Annual', y.start_date, y.end_date,
       LEAST(y.application_open_date, y.application_close_date),
       GREATEST(y.application_open_date, y.application_close_date)
FROM academic_year y
WHERE y.start_date < y.end_date
  AND NOT EXISTS (SELECT 1 FROM intake i WHERE i.academic_year_id = y.id)
  AND EXISTS (SELECT 1 FROM application a WHERE a.academic_year_id = y.id AND a.intake_id IS NULL);

-- 3. Applications with no intake: the year's Annual intake, else the one whose window held the
--    application date, else the first by move-in (as step 3).
UPDATE application a
SET intake_id = (
        SELECT i.id
        FROM intake i
        WHERE i.academic_year_id = a.academic_year_id
        ORDER BY
            CASE WHEN i.intake_type = 'ANNUAL' THEN 0 ELSE 1 END,
            CASE WHEN COALESCE(a.application_date, a.created_at::date)
                      BETWEEN i.application_open_date AND i.application_close_date THEN 0 ELSE 1 END,
            i.stay_start_date,
            i.id
        LIMIT 1)
WHERE a.intake_id IS NULL
  AND a.academic_year_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM intake i WHERE i.academic_year_id = a.academic_year_id);

-- 4. Allocations with no planned stay whose application now has an intake take its dates (step 5).
UPDATE room_allocation ra
SET stay_start_date = i.stay_start_date,
    stay_end_date   = i.stay_end_date
FROM application a
JOIN intake i ON i.id = a.intake_id
WHERE a.id = ra.application_id
  AND ra.stay_start_date IS NULL;
