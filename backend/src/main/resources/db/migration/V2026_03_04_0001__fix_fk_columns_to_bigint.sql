-- =============================================================================
-- Fix FK columns from INT to BIGINT to match Long @Id on referenced entities.
-- On a fresh DB these columns are already BIGINT (init schema was corrected).
-- On an existing DB Flyway had created them as INT; this migration alters them.
-- =============================================================================

-- student
ALTER TABLE student
    ALTER COLUMN user_id         TYPE BIGINT,
    ALTER COLUMN institution_id  TYPE BIGINT,
    ALTER COLUMN year_of_study_id TYPE BIGINT;

-- room
ALTER TABLE room
    ALTER COLUMN building_id TYPE BIGINT;

-- application
ALTER TABLE application
    ALTER COLUMN student_id           TYPE BIGINT,
    ALTER COLUMN academic_year_id     TYPE BIGINT,
    ALTER COLUMN preferred_building_id TYPE BIGINT;

-- document
ALTER TABLE document
    ALTER COLUMN document_type_id TYPE BIGINT,
    ALTER COLUMN student_id       TYPE BIGINT;

-- document_comments
ALTER TABLE document_comments
    ALTER COLUMN document_id TYPE BIGINT,
    ALTER COLUMN admin_id    TYPE BIGINT;

-- room_allocation
ALTER TABLE room_allocation
    ALTER COLUMN student_id      TYPE BIGINT,
    ALTER COLUMN room_id         TYPE BIGINT,
    ALTER COLUMN application_id  TYPE BIGINT,
    ALTER COLUMN academic_year_id TYPE BIGINT;

-- staged_allocations
ALTER TABLE staged_allocations
    ALTER COLUMN student_id    TYPE BIGINT,
    ALTER COLUMN room_id       TYPE BIGINT,
    ALTER COLUMN application_id TYPE BIGINT,
    ALTER COLUMN proposed_by   TYPE BIGINT;

-- allocation_audit_log
ALTER TABLE allocation_audit_log
    ALTER COLUMN allocation_id  TYPE BIGINT,
    ALTER COLUMN admin_user_id  TYPE BIGINT,
    ALTER COLUMN old_room_id    TYPE BIGINT,
    ALTER COLUMN new_room_id    TYPE BIGINT;

-- allocation_rules
ALTER TABLE allocation_rules
    ALTER COLUMN building_id TYPE BIGINT,
    ALTER COLUMN created_by  TYPE BIGINT;

-- building_attributes
ALTER TABLE building_attributes
    ALTER COLUMN building_id TYPE BIGINT;

-- room_attributes
ALTER TABLE room_attributes
    ALTER COLUMN room_id TYPE BIGINT;
