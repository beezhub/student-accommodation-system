-- GH-496 step 11: a place accepted long before move-in is reconfirmed before move-in, or released to
-- the waiting list.
--
-- accepted_at:            when the offer was accepted. Only places accepted more than
--                         reconfirm_min_lead_days before move-in are asked. Empty for rows accepted
--                         before this change, which are never asked.
-- reconfirm_requested_at: when "Still coming?" was first sent.
-- reconfirm_reminded_at:  when the last reminder was sent.
-- reconfirmed_at:         when the student confirmed. A confirmed place keeps its bed.
ALTER TABLE room_allocation ADD COLUMN accepted_at TIMESTAMP;
ALTER TABLE room_allocation ADD COLUMN reconfirm_requested_at TIMESTAMP;
ALTER TABLE room_allocation ADD COLUMN reconfirm_reminded_at TIMESTAMP;
ALTER TABLE room_allocation ADD COLUMN reconfirmed_at TIMESTAMP;

-- The daily job reads ACCEPTED rows by move-in date.
CREATE INDEX idx_room_allocation_status_stay_start_date
    ON room_allocation (status, stay_start_date);

-- Lead times, in days before move-in. Platform defaults; a company overrides them through the
-- system-settings cascade.
INSERT INTO system_settings (setting_key, setting_value, description, tenant_id)
VALUES ('reconfirm_min_lead_days', '42',
        'Places accepted more than this many days before move-in are asked to reconfirm', 0),
       ('reconfirm_ask_days_before', '28',
        'Days before move-in that a student is asked "Still coming?"', 0),
       ('reconfirm_release_days_before', '14',
        'Days before move-in that a place still not reconfirmed is released to the waiting list', 0)
ON CONFLICT (tenant_id, setting_key) DO NOTHING;

COMMENT ON COLUMN room_allocation.accepted_at IS 'When the offer was accepted (GH-496)';
COMMENT ON COLUMN room_allocation.reconfirm_requested_at IS 'When "Still coming?" was first sent (GH-496)';
COMMENT ON COLUMN room_allocation.reconfirm_reminded_at IS 'When the last reconfirmation reminder was sent (GH-496)';
COMMENT ON COLUMN room_allocation.reconfirmed_at IS 'When the student reconfirmed before move-in (GH-496)';
