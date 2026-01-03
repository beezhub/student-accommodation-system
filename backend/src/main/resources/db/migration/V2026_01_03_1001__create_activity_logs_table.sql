-- Create activity_logs table for audit trail and admin monitoring
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    description TEXT NOT NULL,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT,

    CONSTRAINT fk_activity_log_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_action ON activity_logs(action);
CREATE INDEX idx_activity_logs_entity_type ON activity_logs(entity_type);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp DESC);
CREATE INDEX idx_activity_logs_entity ON activity_logs(entity_type, entity_id);

-- Composite index for common queries
CREATE INDEX idx_activity_logs_user_timestamp ON activity_logs(user_id, timestamp DESC);
CREATE INDEX idx_activity_logs_action_entity ON activity_logs(action, entity_type);

-- Add comments to table
COMMENT ON TABLE activity_logs IS 'Audit trail of all user actions in the system';
COMMENT ON COLUMN activity_logs.action IS 'Action performed: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, APPROVE, REJECT, UPLOAD, DOWNLOAD';
COMMENT ON COLUMN activity_logs.entity_type IS 'Type of entity: APPLICATION, DOCUMENT, USER, ROOM, PAYMENT, BUILDING';
COMMENT ON COLUMN activity_logs.entity_id IS 'ID of the entity that was acted upon';
COMMENT ON COLUMN activity_logs.ip_address IS 'IP address of the user (supports IPv4 and IPv6)';
COMMENT ON COLUMN activity_logs.metadata IS 'Additional metadata stored as JSON string';
