-- GH-494 step 1: intakes inside academic years.
--
-- An intake is what a student applies for: the Annual intake, Semester 1 and 2, or Trimester 1 to 3,
-- each with its own stay dates and application window.
CREATE TABLE intake (
    id                     BIGSERIAL   PRIMARY KEY,
    tenant_id              BIGINT      NOT NULL,
    academic_year_id       BIGINT      NOT NULL,
    intake_type            VARCHAR(20) NOT NULL,
    name                   VARCHAR(50) NOT NULL,
    stay_start_date        DATE        NOT NULL,
    stay_end_date          DATE        NOT NULL,
    application_open_date  DATE        NOT NULL,
    application_close_date DATE        NOT NULL,
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_intake_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    -- An intake is part of its year: deleting a year (only allowed without applications) removes them.
    CONSTRAINT fk_intake_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id) ON DELETE CASCADE,
    CONSTRAINT ck_intake_type CHECK (intake_type IN ('ANNUAL', 'SEMESTER', 'TRIMESTER')),
    CONSTRAINT ck_intake_stay_dates CHECK (stay_start_date < stay_end_date),
    -- Equal dates are allowed: "close applications now" on the day a window opens closes it that day.
    CONSTRAINT ck_intake_window_dates CHECK (application_open_date <= application_close_date),
    CONSTRAINT uq_intake_academic_year_name UNIQUE (academic_year_id, name)
);

CREATE INDEX idx_intake_academic_year_id ON intake(academic_year_id);
CREATE INDEX idx_intake_tenant_id        ON intake(tenant_id);

COMMENT ON TABLE intake IS 'What a student applies for: stay dates and an application window inside an academic year';

-- Stay dates must fall inside the year. A CHECK cannot read another table, so two constraint
-- triggers enforce it from both sides. They are deferred to commit, so a year and its intakes can
-- be moved together in one transaction (copying or re-dating a year).
CREATE FUNCTION check_intake_within_year() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM academic_year y
               WHERE y.id = NEW.academic_year_id
                 AND (NEW.stay_start_date < y.start_date OR NEW.stay_end_date > y.end_date)) THEN
        RAISE EXCEPTION 'intake % stay dates fall outside academic year %', NEW.id, NEW.academic_year_id
            USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_intake_within_year
    AFTER INSERT OR UPDATE ON intake
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_intake_within_year();

CREATE FUNCTION check_academic_year_contains_intakes() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM intake i
               WHERE i.academic_year_id = NEW.id
                 AND (i.stay_start_date < NEW.start_date OR i.stay_end_date > NEW.end_date)) THEN
        RAISE EXCEPTION 'academic year % no longer contains all of its intakes', NEW.id
            USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_academic_year_contains_intakes
    AFTER UPDATE OF start_date, end_date ON academic_year
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_academic_year_contains_intakes();

-- Lead times for a new intake's window, counted back from its move-in date. Platform defaults;
-- a company overrides them through the system-settings cascade.
INSERT INTO system_settings (setting_key, setting_value, description, tenant_id)
VALUES ('intake_window_open_days_before', '120',
        'Days before an intake''s move-in date that applications open', 0),
       ('intake_window_close_days_before', '14',
        'Days before an intake''s move-in date that applications close', 0)
ON CONFLICT (tenant_id, setting_key) DO NOTHING;
