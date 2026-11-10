-- GH-496: drop the academic_year columns nothing reads any more.
--
-- is_current: retired in group A (V2026_11_10_1000); the current year follows the dates.
-- application_open_date, application_close_date: each intake has its own window since group A, and
-- since group B applying reads only the intake's. The year's window shown in the API is worked out
-- from its intakes. V2026_11_10_1700, the last migration to read them, runs before this one.
ALTER TABLE academic_year DROP COLUMN is_current;
ALTER TABLE academic_year DROP COLUMN application_open_date;
ALTER TABLE academic_year DROP COLUMN application_close_date;
