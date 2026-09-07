-- GH-239: students had nowhere to record an emergency contact, though two emails already told
-- them to. `grep -ri emergency` returned nothing in any .java or .sql file.
--
-- This is accommodation, and check-in is a physical, in-person process: staff hand someone keys
-- to a room without knowing who to call if something happens to them.
--
-- Columns on `student` rather than a `student_emergency_contact` table. Both the dead registration
-- form and the two emails assume a single contact, and the rest of the profile is stored this way.
-- A second contact would need a table, and that is a migration to write when there is a reason to
-- want one, not before.
--
-- All four are NULLABLE, deliberately. Existing students have no value and none can be invented
-- for them, and this repo's convention for adding NOT NULL is three steps (nullable, backfill,
-- constraint) — there is nothing to backfill from. The requirement is enforced where it is
-- meaningful instead: ApplicationCoreService.submitApplication already refuses an incomplete
-- profile with a message naming what is missing, so the contact is required going forward, for
-- exactly the students it matters for.
--
-- The contact is a third party who never signed up. UserAnonymizationService clears these columns
-- alongside the rest of the profile; see the note there.

ALTER TABLE student
    ADD COLUMN emergency_contact_name         VARCHAR(150),
    ADD COLUMN emergency_contact_relationship VARCHAR(50),
    ADD COLUMN emergency_contact_phone        VARCHAR(30),
    ADD COLUMN emergency_contact_email        VARCHAR(255);

COMMENT ON COLUMN student.emergency_contact_phone IS
    'Free-form, not the 10-digit domestic pattern used for the student''s own number: a student''s '
    'family may not be in the country, which is exactly when this record matters most (GH-239).';
