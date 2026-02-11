-- Update Room Allocation table
ALTER TABLE room_allocation ADD COLUMN check_in_date TIMESTAMP;
ALTER TABLE room_allocation ADD COLUMN check_out_date TIMESTAMP;

-- Add application reference to track which application led to this allocation
ALTER TABLE room_allocation ADD COLUMN application_id INT;
ALTER TABLE room_allocation ADD CONSTRAINT fk_room_allocation_application
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE SET NULL;

-- Add status column for allocation lifecycle management
-- New values: OFFERED, ACCEPTED, DECLINED, ACTIVE, COMPLETED, TERMINATED, CANCELLED
ALTER TABLE room_allocation ADD COLUMN status VARCHAR(20);

-- Set default status for existing allocations to ACTIVE
UPDATE room_allocation SET status = 'ACTIVE' WHERE status IS NULL;

-- Add allocation_method column to track how allocation was made
ALTER TABLE room_allocation ADD COLUMN allocation_method VARCHAR(50);

-- Add end_date column for tracking when allocation ends
ALTER TABLE room_allocation ADD COLUMN end_date TIMESTAMP;
