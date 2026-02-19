-- Rename existing table to activity_logs_old
ALTER TABLE activity_logs RENAME TO activity_logs_old;

-- Create new partitioned table
-- Note: id is still BIGSERIAL but primary key must include partition key (timestamp)
CREATE TABLE activity_logs (
    id BIGSERIAL,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    description TEXT NOT NULL,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT,
    
    -- Partitioning key must be part of primary key
    PRIMARY KEY (id, timestamp),
    
    -- Re-add FK constraint (verified user_id exists in app_user)
    CONSTRAINT fk_activity_log_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) PARTITION BY RANGE (timestamp);

-- Create Partitions
-- 2024
CREATE TABLE activity_logs_2024 PARTITION OF activity_logs
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

-- 2025
CREATE TABLE activity_logs_2025 PARTITION OF activity_logs
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

-- 2026 (Current Year)
CREATE TABLE activity_logs_2026 PARTITION OF activity_logs
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- 2027 (Next Year)
CREATE TABLE activity_logs_2027 PARTITION OF activity_logs
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');

-- Default partition for any other dates
CREATE TABLE activity_logs_default PARTITION OF activity_logs DEFAULT;

-- Migrate data from old table to new partitioned table
INSERT INTO activity_logs (id, user_id, action, entity_type, entity_id, description, ip_address, timestamp, metadata)
SELECT id, user_id, action, entity_type, entity_id, description, ip_address, timestamp, metadata
FROM activity_logs_old;

-- Reset the sequence to the max id from the old table to avoid conflicts
SELECT setval('activity_logs_id_seq', COALESCE((SELECT MAX(id) FROM activity_logs_old), 1));


-- Drop old indexes from activity_logs_old (renamed from activity_logs).
-- When ALTER TABLE ... RENAME is used, PostgreSQL keeps the original index names in the
-- schema namespace — they do NOT follow the table rename. We must drop them before
-- recreating them on the new partitioned table to avoid "relation already exists" errors.
DROP INDEX IF EXISTS idx_activity_logs_user_id;
DROP INDEX IF EXISTS idx_activity_logs_action;
DROP INDEX IF EXISTS idx_activity_logs_entity_type;
DROP INDEX IF EXISTS idx_activity_logs_timestamp;
DROP INDEX IF EXISTS idx_activity_logs_entity;
DROP INDEX IF EXISTS idx_activity_logs_user_timestamp;
DROP INDEX IF EXISTS idx_activity_logs_action_entity;

-- Re-create indexes on the partitioned table (PostgreSQL 11+ propagates these to partitions)
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_action ON activity_logs(action);
CREATE INDEX idx_activity_logs_entity_type ON activity_logs(entity_type);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs(timestamp DESC);
CREATE INDEX idx_activity_logs_entity ON activity_logs(entity_type, entity_id);

-- Composite indexes
CREATE INDEX idx_activity_logs_user_timestamp ON activity_logs(user_id, timestamp DESC);
CREATE INDEX idx_activity_logs_action_entity ON activity_logs(action, entity_type);

-- Add comments
COMMENT ON TABLE activity_logs IS 'Audit trail of all user actions in the system (Partitioned by Year)';
COMMENT ON COLUMN activity_logs.action IS 'Action performed: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, APPROVE, REJECT, UPLOAD, DOWNLOAD';
COMMENT ON COLUMN activity_logs.entity_type IS 'Type of entity: APPLICATION, DOCUMENT, USER, ROOM, PAYMENT, BUILDING';

-- Clean up: Drop old table
-- In production with massive data, we might migrate in batches and keep old table for a while, 
-- but for this task we drop it to complete the migration.
DROP TABLE activity_logs_old;
