-- GH-494 step 1: one ANNUAL intake per existing academic year, with the year's own stay dates and
-- application window, so applying works exactly as before. Idempotent: a year that already has an
-- intake is skipped. LEAST/GREATEST keep a year whose window was saved back to front (nothing
-- validated it before) from failing the deploy; a year that ends before it starts gets no intake.
INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                    application_open_date, application_close_date)
SELECT y.tenant_id, y.id, 'ANNUAL', 'Annual', y.start_date, y.end_date,
       LEAST(y.application_open_date, y.application_close_date),
       GREATEST(y.application_open_date, y.application_close_date)
FROM academic_year y
WHERE y.start_date < y.end_date
  AND NOT EXISTS (SELECT 1 FROM intake i WHERE i.academic_year_id = y.id);
