-- Create Notification table for tracking all email notifications
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    notification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    last_attempt_at TIMESTAMP,
    user_id BIGINT,
    reference_id BIGINT,
    reference_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraint to app_user table
ALTER TABLE notification
ADD CONSTRAINT fk_notification_user
FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE SET NULL;

-- Create indexes for better query performance
CREATE INDEX idx_notification_status ON notification(notification_status);
CREATE INDEX idx_notification_recipient ON notification(recipient);
CREATE INDEX idx_notification_user_id ON notification(user_id);
CREATE INDEX idx_notification_type ON notification(notification_type);
CREATE INDEX idx_notification_created_at ON notification(created_at);

-- Create composite index for retry logic queries
CREATE INDEX idx_notification_retry_lookup ON notification(notification_status, retry_count, last_attempt_at)
WHERE notification_status = 'FAILED';

-- Create index for reference lookups
CREATE INDEX idx_notification_reference ON notification(reference_type, reference_id);

-- Add check constraints
ALTER TABLE notification
ADD CONSTRAINT chk_notification_type CHECK (
    notification_type IN (
        'APPLICATION_APPROVED',
        'APPLICATION_REJECTED',
        'DOCUMENT_VERIFIED',
        'DOCUMENT_REJECTED',
        'ROOM_ALLOCATED',
        'WELCOME_EMAIL',
        'PASSWORD_RESET',
        'GENERAL'
    )
);

ALTER TABLE notification
ADD CONSTRAINT chk_notification_status CHECK (
    notification_status IN (
        'PENDING',
        'SENT',
        'FAILED',
        'RETRYING'
    )
);

ALTER TABLE notification
ADD CONSTRAINT chk_retry_count_positive CHECK (retry_count >= 0);

ALTER TABLE notification
ADD CONSTRAINT chk_max_retries_positive CHECK (max_retries >= 0);

-- Add comments for documentation
COMMENT ON TABLE notification IS 'Stores audit trail and tracking information for all system notifications';
COMMENT ON COLUMN notification.recipient IS 'Email address of the recipient';
COMMENT ON COLUMN notification.subject IS 'Email subject line';
COMMENT ON COLUMN notification.notification_type IS 'Type of notification being sent';
COMMENT ON COLUMN notification.notification_status IS 'Current status of the notification';
COMMENT ON COLUMN notification.sent_at IS 'Timestamp when notification was successfully sent';
COMMENT ON COLUMN notification.retry_count IS 'Number of retry attempts made';
COMMENT ON COLUMN notification.max_retries IS 'Maximum number of retries allowed';
COMMENT ON COLUMN notification.error_message IS 'Error message from last failed attempt';
COMMENT ON COLUMN notification.last_attempt_at IS 'Timestamp of the last delivery attempt';
COMMENT ON COLUMN notification.user_id IS 'Reference to the user receiving the notification';
COMMENT ON COLUMN notification.reference_id IS 'ID of the related entity (Application, Document, etc.)';
COMMENT ON COLUMN notification.reference_type IS 'Type of the related entity';
