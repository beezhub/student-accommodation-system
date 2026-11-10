-- GH-495 step 3: put every application and allocation on a year and intake of its own company.
--
-- Until group A, submitting attached the one platform-wide current year, so applications and
-- allocations at other companies point at another company's year (usually the platform's or the
-- Default Tenant's). Group A then fell back to platform (tenant 0) years for a company with none of
-- its own. From group B a company takes applications only for its own years.
--
-- Nothing is deleted and no existing year or intake is changed: years and intakes are copied into
-- the company that needs them and rows are repointed. Idempotent: run again, it finds nothing left
-- to copy or repoint. The NOT NULL constraints on application.academic_year_id and intake_id come
-- in group C, a later deploy.

-- ── 1. Which years each company needs a copy of ────────────────────────────────────────────────
--
-- (a) years of another company that this company's applications point at;
-- (b) the same for allocations;
-- (c) platform years that have not ended, for a company with no year of its own, so a company that
--     relied on the platform fallback keeps taking applications.
CREATE TEMPORARY TABLE year_copy_pair ON COMMIT DROP AS
SELECT DISTINCT a.academic_year_id AS source_year_id, a.tenant_id AS target_tenant_id
FROM application a
JOIN academic_year y ON y.id = a.academic_year_id
WHERE y.tenant_id <> a.tenant_id
UNION
SELECT DISTINCT ra.academic_year_id, ra.tenant_id
FROM room_allocation ra
JOIN academic_year y ON y.id = ra.academic_year_id
WHERE y.tenant_id <> ra.tenant_id
UNION
SELECT y.id, t.id
FROM academic_year y
CROSS JOIN tenant t
WHERE y.tenant_id = 0
  AND y.end_date >= CURRENT_DATE
  AND t.id <> 0
  AND NOT EXISTS (SELECT 1 FROM academic_year own WHERE own.tenant_id = t.id);

-- ── 2. Copy each year into the company: same name, institution and dates ──────────────────────
--
-- A company that already has a year with the same name and institution keeps it, and its rows are
-- pointed there instead (ON CONFLICT on uq_academic_year_tenant_institution_name). When two source
-- years would give the same copy, the lowest id is the one copied.
CREATE TEMPORARY TABLE year_copy_created (id BIGINT PRIMARY KEY, source_year_id BIGINT NOT NULL) ON COMMIT DROP;

WITH source AS (
    SELECT DISTINCT ON (p.target_tenant_id, y.institution_id, y.name)
           y.id AS source_year_id, p.target_tenant_id, y.institution_id, y.name, y.start_date, y.end_date,
           y.application_open_date, y.application_close_date
    FROM year_copy_pair p
    JOIN academic_year y ON y.id = p.source_year_id
    ORDER BY p.target_tenant_id, y.institution_id, y.name, y.id
), inserted AS (
    INSERT INTO academic_year (name, start_date, end_date, application_open_date, application_close_date,
                               is_current, tenant_id, institution_id, created_at, updated_at)
    SELECT s.name, s.start_date, s.end_date, s.application_open_date, s.application_close_date,
           FALSE, s.target_tenant_id, s.institution_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM source s
    ON CONFLICT ON CONSTRAINT uq_academic_year_tenant_institution_name DO NOTHING
    RETURNING id, tenant_id, institution_id, name
)
INSERT INTO year_copy_created (id, source_year_id)
SELECT i.id, s.source_year_id
FROM inserted i
JOIN source s ON s.target_tenant_id = i.tenant_id
             AND s.name = i.name
             AND s.institution_id IS NOT DISTINCT FROM i.institution_id;

-- Each pair resolves to the company's year with that name and institution: the copy just made, or
-- the one it already had.
CREATE TEMPORARY TABLE year_copy_map ON COMMIT DROP AS
SELECT p.source_year_id, p.target_tenant_id, t.id AS target_year_id
FROM year_copy_pair p
JOIN academic_year s ON s.id = p.source_year_id
JOIN academic_year t ON t.tenant_id = p.target_tenant_id
                    AND t.name = s.name
                    AND t.institution_id IS NOT DISTINCT FROM s.institution_id;

-- ── 3. Intakes of each new copy: the source year's intakes, or an Annual one ──────────────────
INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                    application_open_date, application_close_date)
SELECT y.tenant_id, c.id, i.intake_type, i.name, i.stay_start_date, i.stay_end_date,
       i.application_open_date, i.application_close_date
FROM year_copy_created c
JOIN academic_year y ON y.id = c.id
JOIN intake i ON i.academic_year_id = c.source_year_id;

-- A source year with no intake (one that ends before it starts got none in group A) gives an Annual
-- intake from the year's own dates, as the group A backfill did.
INSERT INTO intake (tenant_id, academic_year_id, intake_type, name, stay_start_date, stay_end_date,
                    application_open_date, application_close_date)
SELECT y.tenant_id, y.id, 'ANNUAL', 'Annual', y.start_date, y.end_date,
       LEAST(y.application_open_date, y.application_close_date),
       GREATEST(y.application_open_date, y.application_close_date)
FROM year_copy_created c
JOIN academic_year y ON y.id = c.id
WHERE y.start_date < y.end_date
  AND NOT EXISTS (SELECT 1 FROM intake i WHERE i.academic_year_id = y.id);

-- ── 4. Repoint rows from another company's year to their own company's copy ───────────────────
UPDATE application a
SET academic_year_id = m.target_year_id
FROM year_copy_map m
WHERE a.academic_year_id = m.source_year_id
  AND a.tenant_id = m.target_tenant_id;

UPDATE room_allocation ra
SET academic_year_id = m.target_year_id
FROM year_copy_map m
WHERE ra.academic_year_id = m.source_year_id
  AND ra.tenant_id = m.target_tenant_id;

-- ── 5. Applications with no year: the company's year containing the application date ──────────
--
-- Preferring a year that applies to the student's institution, then one containing the date, then
-- the nearest by date, then a specific institution's year over an "All institutions" one.
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

-- An allocation with no year takes its application's, as GH-140 did.
UPDATE room_allocation ra
SET academic_year_id = a.academic_year_id
FROM application a
WHERE a.id = ra.application_id
  AND ra.academic_year_id IS NULL
  AND a.academic_year_id IS NOT NULL;

-- ── 6. The intake: the year's Annual intake ────────────────────────────────────────────────────
--
-- A year set up with Semesters or Trimesters only has no Annual intake; there the application takes
-- the intake whose window contained the application date, else the first by move-in. No intake is
-- created in a year that already has some.
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

-- ── 7. The institution, from the student's profile ─────────────────────────────────────────────
UPDATE application a
SET institution_id = s.institution_id
FROM student s
WHERE s.id = a.student_id
  AND a.institution_id IS NULL
  AND s.institution_id IS NOT NULL;
