-- GH-496 step 5: an allocation has planned stay dates, copied from the application's intake, so room
-- capacity can be counted by overlapping stays instead of every live allocation in any year.
--
-- end_date stays what it was: when the stay actually ended (check-out, or an offer or check-in
-- lapsing). stay_end_date is when the check-out is planned; the bed is free again from that date.
ALTER TABLE room_allocation ADD COLUMN stay_start_date DATE;
ALTER TABLE room_allocation ADD COLUMN stay_end_date DATE;

-- Existing allocations take their application's intake.
UPDATE room_allocation ra
SET stay_start_date = i.stay_start_date,
    stay_end_date   = i.stay_end_date
FROM application a
JOIN intake i ON i.id = a.intake_id
WHERE a.id = ra.application_id
  AND ra.stay_start_date IS NULL;

-- An application with no intake gives the allocation its academic year's dates. A year that ends
-- before it starts gives none, rather than failing the deploy.
UPDATE room_allocation ra
SET stay_start_date = y.start_date,
    stay_end_date   = y.end_date
FROM academic_year y
WHERE y.id = ra.academic_year_id
  AND ra.stay_start_date IS NULL
  AND y.start_date < y.end_date;

-- Anything left (an allocation made without an application) keeps no planned dates and blocks the
-- bed for every date, as all allocations did before.
ALTER TABLE room_allocation
    ADD CONSTRAINT ck_room_allocation_stay_dates
    CHECK ((stay_start_date IS NULL) = (stay_end_date IS NULL)
           AND (stay_start_date IS NULL OR stay_start_date < stay_end_date));

CREATE INDEX idx_room_allocation_room_id_stay_dates
    ON room_allocation (room_id, stay_start_date, stay_end_date);

COMMENT ON COLUMN room_allocation.stay_start_date IS
    'Planned move-in, copied from the intake (GH-496)';
COMMENT ON COLUMN room_allocation.stay_end_date IS
    'Planned check-out; the bed is free again from this date (GH-496)';
