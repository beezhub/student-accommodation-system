-- Create staged_allocation_status enum type
CREATE TYPE staged_allocation_status_enum AS ENUM ('PROPOSED', 'CONFIRMED', 'REJECTED');

-- Create staged_allocations table for batch allocation proposals awaiting admin review
CREATE TABLE staged_allocations (
    id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    room_id INT NOT NULL,
    application_id INT,
    status staged_allocation_status_enum DEFAULT 'PROPOSED',
    score DECIMAL(10,4),
    warnings JSONB,
    proposed_by INT,
    batch_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE,
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE SET NULL,
    FOREIGN KEY (proposed_by) REFERENCES app_user(id) ON DELETE SET NULL
);

-- Create indexes for staged_allocations table
CREATE INDEX idx_staged_allocations_batch_id ON staged_allocations(batch_id);
CREATE INDEX idx_staged_allocations_status ON staged_allocations(status);
CREATE INDEX idx_staged_allocations_student_id ON staged_allocations(student_id);
CREATE INDEX idx_staged_allocations_room_id ON staged_allocations(room_id);
CREATE INDEX idx_staged_allocations_application_id ON staged_allocations(application_id);
