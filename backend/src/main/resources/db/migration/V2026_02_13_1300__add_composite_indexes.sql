-- Add composite index for batch allocation query (fetches APPROVED applications for current year)
CREATE INDEX IF NOT EXISTS idx_application_status_year ON application(status, academic_year_id);

-- Add index for expiry job queries (fetches OFFERED/ACCEPTED allocations)
CREATE INDEX IF NOT EXISTS idx_room_allocation_status ON room_allocation(status);

-- Add index for student allocation lookups
CREATE INDEX IF NOT EXISTS idx_room_allocation_student ON room_allocation(student_id);

-- Add composite index for document approval checks
CREATE INDEX IF NOT EXISTS idx_document_application_status ON document(application_id, document_status);

-- Add composite index for user notification polling
CREATE INDEX IF NOT EXISTS idx_user_notification_user_read ON user_notifications(user_id, is_read);
