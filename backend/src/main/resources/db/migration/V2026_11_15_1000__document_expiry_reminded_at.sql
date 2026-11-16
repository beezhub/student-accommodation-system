-- GH-514: the daily document expiry reminder decided from dates alone whether a reminder was due,
-- so a second run on the same day (a restart with a changed cron, a second instance, a manual
-- rerun) sent the same email and in-app notification again. The job now records when it last
-- reminded each document, in South African time, and skips a document already reminded that day.
-- Empty until the first reminder after this change.
ALTER TABLE document ADD COLUMN expiry_reminded_at TIMESTAMP;

COMMENT ON COLUMN document.expiry_reminded_at IS
    'When the last expiry reminder was sent, South African time (GH-514)';
