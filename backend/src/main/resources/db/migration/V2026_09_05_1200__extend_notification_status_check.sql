-- GH-141: the notification retry job had a terminal state it could never write.
--
-- chk_notification_status allowed only PENDING, SENT, FAILED and RETRYING, so the first
-- PERMANENTLY_FAILED write would have raised a constraint violation at commit. Because the whole
-- retry pass runs in one transaction, that would have rolled back every status update in the batch,
-- reverting already-sent rows to FAILED so they were emailed again on the next pass.
--
-- This has never fired, because the retry query filtered on retry_count < max_retries, which is the
-- same predicate as canRetry(), so the branch that writes PERMANENTLY_FAILED was unreachable. This
-- migration is a prerequisite for making it reachable, not a repair of a live failure.

ALTER TABLE notification DROP CONSTRAINT IF EXISTS chk_notification_status;
ALTER TABLE notification ADD CONSTRAINT chk_notification_status CHECK (
    notification_status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING', 'PERMANENTLY_FAILED')
);

-- The retry scan now also picks up RETRYING rows, which were stranded by an early return that
-- committed the status before discovering it could not build the email. The init-schema index has
-- predicate notification_status = 'FAILED' and would stop covering the widened scan.
--
-- Rows stranded as RETRYING are deliberately left as they are. Widening the scan recovers them on
-- the next pass, and rewriting them to FAILED would erase the one signal that distinguishes a row
-- the job abandoned from one whose dispatch genuinely failed.
DROP INDEX IF EXISTS idx_notification_retry_lookup;
CREATE INDEX idx_notification_retry_lookup
    ON notification (notification_status, retry_count, last_attempt_at)
    WHERE notification_status IN ('FAILED', 'RETRYING');
