-- Create accommodation_type table for admin-configurable accommodation types
CREATE TABLE accommodation_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default accommodation types
INSERT INTO accommodation_type (name, description, is_active) VALUES
    ('Single Room', 'Private room for one occupant', TRUE),
    ('Double Room', 'Shared room for two occupants', TRUE),
    ('Studio Apartment', 'Self-contained studio with kitchenette', TRUE),
    ('En-suite Room', 'Private room with attached bathroom', TRUE);
