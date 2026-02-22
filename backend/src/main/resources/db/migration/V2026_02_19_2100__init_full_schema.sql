-- =============================================================================
-- V2026_02_19_2100 - Complete schema for student accommodation system
-- =============================================================================
-- Replaces all 55 previous migration files.
-- Applied to a fresh (empty) database after dropping the old one.
-- Key fixes vs old migrations:
--   * All enum columns use VARCHAR(255) — avoids Hibernate hbm2ddl ALTER TYPE error
--   * activity_logs created as partitioned table from the start
--   * No duplicate indexes, clean FK dependency order
-- =============================================================================


-- =============================================================================
-- ENUM TYPES
-- =============================================================================

-- NOTE: All enum columns use VARCHAR(255) to match Hibernate's @Enumerated(EnumType.STRING) mapping.
-- PostgreSQL native enum types are NOT used because Hibernate would generate
-- 'ALTER COLUMN ... SET DATA TYPE varchar(255)' on startup (hbm2ddl update),
-- which PostgreSQL rejects with a type-cast error.
-- Java enums enforce valid values at the application layer.


-- =============================================================================
-- TENANT  (must exist before all tenant_id FKs)
-- =============================================================================

CREATE TABLE tenant (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    domain     VARCHAR(255),
    config     JSONB        NOT NULL DEFAULT '{}',
    is_active  BOOLEAN      NOT NULL DEFAULT true,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_name   UNIQUE (name),
    CONSTRAINT uk_tenant_domain UNIQUE (domain)
);

CREATE INDEX idx_tenant_is_active ON tenant(is_active);

COMMENT ON TABLE  tenant        IS 'Accommodation providers (landlords) for multi-tenant SaaS deployment';
COMMENT ON COLUMN tenant.config IS 'Tenant-specific configuration overrides (JSON): offer_expiry_days, max_decline_count, required_document_types';


-- =============================================================================
-- REFERENCE / LOOKUP TABLES
-- =============================================================================

CREATE TABLE institution (
    id           SERIAL       PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    address      VARCHAR(255),
    phone_number VARCHAR(20),
    email        VARCHAR(100),
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE year_of_study (
    id         SERIAL    PRIMARY KEY,
    year       VARCHAR   NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_type (
    id          SERIAL      PRIMARY KEY,
    type_name   VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_required BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accommodation_type (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE academic_year (
    id                     SERIAL      PRIMARY KEY,
    name                   VARCHAR(20) NOT NULL UNIQUE,
    start_date             DATE        NOT NULL,
    end_date               DATE        NOT NULL,
    application_open_date  DATE        NOT NULL,
    application_close_date DATE        NOT NULL,
    is_current             BOOLEAN     DEFAULT FALSE,
    tenant_id              BIGINT      NOT NULL,
    created_at             TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_academic_year_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX idx_academic_year_current ON academic_year(is_current) WHERE is_current = TRUE;
CREATE INDEX        idx_academic_year_dates    ON academic_year(application_open_date, application_close_date);
CREATE INDEX        idx_academic_year_tenant_id ON academic_year(tenant_id);

COMMENT ON TABLE  academic_year           IS 'Stores academic year configurations with application window dates';
COMMENT ON COLUMN academic_year.name      IS 'Display name for the academic year, e.g., 2026';
COMMENT ON COLUMN academic_year.is_current IS 'Only one row can have this set to TRUE';


-- =============================================================================
-- CORE USER & STUDENT
-- =============================================================================

CREATE TABLE app_user (
    id                    SERIAL       PRIMARY KEY,
    email                 VARCHAR(100) NOT NULL UNIQUE,
    user_password         VARCHAR(255) NOT NULL,
    user_role             VARCHAR(20)  NOT NULL,
    first_name            VARCHAR(50)  NOT NULL,
    last_name             VARCHAR(50)  NOT NULL,
    email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT false,
    account_locked_until  TIMESTAMP,
    failed_login_attempts INTEGER               DEFAULT 0,
    is_anonymized         BOOLEAN               DEFAULT FALSE,
    tenant_id             BIGINT       NOT NULL,
    created_at            TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_app_user_email_verified ON app_user(email_verified);
CREATE INDEX idx_app_user_is_anonymized  ON app_user(is_anonymized);
CREATE INDEX idx_app_user_tenant_id      ON app_user(tenant_id);

COMMENT ON COLUMN app_user.email_verified IS 'Flag to indicate if user email has been verified';
COMMENT ON COLUMN app_user.tenant_id      IS 'FK to tenant table; admin users scoped to tenant';

CREATE TABLE student (
    id                   SERIAL      PRIMARY KEY,
    student_number       VARCHAR(20) NOT NULL UNIQUE,
    phone_number         VARCHAR(20),
    date_of_birth        DATE,
    registration_date    DATE        NOT NULL,
    gender               VARCHAR(30),
    special_requirements VARCHAR(100),
    user_id              INT         NOT NULL,
    institution_id       INT,
    year_of_study_id     INT,
    created_at           TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)          REFERENCES app_user(id)     ON DELETE CASCADE,
    FOREIGN KEY (institution_id)   REFERENCES institution(id)  ON DELETE SET NULL,
    FOREIGN KEY (year_of_study_id) REFERENCES year_of_study(id) ON DELETE SET NULL
);


-- =============================================================================
-- BUILDING & ROOM
-- =============================================================================

CREATE TABLE building (
    id                 SERIAL          PRIMARY KEY,
    name               VARCHAR(100)    NOT NULL UNIQUE,
    address            VARCHAR(255),
    description        TEXT,
    total_floors       INT,
    latitude           DECIMAL(10, 8),
    longitude          DECIMAL(11, 8),
    place_id           VARCHAR(255),
    formatted_address  VARCHAR(500),
    address_components JSONB,
    tenant_id          BIGINT          NOT NULL,
    created_at         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_building_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_building_tenant_id    ON building(tenant_id);
CREATE INDEX idx_building_coordinates  ON building(latitude, longitude);
CREATE INDEX idx_building_place_id     ON building(place_id);

COMMENT ON COLUMN building.tenant_id IS 'FK to tenant table; building owner';

CREATE TABLE room (
    id                SERIAL    PRIMARY KEY,
    room_number       VARCHAR(20) NOT NULL UNIQUE,
    capacity          INT         NOT NULL,
    current_occupancy INTEGER     NOT NULL DEFAULT 0,
    version           BIGINT      NOT NULL DEFAULT 0,
    building_id       INT,
    tenant_id         BIGINT      NOT NULL,
    created_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE SET NULL,
    CONSTRAINT fk_room_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenant(id)   ON DELETE RESTRICT
);

CREATE INDEX idx_room_tenant_id      ON room(tenant_id);
CREATE INDEX idx_room_tenant_building ON room(tenant_id, building_id);

COMMENT ON COLUMN room.tenant_id IS 'FK to tenant table; belongs to building';


-- =============================================================================
-- APPLICATION
-- =============================================================================

CREATE TABLE application (
    id                     SERIAL                  PRIMARY KEY,
    application_code       VARCHAR(50)             NOT NULL UNIQUE,
    application_date       DATE                    DEFAULT CURRENT_DATE,
    status                 VARCHAR(20)             NOT NULL DEFAULT 'PENDING',
    remarks                VARCHAR(255),
    version                BIGINT                  NOT NULL DEFAULT 0,
    student_id             INT                     NOT NULL,
    academic_year_id       INT,
    tenant_id              BIGINT                  NOT NULL,
    preferred_building_id  INT,
    allocation_method      VARCHAR(255)             DEFAULT 'MANUAL',
    requires_accessibility BOOLEAN                 DEFAULT FALSE,
    preferred_room_type    VARCHAR(50),
    special_requirements   VARCHAR(500),
    institution_name       VARCHAR(150),
    course_name            VARCHAR(150),
    created_at             TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id)            REFERENCES student(id)       ON DELETE CASCADE,
    FOREIGN KEY (academic_year_id)      REFERENCES academic_year(id) ON DELETE SET NULL,
    FOREIGN KEY (preferred_building_id) REFERENCES building(id)      ON DELETE SET NULL,
    CONSTRAINT fk_application_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_application_preferred_building_id ON application(preferred_building_id);
CREATE INDEX idx_application_allocation_method      ON application(allocation_method);
CREATE INDEX idx_application_academic_year          ON application(academic_year_id);
CREATE INDEX idx_application_tenant_id              ON application(tenant_id);
CREATE INDEX idx_application_status_year            ON application(status, academic_year_id);
CREATE INDEX idx_application_tenant_status          ON application(tenant_id, status);

COMMENT ON COLUMN application.status    IS 'Application lifecycle status: PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED, AWAITING_ALLOCATION, ALLOCATED, CHECKED_IN, REJECTED, EXPIRED, CANCELLED';
COMMENT ON COLUMN application.tenant_id IS 'FK to tenant table; student application scoped to tenant';


-- =============================================================================
-- DOCUMENT
-- =============================================================================

CREATE TABLE document (
    id               SERIAL     PRIMARY KEY,
    document_name    VARCHAR(100) NOT NULL,
    document_type_id INT          NOT NULL,
    document_path    VARCHAR(255) NOT NULL,
    document_status  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    file_size        BIGINT,
    student_id       INT          NOT NULL,
    application_id   BIGINT       NOT NULL,
    tenant_id        BIGINT       NOT NULL,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_type_id) REFERENCES document_type(id)  ON DELETE SET NULL,
    FOREIGN KEY (student_id)       REFERENCES student(id)         ON DELETE CASCADE,
    CONSTRAINT fk_document_application FOREIGN KEY (application_id) REFERENCES application(id),
    CONSTRAINT fk_document_tenant      FOREIGN KEY (tenant_id)      REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_document_application_id     ON document(application_id);
CREATE INDEX idx_document_tenant_id          ON document(tenant_id);
CREATE INDEX idx_document_application_status ON document(application_id, document_status);

COMMENT ON COLUMN document.file_size  IS 'File size in bytes of the uploaded document';
COMMENT ON COLUMN document.tenant_id  IS 'FK to tenant table';

CREATE TABLE document_comments (
    id           SERIAL PRIMARY KEY,
    document_id  INT    NOT NULL,
    admin_id     INT    NOT NULL,
    comment_text TEXT   NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES document(id)  ON DELETE CASCADE,
    FOREIGN KEY (admin_id)    REFERENCES app_user(id)  ON DELETE CASCADE
);

CREATE INDEX idx_document_comments_document_id ON document_comments(document_id);
CREATE INDEX idx_document_comments_admin_id    ON document_comments(admin_id);


-- =============================================================================
-- ROOM ALLOCATION
-- =============================================================================

CREATE TABLE room_allocation (
    id                     SERIAL     PRIMARY KEY,
    allocation_date        TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
    status                 VARCHAR(20),
    allocation_method      VARCHAR(50),
    end_date               TIMESTAMP,
    check_in_date          TIMESTAMP,
    check_out_date         TIMESTAMP,
    expected_check_in_date TIMESTAMP,
    offer_expiry_date      TIMESTAMP,
    check_in_expiry_date   TIMESTAMP,
    decline_reason         VARCHAR(50),
    decline_reason_text    VARCHAR(500),
    student_id             INT        NOT NULL,
    room_id                INT        NOT NULL,
    application_id         INT,
    academic_year_id       INT,
    tenant_id              BIGINT     NOT NULL,
    created_at             TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP  DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id)    REFERENCES student(id)      ON DELETE CASCADE,
    FOREIGN KEY (room_id)       REFERENCES room(id)         ON DELETE CASCADE,
    CONSTRAINT fk_room_allocation_application   FOREIGN KEY (application_id)  REFERENCES application(id)   ON DELETE SET NULL,
    CONSTRAINT fk_room_allocation_academic_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(id) ON DELETE SET NULL,
    CONSTRAINT fk_room_allocation_tenant        FOREIGN KEY (tenant_id)        REFERENCES tenant(id)        ON DELETE RESTRICT
);

CREATE INDEX idx_room_allocation_academic_year  ON room_allocation(academic_year_id);
CREATE INDEX idx_room_allocation_tenant_id      ON room_allocation(tenant_id);
CREATE INDEX idx_room_allocation_status         ON room_allocation(status);
CREATE INDEX idx_room_allocation_student        ON room_allocation(student_id);
CREATE INDEX idx_room_allocation_tenant_status  ON room_allocation(tenant_id, status);

-- Partial indexes for scheduled expiry jobs (varchar literals — no enum commit issue)
CREATE INDEX idx_room_allocation_offer_expiry    ON room_allocation(status, offer_expiry_date)   WHERE status = 'OFFERED';
CREATE INDEX idx_room_allocation_check_in_expiry ON room_allocation(status, check_in_expiry_date) WHERE status = 'ACCEPTED';

COMMENT ON COLUMN room_allocation.tenant_id IS 'FK to tenant table';


-- =============================================================================
-- STAGED ALLOCATIONS
-- =============================================================================

CREATE TABLE staged_allocations (
    id             SERIAL                        PRIMARY KEY,
    student_id     INT                            NOT NULL,
    room_id        INT                            NOT NULL,
    application_id INT,
    status         VARCHAR(255)                   DEFAULT 'PROPOSED',
    score          DECIMAL(10,4),
    warnings       JSONB,
    proposed_by    INT,
    batch_id       VARCHAR(100),
    tenant_id      BIGINT                         NOT NULL,
    created_at     TIMESTAMP                      DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP                      DEFAULT CURRENT_TIMESTAMP,
    confirmed_at   TIMESTAMP,
    FOREIGN KEY (student_id)     REFERENCES student(id)      ON DELETE CASCADE,
    FOREIGN KEY (room_id)        REFERENCES room(id)         ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES application(id)  ON DELETE SET NULL,
    FOREIGN KEY (proposed_by)    REFERENCES app_user(id)     ON DELETE SET NULL,
    CONSTRAINT fk_staged_allocations_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_staged_allocations_batch_id       ON staged_allocations(batch_id);
CREATE INDEX idx_staged_allocations_status         ON staged_allocations(status);
CREATE INDEX idx_staged_allocations_student_id     ON staged_allocations(student_id);
CREATE INDEX idx_staged_allocations_room_id        ON staged_allocations(room_id);
CREATE INDEX idx_staged_allocations_application_id ON staged_allocations(application_id);
CREATE INDEX idx_staged_allocations_tenant_id      ON staged_allocations(tenant_id);

COMMENT ON COLUMN staged_allocations.tenant_id IS 'FK to tenant table';


-- =============================================================================
-- ALLOCATION AUDIT LOG
-- =============================================================================

CREATE TABLE allocation_audit_log (
    id                  SERIAL                      PRIMARY KEY,
    allocation_id       INT,
    action_type         VARCHAR(255)                NOT NULL,
    previous_status     VARCHAR(50),
    new_status          VARCHAR(50),
    admin_user_id       INT,
    reason              TEXT,
    old_room_id         INT,
    new_room_id         INT,
    constraint_bypassed VARCHAR(255),
    created_at          TIMESTAMP                   DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (allocation_id)  REFERENCES room_allocation(id) ON DELETE SET NULL,
    FOREIGN KEY (admin_user_id)  REFERENCES app_user(id)        ON DELETE SET NULL,
    FOREIGN KEY (old_room_id)    REFERENCES room(id)            ON DELETE SET NULL,
    FOREIGN KEY (new_room_id)    REFERENCES room(id)            ON DELETE SET NULL
);

CREATE INDEX idx_allocation_audit_log_allocation_id ON allocation_audit_log(allocation_id);
CREATE INDEX idx_allocation_audit_log_admin_user_id ON allocation_audit_log(admin_user_id);
CREATE INDEX idx_allocation_audit_log_created_at    ON allocation_audit_log(created_at);
CREATE INDEX idx_allocation_audit_log_action_type   ON allocation_audit_log(action_type);

-- Partial index: action_type is VARCHAR, so the string literal comparison is always valid
CREATE INDEX idx_allocation_audit_log_status_change
    ON allocation_audit_log(allocation_id, action_type)
    WHERE action_type = 'STATUS_CHANGE';


-- =============================================================================
-- WAITING LIST
-- =============================================================================

CREATE TABLE waiting_list_entry (
    id             BIGSERIAL  PRIMARY KEY,
    student_id     BIGINT     NOT NULL,
    application_id BIGINT,
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    tenant_id      BIGINT     NOT NULL,
    created_at     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_waiting_list_student      FOREIGN KEY (student_id)     REFERENCES student(id),
    CONSTRAINT fk_waiting_list_application  FOREIGN KEY (application_id) REFERENCES application(id),
    CONSTRAINT fk_waiting_list_entry_tenant FOREIGN KEY (tenant_id)      REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_waiting_list_status_created_at   ON waiting_list_entry(status, created_at);
CREATE INDEX idx_waiting_list_student_status       ON waiting_list_entry(student_id, status);
CREATE INDEX idx_waiting_list_entry_tenant_id      ON waiting_list_entry(tenant_id);

CREATE OR REPLACE FUNCTION set_waiting_list_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_waiting_list_updated_at ON waiting_list_entry;
CREATE TRIGGER trg_waiting_list_updated_at
BEFORE UPDATE ON waiting_list_entry
FOR EACH ROW EXECUTE FUNCTION set_waiting_list_updated_at();


-- =============================================================================
-- ALLOCATION RULES
-- =============================================================================

CREATE TABLE allocation_rules (
    id                      SERIAL       PRIMARY KEY,
    building_id             INT,
    rule_name               VARCHAR(100) NOT NULL,
    preference_match_weight DECIMAL(5,2) DEFAULT 1.0,
    desirability_weight     DECIMAL(5,2) DEFAULT 1.0,
    accessibility_weight    DECIMAL(5,2) DEFAULT 1.0,
    active                  BOOLEAN      DEFAULT TRUE,
    priority                INT          DEFAULT 0,
    tenant_id               BIGINT       NOT NULL,
    created_by              INT,
    created_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (building_id) REFERENCES building(id)  ON DELETE CASCADE,
    FOREIGN KEY (created_by)  REFERENCES app_user(id)  ON DELETE SET NULL,
    CONSTRAINT fk_allocation_rules_tenant        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT uq_allocation_rules_building_name UNIQUE (building_id, rule_name)
);

CREATE INDEX idx_allocation_rules_building_id ON allocation_rules(building_id);
CREATE INDEX idx_allocation_rules_active       ON allocation_rules(active);
CREATE INDEX idx_allocation_rules_priority     ON allocation_rules(priority);
CREATE INDEX idx_allocation_rules_tenant_id    ON allocation_rules(tenant_id);

COMMENT ON COLUMN allocation_rules.tenant_id IS 'FK to tenant table';


-- =============================================================================
-- BUILDING & ROOM ATTRIBUTES
-- =============================================================================

CREATE TABLE building_attributes (
    id                     SERIAL                  PRIMARY KEY,
    building_id            INT                     NOT NULL UNIQUE,
    gender_restriction     VARCHAR(255)            DEFAULT 'ANY',
    accessibility_features JSONB,
    desirability_score     INT CHECK (desirability_score >= 1 AND desirability_score <= 10),
    allow_mixed_gender     BOOLEAN                 DEFAULT FALSE,
    created_at             TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE
);

CREATE INDEX idx_building_attributes_building_id        ON building_attributes(building_id);
CREATE INDEX idx_building_attributes_gender_restriction ON building_attributes(gender_restriction);

CREATE TABLE room_attributes (
    id                      SERIAL                  PRIMARY KEY,
    room_id                 INT                     NOT NULL UNIQUE,
    accessibility_compliant BOOLEAN                 DEFAULT FALSE,
    gender_restriction      VARCHAR(255),
    desirability_override   INT                     CHECK (desirability_override IS NULL OR (desirability_override >= 1 AND desirability_override <= 10)),
    min_year_of_study       INTEGER                 NULL,
    max_year_of_study       INTEGER                 NULL,
    created_at              TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE,
    CONSTRAINT chk_min_year_of_study CHECK (min_year_of_study IS NULL OR (min_year_of_study >= 1 AND min_year_of_study <= 10)),
    CONSTRAINT chk_max_year_of_study CHECK (max_year_of_study IS NULL OR (max_year_of_study >= 1 AND max_year_of_study <= 10)),
    CONSTRAINT chk_year_range        CHECK (min_year_of_study IS NULL OR max_year_of_study IS NULL OR min_year_of_study <= max_year_of_study)
);

CREATE INDEX idx_room_attributes_room_id        ON room_attributes(room_id);
CREATE INDEX idx_room_attributes_accessibility  ON room_attributes(accessibility_compliant);

CREATE TABLE building_institution (
    building_id    BIGINT NOT NULL,
    institution_id BIGINT NOT NULL,
    PRIMARY KEY (building_id, institution_id),
    CONSTRAINT fk_building_institution_building    FOREIGN KEY (building_id)    REFERENCES building(id)     ON DELETE CASCADE,
    CONSTRAINT fk_building_institution_institution FOREIGN KEY (institution_id) REFERENCES institution(id)  ON DELETE CASCADE
);

CREATE INDEX idx_building_institution_building    ON building_institution(building_id);
CREATE INDEX idx_building_institution_institution ON building_institution(institution_id);

CREATE TABLE building_image (
    id            BIGSERIAL    PRIMARY KEY,
    building_id   BIGINT       NOT NULL REFERENCES building(id) ON DELETE CASCADE,
    tenant_id     BIGINT       NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    object_path   VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100),
    file_size     BIGINT,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_building_image_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX        idx_building_image_building_id ON building_image(building_id);
CREATE UNIQUE INDEX idx_building_image_primary     ON building_image(building_id) WHERE is_primary = TRUE;
CREATE INDEX        idx_building_image_tenant_id   ON building_image(tenant_id);


-- =============================================================================
-- NOTIFICATIONS
-- =============================================================================

CREATE TABLE notification (
    id                  BIGSERIAL    PRIMARY KEY,
    recipient           VARCHAR(255) NOT NULL,
    subject             VARCHAR(255) NOT NULL,
    notification_type   VARCHAR(50)  NOT NULL,
    notification_status VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    sent_at             TIMESTAMP,
    retry_count         INTEGER      NOT NULL DEFAULT 0,
    max_retries         INTEGER      NOT NULL DEFAULT 3,
    error_message       TEXT,
    last_attempt_at     TIMESTAMP,
    user_id             BIGINT,
    reference_id        BIGINT,
    reference_type      VARCHAR(50),
    tenant_id           BIGINT       NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user   FOREIGN KEY (user_id)   REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_notification_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)   ON DELETE RESTRICT
);

CREATE INDEX idx_notification_status       ON notification(notification_status);
CREATE INDEX idx_notification_recipient    ON notification(recipient);
CREATE INDEX idx_notification_user_id      ON notification(user_id);
CREATE INDEX idx_notification_type         ON notification(notification_type);
CREATE INDEX idx_notification_created_at   ON notification(created_at);
CREATE INDEX idx_notification_retry_lookup ON notification(notification_status, retry_count, last_attempt_at) WHERE notification_status = 'FAILED';
CREATE INDEX idx_notification_reference    ON notification(reference_type, reference_id);
CREATE INDEX idx_notification_tenant_id    ON notification(tenant_id);

ALTER TABLE notification ADD CONSTRAINT chk_notification_status CHECK (
    notification_status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING')
);
ALTER TABLE notification ADD CONSTRAINT chk_retry_count_positive CHECK (retry_count >= 0);
ALTER TABLE notification ADD CONSTRAINT chk_max_retries_positive CHECK (max_retries >= 0);

COMMENT ON TABLE  notification                IS 'Stores audit trail and tracking information for all system notifications';
COMMENT ON COLUMN notification.recipient      IS 'Email address of the recipient';
COMMENT ON COLUMN notification.subject        IS 'Email subject line';
COMMENT ON COLUMN notification.notification_type   IS 'Type of notification being sent';
COMMENT ON COLUMN notification.notification_status IS 'Current status of the notification';
COMMENT ON COLUMN notification.sent_at        IS 'Timestamp when notification was successfully sent';
COMMENT ON COLUMN notification.retry_count    IS 'Number of retry attempts made';
COMMENT ON COLUMN notification.max_retries    IS 'Maximum number of retries allowed';
COMMENT ON COLUMN notification.error_message  IS 'Error message from last failed attempt';
COMMENT ON COLUMN notification.last_attempt_at IS 'Timestamp of the last delivery attempt';
COMMENT ON COLUMN notification.user_id        IS 'Reference to the user receiving the notification';
COMMENT ON COLUMN notification.reference_id   IS 'ID of the related entity (Application, Document, etc.)';
COMMENT ON COLUMN notification.reference_type IS 'Type of the related entity';
COMMENT ON COLUMN notification.tenant_id      IS 'FK to tenant table';

CREATE TABLE user_notifications (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    type           VARCHAR(50)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    message        TEXT         NOT NULL,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at        TIMESTAMP,
    data           TEXT,
    reference_id   BIGINT,
    reference_type VARCHAR(50),
    tenant_id      BIGINT       NOT NULL,
    CONSTRAINT fk_user_notification_user    FOREIGN KEY (user_id)   REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)   ON DELETE RESTRICT
);

CREATE INDEX idx_user_notifications_user_id    ON user_notifications(user_id);
CREATE INDEX idx_user_notifications_is_read    ON user_notifications(is_read);
CREATE INDEX idx_user_notifications_created_at ON user_notifications(created_at DESC);
CREATE INDEX idx_user_notifications_type       ON user_notifications(type);
CREATE INDEX idx_user_notifications_reference  ON user_notifications(reference_type, reference_id);
CREATE INDEX idx_user_notifications_tenant_id  ON user_notifications(tenant_id);
CREATE INDEX idx_user_notification_user_read   ON user_notifications(user_id, is_read);

COMMENT ON TABLE  user_notifications      IS 'Stores in-app notifications shown in the notification bell icon';
COMMENT ON COLUMN user_notifications.type IS 'Notification type: ROOM_ALLOCATION, DOCUMENT_APPROVED, APPLICATION_APPROVED, PAYMENT_DUE, SYSTEM_ANNOUNCEMENT';
COMMENT ON COLUMN user_notifications.data IS 'Additional data stored as JSON string';
COMMENT ON COLUMN user_notifications.reference_id   IS 'ID of the related entity (application, document, room, etc.)';
COMMENT ON COLUMN user_notifications.reference_type IS 'Type of the related entity (APPLICATION, DOCUMENT, ROOM_ALLOCATION, etc.)';
COMMENT ON COLUMN user_notifications.tenant_id      IS 'FK to tenant table';


-- =============================================================================
-- ACTIVITY LOGS  (Partitioned by timestamp from the start)
-- =============================================================================

CREATE TABLE activity_logs (
    id          BIGSERIAL,
    user_id     BIGINT      NOT NULL,
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   BIGINT,
    description TEXT        NOT NULL,
    ip_address  VARCHAR(45),
    timestamp   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata    TEXT,
    tenant_id   BIGINT      NOT NULL,
    PRIMARY KEY (id, timestamp),
    CONSTRAINT fk_activity_log_user    FOREIGN KEY (user_id)   REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)   ON DELETE RESTRICT
) PARTITION BY RANGE (timestamp);

-- Year partitions
CREATE TABLE activity_logs_2024    PARTITION OF activity_logs FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE activity_logs_2025    PARTITION OF activity_logs FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE activity_logs_2026    PARTITION OF activity_logs FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE activity_logs_2027    PARTITION OF activity_logs FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE activity_logs_default PARTITION OF activity_logs DEFAULT;

CREATE INDEX idx_activity_logs_user_id        ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_action         ON activity_logs(action);
CREATE INDEX idx_activity_logs_entity_type    ON activity_logs(entity_type);
CREATE INDEX idx_activity_logs_timestamp      ON activity_logs(timestamp DESC);
CREATE INDEX idx_activity_logs_entity         ON activity_logs(entity_type, entity_id);
CREATE INDEX idx_activity_logs_user_timestamp ON activity_logs(user_id, timestamp DESC);
CREATE INDEX idx_activity_logs_action_entity  ON activity_logs(action, entity_type);
CREATE INDEX idx_activity_logs_tenant_id      ON activity_logs(tenant_id);

COMMENT ON TABLE  activity_logs             IS 'Audit trail of all user actions in the system (Partitioned by Year)';
COMMENT ON COLUMN activity_logs.action      IS 'Action performed: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, APPROVE, REJECT, UPLOAD, DOWNLOAD';
COMMENT ON COLUMN activity_logs.entity_type IS 'Type of entity: APPLICATION, DOCUMENT, USER, ROOM, PAYMENT, BUILDING';
COMMENT ON COLUMN activity_logs.ip_address  IS 'IP address of the user (supports IPv4 and IPv6)';
COMMENT ON COLUMN activity_logs.metadata    IS 'Additional metadata stored as JSON string';
COMMENT ON COLUMN activity_logs.tenant_id   IS 'FK to tenant table';


-- =============================================================================
-- SYSTEM SETTINGS
-- =============================================================================

CREATE TABLE system_settings (
    id            SERIAL       PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(255),
    description   TEXT,
    tenant_id     BIGINT       NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_system_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT
);

CREATE INDEX idx_system_settings_key       ON system_settings(setting_key);
CREATE INDEX idx_system_settings_tenant_id ON system_settings(tenant_id);

COMMENT ON COLUMN system_settings.tenant_id IS 'FK to tenant table; tenant-specific settings';


-- =============================================================================
-- APPLICATION PREFERENCES ARCHIVE  (historical audit, no active writes)
-- =============================================================================

CREATE TABLE application_preferences_archive (
    application_id               BIGINT PRIMARY KEY,
    preferred_accommodation_type VARCHAR(50),
    preferred_move_in_date       DATE,
    preferred_duration_months    INTEGER,
    archived_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_archive_application FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE
);


-- =============================================================================
-- AUTH TOKENS
-- =============================================================================

CREATE TABLE password_reset_token (
    id          BIGSERIAL    PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_token_token   ON password_reset_token(token);
CREATE INDEX idx_password_reset_token_user_id ON password_reset_token(user_id);
CREATE INDEX idx_password_reset_token_expiry  ON password_reset_token(expiry_date);

COMMENT ON TABLE  password_reset_token            IS 'Stores password reset tokens for user password recovery';
COMMENT ON COLUMN password_reset_token.token       IS 'Unique token for password reset';
COMMENT ON COLUMN password_reset_token.expiry_date IS 'Expiration date of the token';
COMMENT ON COLUMN password_reset_token.used        IS 'Flag to indicate if token has been used';

CREATE TABLE email_verification_token (
    id          BIGSERIAL    PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_token_token   ON email_verification_token(token);
CREATE INDEX idx_email_verification_token_user_id ON email_verification_token(user_id);
CREATE INDEX idx_email_verification_token_expiry  ON email_verification_token(expiry_date);

COMMENT ON TABLE  email_verification_token            IS 'Stores email verification tokens for user email confirmation';
COMMENT ON COLUMN email_verification_token.token       IS 'Unique token for email verification';
COMMENT ON COLUMN email_verification_token.expiry_date IS 'Expiration date of the token';
COMMENT ON COLUMN email_verification_token.used        IS 'Flag to indicate if token has been used';


-- =============================================================================
-- FF4J FEATURE FLAG TABLES
-- (clazz column included from the start — was missing in the originals)
-- =============================================================================

CREATE TABLE IF NOT EXISTS ff4j_features (
    uid         VARCHAR(100) PRIMARY KEY,
    enable      BOOLEAN      NOT NULL,
    description VARCHAR(1000),
    strategy    VARCHAR(1000),
    expression  VARCHAR(255),
    clazz       VARCHAR(255),
    groupname   VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS ff4j_roles (
    feat_uid  VARCHAR(100) NOT NULL REFERENCES ff4j_features(uid),
    role_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (feat_uid, role_name)
);

CREATE TABLE IF NOT EXISTS ff4j_audit (
    evt_uuid     VARCHAR(40) PRIMARY KEY,
    evt_time     TIMESTAMP   NOT NULL,
    evt_type     VARCHAR(100),
    evt_name     VARCHAR(100),
    evt_action   VARCHAR(100),
    evt_hostname VARCHAR(100),
    evt_source   VARCHAR(100),
    evt_duration INTEGER,
    evt_user     VARCHAR(100),
    evt_value    VARCHAR(100),
    evt_keys     TEXT
);

CREATE TABLE IF NOT EXISTS ff4j_custom_properties (
    property_id  VARCHAR(100) NOT NULL,
    clazz        VARCHAR(255) NOT NULL,
    currentValue VARCHAR(255),
    description  VARCHAR(1000),
    fixedValues  VARCHAR(1000),
    feat_uid     VARCHAR(100) REFERENCES ff4j_features(uid),
    PRIMARY KEY (property_id, feat_uid)
);
