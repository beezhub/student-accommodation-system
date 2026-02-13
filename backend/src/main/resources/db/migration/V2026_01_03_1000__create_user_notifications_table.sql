-- Create user_notifications table for in-app notifications (bell icon)
CREATE TABLE user_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    data TEXT,
    reference_id BIGINT,
    reference_type VARCHAR(50),

    CONSTRAINT fk_user_notification_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_user_notifications_user_id ON user_notifications(user_id);
CREATE INDEX idx_user_notifications_is_read ON user_notifications(is_read);
CREATE INDEX idx_user_notifications_created_at ON user_notifications(created_at DESC);
CREATE INDEX idx_user_notifications_type ON user_notifications(type);
CREATE INDEX idx_user_notifications_reference ON user_notifications(reference_type, reference_id);

-- Add comment to table
COMMENT ON TABLE user_notifications IS 'Stores in-app notifications shown in the notification bell icon';
COMMENT ON COLUMN user_notifications.type IS 'Notification type: ROOM_ALLOCATION, DOCUMENT_APPROVED, APPLICATION_APPROVED, PAYMENT_DUE, SYSTEM_ANNOUNCEMENT';
COMMENT ON COLUMN user_notifications.data IS 'Additional data stored as JSON string';
COMMENT ON COLUMN user_notifications.reference_id IS 'ID of the related entity (application, document, room, etc.)';
COMMENT ON COLUMN user_notifications.reference_type IS 'Type of the related entity (APPLICATION, DOCUMENT, ROOM_ALLOCATION, etc.)';
