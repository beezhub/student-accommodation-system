-- GH-499 step 7: an application is flagged as returning when, at submit, the student was checked in
-- at the same company in an earlier year or is living there now, including a lease terminated
-- early. Shown to company admins; no effect on scoring, windows or rooms. Set at submit only:
-- applications submitted before this change are not flagged.
ALTER TABLE application ADD COLUMN is_returning BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN application.is_returning IS
    'The student had been checked in at this company when they applied (GH-499)';
