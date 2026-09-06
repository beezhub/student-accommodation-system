-- GH-140: room_allocation.academic_year_id was null on every row.
--
-- Neither of the two construction sites ever stamped it, so the waiting-list wait estimate, which
-- counts allocations made for the year, always counted zero. Zero is below its minimum sample size,
-- so the estimate returned null every time and the admin screen showed nothing.
--
-- The code now stamps it from the application. This fills in what already exists, so the estimate
-- has history to work from rather than starting empty.

UPDATE room_allocation ra
SET academic_year_id = a.academic_year_id
FROM application a
WHERE a.id = ra.application_id
  AND ra.academic_year_id IS NULL
  AND a.academic_year_id IS NOT NULL;

-- Allocations with no application keep a null year. There is nothing to derive one from, and
-- guessing the current year would inflate the velocity the estimate reads.
