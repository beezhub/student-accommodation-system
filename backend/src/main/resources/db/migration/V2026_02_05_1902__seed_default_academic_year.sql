-- Seed default academic year for 2026
INSERT INTO academic_year (
    name, 
    start_date, 
    end_date, 
    application_open_date, 
    application_close_date, 
    is_current
) VALUES (
    '2026',
    '2026-02-01',   -- Academic year starts Feb 2026
    '2026-11-30',   -- Academic year ends Nov 2026
    '2025-09-01',   -- Applications open Sep 2025
    '2026-01-31',   -- Applications close Jan 2026
    TRUE
);

-- Link existing applications to the default academic year
UPDATE application 
SET academic_year_id = (SELECT id FROM academic_year WHERE name = '2026')
WHERE academic_year_id IS NULL;

-- Link existing room allocations to the default academic year
UPDATE room_allocation 
SET academic_year_id = (SELECT id FROM academic_year WHERE name = '2026')
WHERE academic_year_id IS NULL;
