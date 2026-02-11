-- Add CHECKED_IN status to application status column
-- The status column is VARCHAR(20), not a PostgreSQL enum type
-- This migration doesn't need to alter the schema since VARCHAR can hold the new value
-- The Java enum ApplicationStatus already includes CHECKED_IN

-- This is a no-op migration to maintain version tracking
-- The CHECKED_IN status is already supported by the VARCHAR(20) column
SELECT 1;

-- Add comment to document the new status
COMMENT ON COLUMN application.status IS 'Application lifecycle status: PENDING, UNDER_REVIEW, CHANGES_REQUESTED, APPROVED, AWAITING_ALLOCATION, ALLOCATED, CHECKED_IN, REJECTED, EXPIRED, CANCELLED';
