-- Create allocation_rules table for per-building weighted scoring configuration
CREATE TABLE allocation_rules (
    id SERIAL PRIMARY KEY,
    building_id INT,
    rule_name VARCHAR(100) NOT NULL,
    preference_match_weight DECIMAL(5,2) DEFAULT 1.0,
    desirability_weight DECIMAL(5,2) DEFAULT 1.0,
    accessibility_weight DECIMAL(5,2) DEFAULT 1.0,
    active BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT uq_allocation_rules_building_name UNIQUE (building_id, rule_name)
);

-- Create indexes for allocation_rules table
CREATE INDEX idx_allocation_rules_building_id ON allocation_rules(building_id);
CREATE INDEX idx_allocation_rules_active ON allocation_rules(active);
CREATE INDEX idx_allocation_rules_priority ON allocation_rules(priority);
