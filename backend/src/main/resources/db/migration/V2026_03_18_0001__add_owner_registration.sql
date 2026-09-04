-- Owner registration: pre-approval self-service request table.
-- Stores registration requests BEFORE an app_user is created.
-- On admin approval, an app_user + building_owner profile is created.

CREATE TABLE IF NOT EXISTS owner_registration (
    id                              BIGSERIAL PRIMARY KEY,
    full_name                       VARCHAR(200) NOT NULL,
    email                           VARCHAR(255) NOT NULL,
    organisation_name               VARCHAR(200) NOT NULL,
    contact_phone                   VARCHAR(20),
    message                         TEXT,
    status                          VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                                        CHECK (status IN ('PENDING', 'PENDING_REVIEW', 'APPROVED', 'REJECTED')),
    tenant_id                       BIGINT REFERENCES tenant(id),
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at                     TIMESTAMP,
    reminder_sent_at                TIMESTAMP,
    -- Email verification token (24-hour expiry)
    email_verification_token        VARCHAR(255),
    email_verification_expires_at   TIMESTAMP,
    -- Password setup token issued on admin approval (48-hour expiry)
    password_setup_token            VARCHAR(255),
    password_setup_expires_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_owner_registration_email  ON owner_registration(email);
CREATE INDEX IF NOT EXISTS idx_owner_registration_status ON owner_registration(status);
CREATE INDEX IF NOT EXISTS idx_owner_registration_token  ON owner_registration(email_verification_token);
