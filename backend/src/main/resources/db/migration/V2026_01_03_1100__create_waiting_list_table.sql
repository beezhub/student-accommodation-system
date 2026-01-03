-- Create waiting list table to support auto-allocation fallback
CREATE TABLE IF NOT EXISTS waiting_list_entry (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    application_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_waiting_list_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_waiting_list_application FOREIGN KEY (application_id) REFERENCES application(id)
);

CREATE INDEX IF NOT EXISTS idx_waiting_list_status_created_at ON waiting_list_entry(status, created_at);
CREATE INDEX IF NOT EXISTS idx_waiting_list_student_status ON waiting_list_entry(student_id, status);

-- Trigger to keep updated_at fresh
CREATE OR REPLACE FUNCTION set_waiting_list_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_waiting_list_updated_at ON waiting_list_entry;
CREATE TRIGGER trg_waiting_list_updated_at
BEFORE UPDATE ON waiting_list_entry
FOR EACH ROW EXECUTE FUNCTION set_waiting_list_updated_at();
