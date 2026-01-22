-- Create allocation_action_type enum
CREATE TYPE allocation_action_type_enum AS ENUM ('MOVE', 'OVERRIDE', 'DELETE', 'FORCE_ALLOCATE');

-- Create allocation_audit_log table for compliance audit trail
CREATE TABLE allocation_audit_log (
    id SERIAL PRIMARY KEY,
    allocation_id INT,
    action_type allocation_action_type_enum NOT NULL,
    admin_user_id INT,
    reason TEXT,
    old_room_id INT,
    new_room_id INT,
    constraint_bypassed VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (allocation_id) REFERENCES room_allocation(id) ON DELETE SET NULL,
    FOREIGN KEY (admin_user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    FOREIGN KEY (old_room_id) REFERENCES room(id) ON DELETE SET NULL,
    FOREIGN KEY (new_room_id) REFERENCES room(id) ON DELETE SET NULL
);

-- Create indexes for allocation_audit_log table
CREATE INDEX idx_allocation_audit_log_allocation_id ON allocation_audit_log(allocation_id);
CREATE INDEX idx_allocation_audit_log_admin_user_id ON allocation_audit_log(admin_user_id);
CREATE INDEX idx_allocation_audit_log_created_at ON allocation_audit_log(created_at);
CREATE INDEX idx_allocation_audit_log_action_type ON allocation_audit_log(action_type);
