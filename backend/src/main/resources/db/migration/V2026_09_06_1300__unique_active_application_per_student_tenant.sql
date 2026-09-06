-- GH-201: one active application per student per property company was enforced only by a
-- check-then-insert in ApplicationCoreService.submitApplication. Two concurrent submits both pass
-- the existence check and both insert.
--
-- This used to be masked: application codes were derived from a lexicographic MAX, so the second
-- insert usually died on the application_code unique index and the student saw a 409. GH-143 moved
-- codes to a sequence, which removed the collision and with it that accidental backstop, so the
-- duplicate now goes through silently — worse than a confusing error.
--
-- The status list must match the activeStatuses list in ApplicationCoreService exactly. A narrower
-- index would let a duplicate through; a wider one would refuse a submission the application
-- considers legal. CHECKED_IN is deliberately absent from both.

-- ── Repair before constraining ───────────────────────────────────────────────────────────────
--
-- Any duplicate already in the table would make the CREATE INDEX below fail and take the whole
-- deployment down, so the surplus rows are cancelled first. CANCELLED is a legal terminal status
-- and keeps the record; nothing is deleted.
--
-- Which row survives matters, because cancelling the wrong one would orphan a live offer. The
-- ordering keeps, in this priority:
--   1. an application holding a live room allocation — cancelling it would strand the allocation;
--   2. otherwise the furthest through the workflow;
--   3. otherwise the most recently created.
WITH ranked AS (
    SELECT a.id,
           ROW_NUMBER() OVER (
               PARTITION BY a.student_id, a.tenant_id
               ORDER BY
                   CASE WHEN EXISTS (
                       SELECT 1 FROM room_allocation ra
                        WHERE ra.application_id = a.id
                          AND ra.status IN ('OFFERED', 'ACCEPTED', 'ACTIVE')
                   ) THEN 0 ELSE 1 END,
                   CASE a.status
                       WHEN 'ALLOCATED'           THEN 0
                       WHEN 'AWAITING_ALLOCATION'  THEN 1
                       WHEN 'APPROVED'             THEN 2
                       WHEN 'CHANGES_REQUESTED'    THEN 3
                       WHEN 'UNDER_REVIEW'         THEN 4
                       ELSE 5
                   END,
                   a.id DESC
           ) AS rn
      FROM application a
     WHERE a.status IN ('PENDING', 'UNDER_REVIEW', 'CHANGES_REQUESTED',
                        'APPROVED', 'AWAITING_ALLOCATION', 'ALLOCATED')
)
UPDATE application a
   SET status = 'CANCELLED',
       remarks = COALESCE(a.remarks,
                          'Cancelled by GH-201: duplicate active application for this property company'),
       updated_at = CURRENT_TIMESTAMP
  FROM ranked
 WHERE ranked.id = a.id
   AND ranked.rn > 1;

-- ── The invariant, in the one place that can actually hold it ────────────────────────────────
--
-- Partial, so a student may still re-apply to the same company once an earlier application reaches
-- a terminal status, and may hold one application with each of several companies.
--
-- Named, because ApplicationCoreService reads the constraint name off the
-- DataIntegrityViolationException to tell this violation apart from any other. Renaming it here
-- without renaming it there turns a 409 back into a 500.
CREATE UNIQUE INDEX ux_application_student_tenant_active
    ON application (student_id, tenant_id)
 WHERE status IN ('PENDING', 'UNDER_REVIEW', 'CHANGES_REQUESTED',
                  'APPROVED', 'AWAITING_ALLOCATION', 'ALLOCATED');
