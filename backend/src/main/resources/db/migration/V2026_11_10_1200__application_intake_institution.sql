-- GH-495 step 2: an application is for one intake of one company, and records the student's
-- institution as a link rather than only the free-text institution_name.
--
-- Both columns stay nullable here. Step 3 fills them in for existing rows, and the NOT NULL
-- constraints come in a later deploy (group C), so no instance still running the old code, which
-- inserts without an intake, can be refused during the rollout.
ALTER TABLE application ADD COLUMN intake_id BIGINT;
ALTER TABLE application ADD COLUMN institution_id BIGINT;

-- RESTRICT on both: an intake or institution with applications is refused on delete rather than
-- silently detached from them. IntakeService already refuses the intake case with a clear message.
ALTER TABLE application
    ADD CONSTRAINT fk_application_intake
    FOREIGN KEY (intake_id) REFERENCES intake(id) ON DELETE RESTRICT;

ALTER TABLE application
    ADD CONSTRAINT fk_application_institution
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT;

CREATE INDEX idx_application_intake_id      ON application(intake_id);
CREATE INDEX idx_application_institution_id ON application(institution_id);

COMMENT ON COLUMN application.intake_id IS
    'The intake applied for (GH-495). Nullable until group C, after step 3 has filled existing rows';
COMMENT ON COLUMN application.institution_id IS
    'The student''s institution when they applied, from their profile (GH-495)';
