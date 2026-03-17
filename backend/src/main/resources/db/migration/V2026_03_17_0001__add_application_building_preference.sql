-- Migration: Add application_building_preference table for ordered multi-building preferences
-- Students may select up to 3 preferred buildings (in order) when submitting an application.

CREATE TABLE application_building_preference (
    id                BIGSERIAL       PRIMARY KEY,
    application_id    BIGINT          NOT NULL REFERENCES application(id) ON DELETE CASCADE,
    building_id       BIGINT          NOT NULL REFERENCES building(id)    ON DELETE CASCADE,
    preference_order  INT             NOT NULL CHECK (preference_order BETWEEN 1 AND 3),
    UNIQUE (application_id, preference_order)
);

CREATE INDEX idx_app_building_pref_application ON application_building_preference (application_id);
