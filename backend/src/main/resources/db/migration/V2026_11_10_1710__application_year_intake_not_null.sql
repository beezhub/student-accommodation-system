-- GH-496: application.academic_year_id and intake_id become NOT NULL, the last of the three steps
-- (nullable columns in group B, backfill in V2026_11_10_1300 and V2026_11_10_1700, constraint here).
--
-- A row that the backfill could not fill belongs to a company with no academic year at all, and
-- filling it would mean inventing a year. Such rows are not deleted and not invented: the
-- constraint is then left out for that column, with a warning in the migration log naming how many
-- rows are empty, and a later migration adds it once a person has dealt with them. Every path that
-- creates an application since group B sets both columns.
DO $$
DECLARE
    missing_year   BIGINT;
    missing_intake BIGINT;
BEGIN
    SELECT COUNT(*) INTO missing_year FROM application WHERE academic_year_id IS NULL;
    IF missing_year = 0 THEN
        ALTER TABLE application ALTER COLUMN academic_year_id SET NOT NULL;
    ELSE
        RAISE WARNING 'application.academic_year_id left nullable: % application(s) have no academic year '
                      'and their company has none to repair them with', missing_year;
    END IF;

    SELECT COUNT(*) INTO missing_intake FROM application WHERE intake_id IS NULL;
    IF missing_intake = 0 THEN
        ALTER TABLE application ALTER COLUMN intake_id SET NOT NULL;
    ELSE
        RAISE WARNING 'application.intake_id left nullable: % application(s) have no intake to repair them with',
                      missing_intake;
    END IF;
END $$;
