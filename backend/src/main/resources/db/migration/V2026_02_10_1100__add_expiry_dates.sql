-- Add expiry date columns
ALTER TABLE room_allocation ADD COLUMN offer_expiry_date TIMESTAMP;
ALTER TABLE room_allocation ADD COLUMN check_in_expiry_date TIMESTAMP;

-- Backfill existing OFFERED allocations
UPDATE room_allocation
SET offer_expiry_date = created_at + INTERVAL '7 days'
WHERE status = 'OFFERED' AND offer_expiry_date IS NULL;

-- Backfill existing ACCEPTED allocations
UPDATE room_allocation
SET check_in_expiry_date = updated_at + INTERVAL '7 days'
WHERE status = 'ACCEPTED' AND check_in_expiry_date IS NULL;

-- Indexes for scheduled job performance
CREATE INDEX idx_room_allocation_offer_expiry
ON room_allocation (status, offer_expiry_date)
WHERE status = 'OFFERED';

CREATE INDEX idx_room_allocation_check_in_expiry
ON room_allocation (status, check_in_expiry_date)
WHERE status = 'ACCEPTED';
