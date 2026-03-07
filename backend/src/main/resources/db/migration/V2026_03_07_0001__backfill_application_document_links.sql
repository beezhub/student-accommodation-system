-- Backfill application_document junction entries for existing documents
-- that are linked to a student who has an active application.
-- This fixes the issue where documents uploaded before the auto-link fix
-- were not visible in the admin application review checklist.

INSERT INTO application_document (application_id, document_id, attached_at)
SELECT
    a.id   AS application_id,
    d.id   AS document_id,
    NOW()  AS attached_at
FROM document d
JOIN student s ON d.student_id = s.id
JOIN application a ON a.student_id = s.id
WHERE a.status IN ('PENDING', 'UNDER_REVIEW', 'CHANGES_REQUESTED', 'APPROVED', 'AWAITING_ALLOCATION')
  AND NOT EXISTS (
      SELECT 1
      FROM application_document ad
      WHERE ad.application_id = a.id
        AND ad.document_id    = d.id
  );
